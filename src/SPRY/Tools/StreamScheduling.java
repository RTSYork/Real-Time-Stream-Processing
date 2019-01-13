package SPRY.Tools;

import java.util.ArrayList;
import java.util.HashMap;

import SPRY.DataAllocation.DataAllocationPolicy;
import SPRY.DataAllocation.ResponseTimeDataAllocationPolicy;
import SPRY.Tools.Conceptual.DServer;
import SPRY.Tools.Conceptual.PeriodicTask;
import SPRY.Tools.DeferrableServerTools.StreamingDServerGenerator;
import SPRY.Tools.Timing.RTATaskUnderDServer;

public class StreamScheduling {
	public static boolean printInfos = true;
	
	/**
	 * For Static Stream Analysis, Response Time of each data item is not
	 * included
	 */
	public static boolean scheduleStaticStream(ArrayList<ArrayList<PeriodicTask>> taskOnAllCPU, PeriodicTask t_stream, long prologue, long splitting, long stream,
			long epilogue, int NumberOfPatitions, int min_period_filter, int release_CPU) {
		return schedule(taskOnAllCPU, t_stream, prologue, splitting, stream, epilogue, NumberOfPatitions, min_period_filter, release_CPU, null, false, 0, 0, null);
	}

	/**
	 * For Static Stream Analysis, Response Time of each data item is not
	 * included
	 */
	public static boolean scheduleStaticStream(ArrayList<ArrayList<PeriodicTask>> taskOnAllCPU, PeriodicTask t_stream, long prologue, long splitting, long stream,
			long epilogue, int NumberOfPatitions, int min_period_filter, int release_CPU, DataAllocationPolicy shuffle) {
		return schedule(taskOnAllCPU, t_stream, prologue, splitting, stream, epilogue, NumberOfPatitions, min_period_filter, release_CPU, shuffle, false, 0, 0, null);
	}

	/**
	 * For Streaming Stream Analysis, including the Response Time of each data
	 * item in the data flow. Assume the NumberOfPatitions is an exact divisor of MAXBatchSize.
	 */
	public static boolean scheduleStreamingStream(ArrayList<ArrayList<PeriodicTask>> taskOnAllCPU, PeriodicTask t_stream, long prologue, long splitting,
			long stream, long epilogue, int NumberOfPatitions, int min_period_filter, int release_CPU, int MIT, double Latency) {
		return scheduleStreamingStream(taskOnAllCPU, t_stream, prologue, splitting, stream, epilogue, NumberOfPatitions, min_period_filter, release_CPU, null, MIT, Latency);
	}

	/**
	 * For Streaming Stream Analysis, including the Response Time of each data
	 * item in the data flow. Assume the NumberOfPatitions is an exact divisor of MAXBatchSize.
	 */
	public static boolean scheduleStreamingStream(ArrayList<ArrayList<PeriodicTask>> taskOnAllCPU, PeriodicTask t_stream, long prologue, long splitting,
			long stream, long epilogue, int NumberOfPatitions, int min_period_filter, int release_CPU, DataAllocationPolicy shuffle, int MIT, double Latency) {
		meetLatency isMeet = new meetLatency();
		boolean batchDeadlineMeet = schedule(taskOnAllCPU, t_stream, prologue, splitting, stream, epilogue, NumberOfPatitions, min_period_filter, release_CPU, shuffle, true, MIT,
				Latency, isMeet);
		//System.out.println(isMeet.meetLatency + "\t" + batchDeadlineMeet);
		return isMeet.meetLatency & batchDeadlineMeet;
	}

	public static boolean schedule(ArrayList<ArrayList<PeriodicTask>> taskOnAllCPU, PeriodicTask t_stream, long prologue, long splitting, long stream,
			long epilogue, int NumberOfPatitions, int min_period_filter, int release_CPU, DataAllocationPolicy shuffle, boolean isStreaming, int MIT, 
			double Latency, meetLatency constraints) {
		
		/* Server generation on all processors */
		if (printInfos) System.out.println("-----------------------Server Generating-----------------------");
		ArrayList<DServer> servers = new ArrayList<>();
		StreamingDServerGenerator ss = new StreamingDServerGenerator();
		ArrayList<Double> guaranteed_C = new ArrayList<>();
		servers = ss.serverGeneration(t_stream, prologue + splitting, epilogue, taskOnAllCPU, release_CPU, guaranteed_C, min_period_filter);
		if (printInfos) {
			servers.forEach(x -> System.out.println(x));
			System.out.println("Total guaranteed computation time for client\n\tfrom all processors is "+
			guaranteed_C.stream().mapToDouble(gc -> (double) gc).sum() + "\tin the deadline of " + t_stream.deadline);
			System.out.println("-----------------------Server Generated-----------------------");}
		
		/* Determine the data allocation once the servers have been generated */
		if (shuffle == null){
			shuffle = new ResponseTimeDataAllocationPolicy(guaranteed_C, stream/NumberOfPatitions, NumberOfPatitions, prologue+splitting, release_CPU, servers, taskOnAllCPU, t_stream);
			if(!((ResponseTimeDataAllocationPolicy)shuffle).schedulable) return false;
		}

		if(printInfos)	System.out.println("\n=======================Real-Time Stream Processing Task Response Time Analysis=======================");
		if(printInfos)	System.out.println("-----------------------Default Pesimism Analysis-----------------------");
		double R = WCRTAanalysiStream(taskOnAllCPU, t_stream, prologue, splitting, stream, epilogue, NumberOfPatitions, release_CPU, servers, true, shuffle,
				isStreaming, MIT, Latency, constraints);
		String check = "✘";
		boolean Schedulable = false;
		if (R <= t_stream.deadline) {
			Schedulable = true;
			check = "✓";
		}
		if(printInfos)	System.out.println(String.format("The stream processing task's response time:\t%.2f\t%s", R, check));
		if(Schedulable) return true;
		
		if(!Schedulable){
			if (guaranteed_C.stream().mapToDouble(gc->(double)gc).sum() >= stream)
				Schedulable = true;
		
			if(printInfos)	System.out.println("-----------------------Mitegating Pessimism---------------------------");
			if (Schedulable)
				if(printInfos)	System.out.println("Static Stream is Schedulable");
			else
				if(printInfos)	System.out.println("Static Stream is Not Schedulable");		
			return Schedulable;
		}
		return Schedulable;
	}

	private static double WCRTAanalysiStream(ArrayList<ArrayList<PeriodicTask>> taskOnAllCPU, PeriodicTask t_stream, long prologue, long splitting, long stream,
			long epilogue, int NumberOfPatitions, int release_CPU, ArrayList<DServer> servers, boolean printInfomation, DataAllocationPolicy shuffle, boolean isStreaming,
			int MIT, double Latency, meetLatency constraints) {
		String printInfo = "";
		HashMap<Long, Double> latencyRecords = new HashMap<>();	
		
		/* Response time when splitting finishing */
		PeriodicTask temp = new PeriodicTask(0, t_stream.period, prologue + splitting, t_stream.deadline, "");		
		double R_splitting = RTATaskUnderDServer.ResTime(temp, servers.get(release_CPU), taskOnAllCPU.get(release_CPU));
		if (temp.WCET == 0) R_splitting = 0;
		printInfo += String.format("Splitting finishes at:\t%.2f", R_splitting) + "\n";

		/* partition calculation */
		ArrayList<ArrayList<Long>> partitionAllocations = new ArrayList<>();
		for (int i = 0; i < taskOnAllCPU.size(); i++)
			partitionAllocations.add(new ArrayList<>());
		for (long i = 0; i < NumberOfPatitions; i++) {
			int cpuId = shuffle.get(i, NumberOfPatitions);
			partitionAllocations.get(cpuId).add(i);
		}
		
		/* print out the partitions allocations */
		System.out.println("Paritition allocations:");
		for (int i = 0; i < partitionAllocations.size(); i++){
			System.out.print("CPU " + i + ":\t");
			partitionAllocations.get(i).forEach(x->System.out.print(x+", "));
			System.out.println();
		}
		System.out.println("----------------------------------------------");
		double R_stream = 0;
		for (int i = 0; i < partitionAllocations.size(); i++) {
			int paritionCount = partitionAllocations.get(i).size();
			double workload = stream * paritionCount / NumberOfPatitions;
			if (i == release_CPU) workload = workload + prologue + splitting;
			PeriodicTask temp2 = new PeriodicTask(0, t_stream.period, workload, t_stream.deadline, "");
			double R = RTATaskUnderDServer.ResTime(temp2, servers.get(i), taskOnAllCPU.get(i));
			if (i != release_CPU) R += R_splitting;
			/* analysis for streaming data items */
			int MAXBatchSize = NumberOfPatitions;
			int partitionSize = MAXBatchSize/NumberOfPatitions;
			if(isStreaming){
				for (int partitionIndex = 0; partitionIndex < partitionAllocations.get(i).size(); partitionIndex++){
					long partitionId = partitionAllocations.get(i).get(partitionIndex);
					for (int itemIndex = 0; itemIndex < partitionSize; itemIndex++){
						long itemId = partitionId * partitionSize + itemIndex;
						int ItemsCount = partitionIndex * partitionSize + itemIndex + 1;
						workload = stream * ItemsCount / MAXBatchSize;
						if (i == release_CPU) workload = workload + prologue + splitting;
						double R_Item = RTATaskUnderDServer.ResTime(new PeriodicTask(0, t_stream.period, workload, t_stream.deadline, ""), servers.get(i), taskOnAllCPU.get(i));
						if (i != release_CPU) R_Item += R_splitting;
						double WaitingTime = MIT * (MAXBatchSize - (itemId+1));
						latencyRecords.put(itemId, R_Item + WaitingTime);
					}
				}
			}
			
			printInfo += "In processor " + i + String.format(" parallel processing finishes at:\t%.2f", R) + "\t number of partitions allocated: "
					+ paritionCount + "\n";
			if (R_stream < R) R_stream = R;
		}
		printInfo += String.format("All of the parallel processings finshes at:\t%.2f", R_stream) + "\n";

		PeriodicTask temp3 = new PeriodicTask(0, t_stream.period, epilogue, t_stream.deadline, "");
		double R_epi = RTATaskUnderDServer.ResTime(temp3, servers.get(release_CPU), taskOnAllCPU.get(release_CPU));
		if (epilogue == 0) R_epi = 0;
		if (R_epi != 0) R_epi += servers.get(release_CPU).period - servers.get(release_CPU).WCET;
		printInfo += String.format("The resposne time of the epilogue of the task:\t%.2f", R_epi);
		double R = R_stream + R_epi;
		if (printInfomation){
			if(printInfos)	System.out.println(printInfo);
			if(isStreaming){
				String meetLatency = "✘";
				if(printInfos)System.out.println("-----------------Latency of items --------------------");
				for (long i = 0; i < NumberOfPatitions; i++) {				
					double latency = latencyRecords.get(i);
					if (latency <= Latency)
						meetLatency = "✓";
					else {
						meetLatency = "✘";
						constraints.meetLatency = false;
					}
					if(printInfos)	System.out.println(String.format("\tItem %d's\tLatency =\t%.2f\t%s", i, latency, meetLatency));
				}				
			}
		}
		return R;
	}
}

class meetLatency{
	public boolean meetLatency = true;
}
