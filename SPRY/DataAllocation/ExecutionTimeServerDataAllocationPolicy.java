package SPRY.DataAllocation;

import java.util.ArrayList;
import java.util.HashMap;

import SPRY.Tools.Conceptual.DServer;
import SPRY.Tools.Conceptual.PeriodicTask;
import SPRY.Tools.Timing.RTATaskUnderDServer;

public class ExecutionTimeServerDataAllocationPolicy implements DataAllocationPolicy {
	private HashMap<Long, Integer> allocationTable = new HashMap<>();

	public ExecutionTimeServerDataAllocationPolicy(ArrayList<Double> guaranteed_C, double WCET_Item, long NumberOfPatitions, long prologue, int release_CPU,
			ArrayList<DServer> servers, ArrayList<ArrayList<PeriodicTask>> taskOnAllCPU, PeriodicTask t_stream) {

		int MIN_R_CPU = 0;
		double MIN_R = Double.MAX_VALUE;
		
		/* partition allocations */
		ArrayList<ArrayList<Long>> partitionAllocations = new ArrayList<>();
		for (int i = 0; i < taskOnAllCPU.size(); i++)
			partitionAllocations.add(new ArrayList<>());
		
		for (long partition = 0; partition < NumberOfPatitions; partition++) {
			/* find out in which CPU R is minimum */
			MIN_R_CPU = 0;
			MIN_R = Double.MAX_VALUE;
			PeriodicTask temp = new PeriodicTask(0, t_stream.period, prologue, t_stream.deadline, "");
			double R_splitting = RTATaskUnderDServer.ResTime(temp, servers.get(release_CPU), taskOnAllCPU.get(release_CPU));
			for (int CPU_ID = 0; CPU_ID < partitionAllocations.size(); CPU_ID++) {
				int paritionCount = partitionAllocations.get(CPU_ID).size();
				double workload = WCET_Item * paritionCount + WCET_Item;
				if (CPU_ID == release_CPU) workload = workload + prologue;
				PeriodicTask temp2 = new PeriodicTask(0, t_stream.period, workload, t_stream.deadline, "");
				double R = RTATaskUnderDServer.ResTime(temp2, servers.get(CPU_ID), taskOnAllCPU.get(CPU_ID));
				if (CPU_ID != release_CPU) R += R_splitting;
				//System.out.print(CPU_ID + " : " + R + ", ");
				if (MIN_R > R) {
					MIN_R = R;
					MIN_R_CPU = CPU_ID;
				}
			}
			/* allocate */
			partitionAllocations.get(MIN_R_CPU).add(partition);
			allocationTable.put(partition, MIN_R_CPU);
			//System.out.println("\n" + partition + "--->" + MIN_R_CPU);
		}
	}

	@Override
	public int get(long item_id) {
		return allocationTable.get(item_id);
	}

	@Override
	public int get(long item_id, long Total_NumberOf_items) {
		return get(item_id);
	}

}
