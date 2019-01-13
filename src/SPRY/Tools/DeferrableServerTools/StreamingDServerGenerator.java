package SPRY.Tools.DeferrableServerTools;

import java.util.ArrayList;

import SPRY.Tools.Conceptual.DServer;
import SPRY.Tools.Conceptual.PeriodicTask;
import SPRY.Tools.Timing.RTA;
import SPRY.Tools.Timing.RTATaskUnderDServer;
/** This utility is used to generate a set of deferrable servers (one per processor), so that
 * the client parallel stream processing task's computation time is maximised, while all the 
 * hard real-time tasks in the system remain schedulable. */
public class StreamingDServerGenerator {
	public boolean Latex = false;
	public boolean printInfo = false;
	
	private double Max_C_In_DataProcessingWindow = 0;

	private final int PERIOD_ORDER = -1; /* -1 longest first, 1 shortest first */
	private ArrayList<ArrayList<DServer>> allServerSets = new ArrayList<>();
	
	public ArrayList<ArrayList<DServer>> getAllServerSets() {
		return allServerSets;
	}

	public ArrayList<DServer> serverGeneration(PeriodicTask t, double prologue, double epilogue, ArrayList<ArrayList<PeriodicTask>> taskOnAllCPU, int release_CPU,
											ArrayList<Double> guaranteed_C) {
		return serverGeneration(t, prologue, epilogue, taskOnAllCPU, release_CPU,guaranteed_C, 1);
	}

	public ArrayList<DServer> serverGeneration(PeriodicTask t, double prologue, double epilogue, ArrayList<ArrayList<PeriodicTask>> taskOnAllCPU, int release_CPU,
											ArrayList<Double> guaranteed_C, double minPeriod) {
		return serverGeneration(t, prologue, epilogue, taskOnAllCPU, release_CPU, guaranteed_C, minPeriod, 0);
	}
	
	public ArrayList<DServer> serverGeneration(PeriodicTask t, double prologue, double epilogue, ArrayList<ArrayList<PeriodicTask>> taskOnAllCPU,
									int release_CPU, ArrayList<Double> guaranteed_C, double minPeriod, double DSReleaseJitter) {
		if (taskOnAllCPU == null) taskOnAllCPU = new ArrayList<>();
		
		int NUM_Procs = taskOnAllCPU.size();
		if(guaranteed_C == null) 	guaranteed_C = new ArrayList<>();
		
		ArrayList<PeriodicTask> tasks = taskOnAllCPU.get(release_CPU);
		if (tasks == null) tasks = new ArrayList<>();
		
		tasks.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
		if (!RTA.schedulabilityTest(tasks)) return null;
		
		/* make a copy */
		int numberOfTasks = tasks.size();
		ArrayList<PeriodicTask> hardTasks = new ArrayList<>(numberOfTasks);
		tasks.forEach(task -> hardTasks.add(task));
		
		
		/* calculate exact divisors */
		long divisor = (long) t.period;
		ArrayList<Long> divisors = new ArrayList<>();
		while (divisor >= 1) {
			long p = (long) (t.period / divisor);
			if (((long) (t.period)) % divisor == 0) {
				if (p >= minPeriod) divisors.add(p);
			}
			divisor--;
		}
		
		/* *********************************************************
		 * start search from the shortest or longest period 
		 * *********************************************************/
		divisors.sort((p1, p2) -> PERIOD_ORDER * Long.compare(p1, p2));
		
		DServerCapacityFinder DS_C_Finder = new DServerCapacityFinder();
		double DSPeriod, DSDeadline;		
		ArrayList<DServer> optServers = null;
		double max_C_Total = 0;
		double current_MAX_C_Total = 0;
		for (int P_INDEX = 0; P_INDEX < divisors.size(); P_INDEX++) {
			DSDeadline = DSPeriod = divisors.get(P_INDEX);
			/* determine the priority to start searching */
			ArrayList<Integer> DSPriorities = deadlineMonotonic(hardTasks, DSDeadline, DSReleaseJitter);
			DServer currentPrologueServer = null;
			for (int pi = 0; pi < DSPriorities.size(); pi++) {
				int DSPriority = DSPriorities.get(pi);
				currentPrologueServer = DS_C_Finder.findMaxCapacity(hardTasks, DSPriority, DSPeriod, DSReleaseJitter);
				if (hardTasks.size() == 0) currentPrologueServer = new DServer(DSPriority, DSPeriod, DSPeriod);
				current_MAX_C_Total = 0;
				if (currentPrologueServer != null) {
					/* ********************************************************************
					 * Calculate the GAP between prologue (include splitting) finishes 
					 * and the time when executing the epilogue using this server.
					 * 
					 * Find a set of servers in all processors that can provide the max 
					 * capacity in total.					
					 * 
					 * Given any stream processing task, the GAP can be calculated by follows:
					 * 1. Calculate the response time of prologue
					 * 2. Calculate the max capacity C_M can be guaranteed before the deadline of
					 * 		this task.
					 * 3. Remove the WCET of epilogue from C_M, say, C_ProData and calcualte the 
					 *    response time of C_ProData.
					 * This is the time when the epilogue starts executing. Then the GAP is from
					 * the response time of the prologue, to this time.
					 * 
					 * We don't have to consider the worst-case response time of the epilogue,
					 * i.e., considering the GAP between prologue and the latest time when we have
					 * to start executing epilogue. The reason is that, can avoid the
					 * analysis pessimism when analysing the epilogue.
					 * 
					 * From the point view of scheduling. Without given more than the C_Ddata (C_ProData - Prolgue),
					 * the epilogue is surely to meet the deadline. If give the same as C_Data,
					 * the epilogue will just meet the deadline, the whole processing can be treated
					 * as a whole, and this is the result of the RTA. If give less than C_Data,
					 * so that the epilogue is delayed by the data processing in other processor,
					 * i.e., there is an empty slot before eplilogue, the epilogue can also meet the deadline.
					 * The reason is that, 1 .we use deferrable server, the capacity will not idle away, the 
					 * worst-case is has been consumed, given less load, will not result in  longer response
					 * time of epilogue; 2. someone may say the interference from higher priority tasks
					 * is assumed at the beginning, i.e., the prologue, what happens if the interference
					 * occurs when the epilogue task start at above the latest time. However, this situations
					 * will not happen, because assume the higher priorities release at the start can 
					 * receive as much as interference as possible. In addition, if the inteference does not
					 * occur before, and moves back until the epilogue releases, the epilogue must be released
					 * early than the above latest start time, because less inteference in the prologue and 
					 * parallel processing will make the epilogue's release shit early.
					 * ********************************************************************/
					/* worst-case response time of prologue using this server */
					PeriodicTask prologueTask = new PeriodicTask(0, t.period, prologue, t.deadline, "");
					double R_Prologue = RTATaskUnderDServer.ResTime(prologueTask, currentPrologueServer, hardTasks);
					
					/* search the maximum possible capacity can be guaranteed before the deadline */
					double current_PrologueServer_MAX_C_Guaranteed = 0;
					for (int c = 1; c <= t.deadline; c++) {
						t.WCET = c;
						double res = RTATaskUnderDServer.ResTime(t, currentPrologueServer, hardTasks);
						if (res <= t.deadline) {
							current_PrologueServer_MAX_C_Guaranteed = c;							
						} else
							break;
					}
					
					/* the latest time to start epilogue, i.e., R_PrologueData */
					double C_ProData = current_PrologueServer_MAX_C_Guaranteed - epilogue;					
					PeriodicTask prologueAndDataTask = new PeriodicTask(0, t.period, C_ProData, t.deadline, "");
					double R_PrologueData = RTATaskUnderDServer.ResTime(prologueAndDataTask, currentPrologueServer, hardTasks);
					
					/* GAP */
					if(prologue==0) R_PrologueData =t.deadline;
					double GAP = R_PrologueData - R_Prologue;
					//System.out.println("GAP:" + GAP + " (" + R_PrologueData + " - " + R_Prologue + ")");
					/* search the maximum possible capacity can be guaranteed within GAP for the rest processors */
					current_MAX_C_Total = 0;
					ArrayList<DServer> servers = new ArrayList<>(NUM_Procs);
					ArrayList<Double> current_guaranteed_C = new ArrayList<>(NUM_Procs);
					current_MAX_C_Total += (current_PrologueServer_MAX_C_Guaranteed - prologue - epilogue);
					
					for (int i = 0; i < NUM_Procs; i++) {
						if (i != release_CPU){
							Max_C_In_DataProcessingWindow = 0;
							PeriodicTask t_temp = new PeriodicTask(0, t.period, 0, GAP, null);
							DServer optTempServer = searchForRestProcessor(taskOnAllCPU.get(i), t_temp, minPeriod, DSReleaseJitter);
							current_MAX_C_Total += Max_C_In_DataProcessingWindow;
							servers.add(optTempServer);
							current_guaranteed_C.add(Max_C_In_DataProcessingWindow);
							//System.out.println("Max_C_In_GAP:" + Max_C_In_DataProcessingWindow);
						}
						else{
							servers.add(currentPrologueServer);
							current_guaranteed_C.add((current_PrologueServer_MAX_C_Guaranteed - prologue - epilogue));
							//System.out.println("Max_C_In_GAP Prologue Processor:" + (current_PrologueServer_MAX_C_Guaranteed - prologue - epilogue));
						}
					}
					
					allServerSets.add(servers);					
					if (max_C_Total == 0 || max_C_Total < current_MAX_C_Total) {
						max_C_Total = current_MAX_C_Total;
						optServers = servers;
						guaranteed_C.clear();
						for (int i = 0; i < NUM_Procs; i++) guaranteed_C.add(current_guaranteed_C.get(i));
						//System.out.println(guaranteed_C.stream().mapToDouble(gc->(double)gc).sum());
					}					
					
					//System.out.println("Guaranteed Computation Time From All Processors for Data Processing\t" + current_MAX_C_Total + "\t" + currentPrologueServer);
					
					if(Latex){
						System.out.println(currentPrologueServer.priority + "\t&" + String.format("%1.3f", currentPrologueServer.WCET) + "\t&"
								+ (long) currentPrologueServer.period + "\t&" + String.format("%1.1f", GAP) + "\t&" 
								+ String.format("%1.1f", current_MAX_C_Total)
								+ "\\\\");
					}
				}
			}
		}
		if (optServers != null) {
//			System.out.println("Servers:");
//			optServers.forEach(x->System.out.println(x));
//			System.out.println("From All Processors, Guaranteed Max Available Capacity For Data Processing:\t" + max_C_Total + "\t");
		}
		return optServers;
	}
	
	
	public DServer searchForRestProcessor(ArrayList<PeriodicTask> tasks, PeriodicTask t) {
		return searchForRestProcessor(tasks, t, 1);
	}
	
	public DServer searchForRestProcessor(ArrayList<PeriodicTask> tasks, PeriodicTask t, double minPeriod) {
		return searchForRestProcessor(tasks, t, minPeriod, 0);
	}
	
	public DServer searchForRestProcessor(ArrayList<PeriodicTask> tasks, PeriodicTask t, double minPeriod, double DSReleaseJitter) {
		if (tasks == null) return new DServer(1, t.period, t.period);
		if (tasks.size() == 0) {
			Max_C_In_DataProcessingWindow = t.period;
			return new DServer(1, t.period, t.period);
		}
		
		tasks.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
		if (!RTA.schedulabilityTest(tasks)) return null;
		
		/* make a copy */
		int numberOfTasks = tasks.size();
		ArrayList<PeriodicTask> hardTasks = new ArrayList<>(numberOfTasks);
		tasks.forEach(task -> hardTasks.add(task));
		

		/* calculate exact divisors */
		long divisor = (long) t.period;
		ArrayList<Long> divisors = new ArrayList<>();
		while (divisor >= 1) {
			long p = (long) (t.period / divisor);
			if (((long) (t.period)) % divisor == 0) {
				if (p >= minPeriod) divisors.add(p);
			}
			divisor--;
		}
		
		/* *********************************************************
		 * start search from the shortest or longest period 
		 * *********************************************************/
		divisors.sort((p1, p2) -> PERIOD_ORDER * Long.compare(p1, p2));
		
		DServerCapacityFinder DS_C_Finder = new DServerCapacityFinder();
		double DSPeriod, DSDeadline;
		DServer optServer = null;
		double max_C = 0;
		double current_MAX_C_Guaranteed = 0;
		
		for (int P_INDEX = 0; P_INDEX < divisors.size(); P_INDEX++) {
			DSDeadline = DSPeriod = divisors.get(P_INDEX);
			/* determine the priority to start searching */
			ArrayList<Integer> DSPriorities = deadlineMonotonic(hardTasks, DSDeadline, DSReleaseJitter);
			DServer currentSearchingDS = null;
			for (int pi = 0; pi < DSPriorities.size(); pi++) {
				int DSPriority = DSPriorities.get(pi);
				currentSearchingDS = DS_C_Finder.findMaxCapacity(hardTasks, DSPriority, DSPeriod, DSReleaseJitter);
				if (hardTasks.size() == 0) currentSearchingDS = new DServer(DSPriority, DSPeriod, DSPeriod);
				current_MAX_C_Guaranteed = 0;
				if (currentSearchingDS != null) {
					/* search the maximum possible capacity can be guaranteed before the task t's deadline */
					for (int max_C_Guaranteed = 1; max_C_Guaranteed <= t.deadline; max_C_Guaranteed++) {
						t.WCET = max_C_Guaranteed;
						double res = RTATaskUnderDServer.ResTime(t, currentSearchingDS, hardTasks);
						if (res <= t.deadline) {
							current_MAX_C_Guaranteed = max_C_Guaranteed;
							if (max_C == 0){
								optServer = currentSearchingDS;
								max_C = current_MAX_C_Guaranteed;
							}
							else if (max_C < current_MAX_C_Guaranteed) {
								optServer = currentSearchingDS;
								max_C = current_MAX_C_Guaranteed;
							}
						}
						else break;
					}
					// System.out.println(current_MAX_C_Guaranteed + "\t" + currentSearchingDS);

//					 System.out.println(currentSearchingDS.priority+"\t&"+String.format("%1.3f",currentSearchingDS.WCET)+"\t&"
//					 +(long)currentSearchingDS.period
//					 +"\t&"+String.format("%1.1f",current_MAX_C_Guaranteed)+"\\\\");
				}
			}
		}
		if (optServer != null){
			// System.out.println("Server : " + optServer);
			// System.out.println("\t Max Available Capacity Within the GAP\t" + max_C);
			Max_C_In_DataProcessingWindow = max_C;
		}else{
			optServer = new DServer(0, t.period, 0);
		}
		return optServer;
	}
	
	private ArrayList<Integer> deadlineMonotonic(ArrayList<PeriodicTask> hardTasks, double Given_Deadline, double Given_Release_Jitter){
		ArrayList<Integer> priorities = new ArrayList<>();
		int priority = Integer.MAX_VALUE;
		/* start with the minimum priority -1 */
		for (int i = 0; i < hardTasks.size(); i++)
			if (hardTasks.get(i).priority < priority) priority = hardTasks.get(i).priority;
		priority -= 1;
		
		/* search from the longest deadline */
		for (int i = hardTasks.size() - 1; i >= 0; i--) {
			if (hardTasks.get(i).deadline - hardTasks.get(i).jitter > (Given_Deadline - Given_Release_Jitter)) {
				priorities.clear();
				priority = hardTasks.get(i).priority + 1;
				priorities.add(priority);
			}
			
			if (hardTasks.get(i).deadline - hardTasks.get(i).jitter == (Given_Deadline - Given_Release_Jitter)) {
				priorities.clear();
				priority = hardTasks.get(i).priority + 1;
				priorities.add(priority);
				priority = hardTasks.get(i).priority - 1;
				priorities.add(priority);
			}
			
			if (hardTasks.get(i).deadline - hardTasks.get(i).jitter < (Given_Deadline - Given_Release_Jitter)) {
				priorities.clear();
				priority = hardTasks.get(i).priority - 1;
				priorities.add(priority);
				break;
			}
		}
		if (hardTasks.size() == 0) priorities.add(1000);
		return priorities;
	}
}