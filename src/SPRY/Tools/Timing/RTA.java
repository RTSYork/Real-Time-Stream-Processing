package SPRY.Tools.Timing;

import java.util.ArrayList;

import SPRY.Tools.Conceptual.DServer;
import SPRY.Tools.Conceptual.PeriodicTask;
import SPRY.Tools.Conceptual.SporadicTask;
/** Response time analysis tools for a set of periodic (sporadic) real-time tasks */
public class RTA {

	public static boolean schedulabilityTest(ArrayList<PeriodicTask> hardRTTasks) {
		return schedulabilityTest(hardRTTasks, false);
	}
	
	public static boolean schedulabilityTest(ArrayList<PeriodicTask> hardRTTasks, boolean printDebugInfo) {
		if (hardRTTasks == null | hardRTTasks.size() == 0) return true;
		hardRTTasks.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
		boolean allMeetDeadlines = true;
		/* initialise variables */
		Double[] Wi = new Double[hardRTTasks.size()];

		for (int i = 0; i < hardRTTasks.size(); i++) {
			PeriodicTask t = hardRTTasks.get(i);
			Wi[i] = t.WCET; /* Wi */
			double WiPlus1;
			while (true) {
				WiPlus1 = busyWindow(t, hardRTTasks, Wi[i]);
				if (Wi[i] == WiPlus1) {/* response time for task: i, found */
					break;
				} else {
					Wi[i] = WiPlus1;
				}
				if (Wi[i] > t.deadline) {
					if (printDebugInfo) System.out.println(Wi[i] + "\tD=" + t.deadline);
					return false;
				}
			}
			/* add the jitter to the response time */
			if (t.jitter > 0) {
				if (t instanceof DServer) {
					Wi[i] += ((DServer) t).server_jitter;
				} else {
					Wi[i] += t.jitter;
				}
			}
			if (printDebugInfo) System.out.println(Wi[i] + "\tD=" + t.deadline);
			if (Wi[i] > t.deadline) return false;
		}
		return allMeetDeadlines;
	}

	public static double busyWindow(PeriodicTask t, ArrayList<PeriodicTask> tasks, double Ri) {
		double interference = 0;
		for (int i = 0; i < tasks.size(); i++) {
			PeriodicTask hp = tasks.get(i);
			if (hp.priority > t.priority) {
				double jitter = hp.jitter;
				/* *****************************************************************************************
				 * When calculating the interference of higher priority Deferrable Server(DS)
				 * on a task: i. We need to consider the 'double hit' phenomenon. See the paper:
				 * New Results on Fixed Priority Aperiodic Servers by Bernat and Burns
				 * 
				 * Therefore, the equivalent way is treat DS as a task with jitter = T_s- C_s.
				 * However, when T_s % T_i ==0, or T_i % T_s ==0 'double hit' phenomenon never happens.
				 * Hence, in this situation, T_s- C_s is not required to be involved.
				 * 
				 * However, this requires t should not be a Sporadic task, because it will
				 * suffer double hit due to its non-periodic releases.
				 * 
				 * Note that, when calculating the response time of DS, T_s- C_s should not be involved.
				 * *****************************************************************************************/
				if (hp instanceof DServer) {
					if ((t.period % hp.period == 0) || (hp.period % t.period == 0) &&
							!(t instanceof SporadicTask)) {
						jitter = ((DServer) hp).server_jitter;
					} else
					{
						jitter = ((DServer) hp).server_jitter + hp.period - hp.WCET;
					}
				}
				interference += Math.ceil(((Ri + jitter) / hp.period)) * hp.WCET;
			}
		}
		double responseTime = t.WCET + interference;
		return responseTime;
	}
}