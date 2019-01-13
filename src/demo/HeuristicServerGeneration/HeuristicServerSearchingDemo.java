package demo.HeuristicServerGeneration;

import java.util.ArrayList;

import SPRY.Tools.Conceptual.PeriodicTask;
import SPRY.Tools.DeferrableServerTools.HeuristicDServerSearching;

/**
 * Demonstrates the server searching algorithm is that:
 * we want the aperiodic task can be executed as fast as possible,
 * while we do not have interests with the deadline of it.
 */

public class HeuristicServerSearchingDemo {

	public static void main(String[] args) {
		PeriodicTask t1 = new PeriodicTask(36, 5, 2, 3, "");
		PeriodicTask t2 = new PeriodicTask(25, 40, 8, 40, "");

		ArrayList<PeriodicTask> s = new ArrayList<>();
		s.add(t1);
		s.add(t2);
		HeuristicDServerSearching ss = new HeuristicDServerSearching();
		ss.serverSearching(s);
		System.out.println(ss.getResult());
	}
}