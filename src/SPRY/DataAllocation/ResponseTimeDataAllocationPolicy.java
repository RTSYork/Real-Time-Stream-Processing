package SPRY.DataAllocation;

import java.util.ArrayList;
import java.util.HashMap;

import SPRY.Tools.Conceptual.DServer;
import SPRY.Tools.Conceptual.PeriodicTask;
import SPRY.Tools.Timing.RTATaskUnderDServer;
/** The data allocation policy data tries to minimise the response time of each data partition */
public class ResponseTimeDataAllocationPolicy implements DataAllocationPolicy {
	private HashMap<Long, Integer> allocationTable = new HashMap<>();
	public boolean schedulable = true;
	public ResponseTimeDataAllocationPolicy(ArrayList<Double> guaranteed_C, double WCET_Item, long NumberOfPatitions, long prologue, int release_CPU,
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
			if (prologue == 0) R_splitting = 0;
			for (int CPU_ID = 0; CPU_ID < partitionAllocations.size(); CPU_ID++) {
				int paritionCount = partitionAllocations.get(CPU_ID).size();
				double workload = WCET_Item * paritionCount + WCET_Item;
				if (CPU_ID == release_CPU) workload = workload + prologue;
				PeriodicTask temp2 = new PeriodicTask(0, t_stream.period, workload, t_stream.deadline, "");
				double R = RTATaskUnderDServer.ResTime(temp2, servers.get(CPU_ID), taskOnAllCPU.get(CPU_ID));
				if (CPU_ID != release_CPU) R += R_splitting;
				//System.out.println(temp2.WCET + " on " + CPU_ID + " : " + R + ", ");
				if (MIN_R > R) {
					MIN_R = R;
					MIN_R_CPU = CPU_ID;
				}
			}
			/* allocate */
			partitionAllocations.get(MIN_R_CPU).add(partition);
			allocationTable.put(partition, MIN_R_CPU);
			//System.out.println("\n" + partition + "--->" + MIN_R_CPU);
			if (MIN_R > t_stream.deadline) {
				schedulable = false;
			}
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
