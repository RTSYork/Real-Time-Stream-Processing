package SPRY.Tools;

import java.util.ArrayList;

import SPRY.Tools.Conceptual.PeriodicTask;
/** This utility assign priorities to a set of real-time tasks, 
 * using the deadline monotonic priority assignment */
public class PriorityUtil {
	public static final int MAX_PRIORITY = 1000;
	
	private static ArrayList<Integer> generatePriorities(int number) {
		ArrayList<Integer> priorities = new ArrayList<>();
		for (int i = 0; i < number; i++)
			priorities.add(MAX_PRIORITY - (i + 1) * 2);
		return priorities;
	}

	public static void deadlineMonotonicPriorityAssignment(ArrayList<PeriodicTask> taskset, int number) {
		ArrayList<Integer> priorities = generatePriorities(number);
		/* deadline monotonic assignment */
		taskset.sort((t1, t2) -> Double.compare(t1.deadline - t1.jitter, t2.deadline - t2.jitter));
		priorities.sort((p1, p2) -> -Integer.compare(p1, p2));
		for (int i = 0; i < taskset.size(); i++) {
			taskset.get(i).priority = priorities.get(i);
		}
	}
}
