package SPRY.Tools.Timing;

import java.util.ArrayList;

import SPRY.Tools.Conceptual.DServer;
import SPRY.Tools.Conceptual.PeriodicTask;
/** Response time analysis tools for a task, which is executing under a deferrable server,
 * and receiving interference from a set of periodic (sporadic) higher priority real-time tasks */
public class RTATaskUnderDServer {
	public static boolean schedulabilityTest(PeriodicTask t, DServer S, ArrayList<PeriodicTask> higperPriorities) {	
		if (ResTime(null, t, S, higperPriorities) != -1) return true;		
		return false;		
	}
	
	public static boolean schedulabilityTest(ArrayList<PeriodicTask> allTasksinServer, PeriodicTask t, DServer S, ArrayList<PeriodicTask> higperPriorities) {	
		if (ResTime(allTasksinServer, t, S, higperPriorities) != -1) return true;		
		return false;		
	}

	public static double ResTime(PeriodicTask t, DServer S, ArrayList<PeriodicTask> higperPriorities) {
		return ResTime(null, t, S, higperPriorities);
	}
	
	public static double ResTime(ArrayList<PeriodicTask> allTasksinServer, PeriodicTask t, DServer S, ArrayList<PeriodicTask> higperPriorities) {
		if (S == null) return Double.MAX_VALUE;
		if (S.WCET <= 0) return Double.MAX_VALUE;
		if (t.WCET == 0) return 0;
		if (allTasksinServer == null) allTasksinServer = new ArrayList<>();
		if (higperPriorities == null) higperPriorities = new ArrayList<>();
		higperPriorities.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
		
		double w0 = t.WCET + (Math.ceil(t.WCET / S.WCET) - 1) * (S.period - S.WCET);
		// System.out.println("w0=" + w0);
		double w_n = w0;
		double w_nPlus = W(allTasksinServer, w_n, t, S, higperPriorities);
		double jitter = S.period - S.WCET;
		if ((S.period % t.period == 0) || (t.period % S.period == 0))	jitter = 0;
		while (w_n != w_nPlus) {			
			if (w_nPlus > (t.deadline - jitter)) return Double.MAX_VALUE;
			w_n = w_nPlus;
			w_nPlus = W(allTasksinServer, w_n, t, S, higperPriorities);
		}
		return w_n;
	}
	
	private static double load(double wi, ArrayList<PeriodicTask> allTasksinServer, PeriodicTask t, DServer S){
		double l = 0;
		for (int i = 0; i < allTasksinServer.size(); i++) {
			PeriodicTask J = allTasksinServer.get(i);
			if (J.priority > t.priority) {
				double jitter = 0;
				if (J instanceof DServer) {
					if ((S.period % J.period == 0) || (J.period % S.period == 0)) {
						jitter = 0;
					} else
					{
						jitter = J.period - J.WCET;
					}
				}
				l += Math.ceil((wi + jitter) / J.period) * J.WCET;
			}
		}
		l += t.WCET;
		return l;
	}
	
	private static double W(ArrayList<PeriodicTask> allTasksinServer, double w_n,PeriodicTask Ti, DServer S, ArrayList<PeriodicTask> higperPriorities){
		double w = 0;
		double l = load(w_n, allTasksinServer, Ti, S);
		//System.out.println("l=" + l);
		w = l + (Math.ceil(l / S.WCET) - 1) * (S.period - S.WCET);
		//System.out.println("w=" + w);
		for (int i = 0; i < higperPriorities.size(); i++) {
			PeriodicTask hp = higperPriorities.get(i);
			if (hp.priority > S.priority) {
				double jitter = 0;
				if (hp instanceof DServer) {
					if ((S.period % hp.period == 0) || (hp.period % S.period == 0)) {
						jitter = 0;
					} else 
					{
						jitter = hp.period - hp.WCET;
					}
				}
				w += Math.ceil(Math.max(0, (w_n - (Math.ceil(l / S.WCET) - 1) * S.period) + jitter) / hp.period) * hp.WCET;
			}
		}
		//System.out.println("w=" + w);
		return w;
	}
}