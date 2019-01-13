package SPRY.Tools.Timing;

import java.util.ArrayList;

import SPRY.Tools.Conceptual.DServer;
import SPRY.Tools.Conceptual.PeriodicTask;
/** Response time analysis tools for a task, which is executing under a periodic server,
 * and receiving interference from a set of periodic (sporadic) higher priority real-time tasks */
public class RTATaskUnderPeriodicServer {
	public static boolean schedulabilityTest(ArrayList<PeriodicTask> hardRTTasks, PeriodicTask t,
			DServer S, ArrayList<PeriodicTask> taskHpThanS) {	
		if (ResTime(hardRTTasks, t, S, taskHpThanS) != -1) return true;		
		return false;		
	}
	
	public static double ResTime(ArrayList<PeriodicTask> hardRTTasks, PeriodicTask t, DServer S, ArrayList<PeriodicTask> taskHpThanS) {
		if (hardRTTasks == null) hardRTTasks = new ArrayList<>();
		hardRTTasks.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
		if (taskHpThanS == null) taskHpThanS = new ArrayList<>();
		
		double w0 = t.WCET + (Math.ceil(t.WCET / S.WCET) - 1) * (S.period - S.WCET);
		//System.out.println("w0=" + w0);
		double w_n = w0;
		double w_nPlus = W(w_n, hardRTTasks, t, S, taskHpThanS);
		while (w_n != w_nPlus) {
			if (w_nPlus > (t.deadline - t.jitter)) return -1;
			w_n = w_nPlus;
			w_nPlus = W(w_n, hardRTTasks, t, S, taskHpThanS);
		}
		return w_n;
	}
	
	private static double L(double wi, ArrayList<PeriodicTask> hardRTTasks, PeriodicTask Ti, DServer S){
		double l = 0;
		for (int i = 0; i < hardRTTasks.size(); i++) {
			PeriodicTask J = hardRTTasks.get(i);
			if (J.priority > Ti.priority) {
				l += Math.ceil((wi + J.jitter) / J.period) * J.WCET;
			}
		}
		l += Ti.WCET;
		return l;
	}
	
	private static double W(double wi, ArrayList<PeriodicTask> hardRTTasks, PeriodicTask Ti, DServer S, ArrayList<PeriodicTask> taskHpThanS){
		double w = 0;
		double l = L(wi, hardRTTasks, Ti, S);
		//System.out.println("l=" + l);
		w = l + (Math.ceil(l / S.WCET) - 1) * (S.period - S.WCET);
		//System.out.println("w=" + w);
		for (int i = 0; i < taskHpThanS.size(); i++) {
			PeriodicTask X = taskHpThanS.get(i);
			if (X.priority > S.priority) {
				w += Math.ceil(Math.max(0, wi - (Math.ceil(l / S.WCET) - 1) * S.period) / X.period) * X.WCET;
			}
		}
		//System.out.println("w=" + w);
		return w;
	}
}