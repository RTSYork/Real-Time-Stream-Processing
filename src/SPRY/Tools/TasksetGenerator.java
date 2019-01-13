package SPRY.Tools;

import java.util.ArrayList;
import java.util.Random;

import SPRY.Tools.Conceptual.PeriodicTask;
import SPRY.Tools.Timing.RTA;
/** Generate a set of periodic real-time tasks that have a given total utilisation.
 * The period is randomly generated between the given MIN and MAX values */
public class TasksetGenerator {
	
	public static enum Deadlines {
		DeadlineEqualPeriod, DeadlineLessThanPeriod, DeadlineBiggerThanPeriod, ArbitraryDeadline
	};

	public static ArrayList<PeriodicTask> generateTaskset(int number, double minT, double maxT, double util) {
		return generateTaskset(number, minT, maxT, util, Deadlines.DeadlineEqualPeriod);
	}

	public static ArrayList<PeriodicTask> generateTaskset(int number, double minT, double maxT, double util, Deadlines DPolice) {
		ArrayList<Double> utils = null;
		uunifastDiscard unifastDiscard = new uunifastDiscard(util, number, 1000);
		while (true) {
			utils = unifastDiscard.getUtils();
			if (utils != null) if (utils.size() == number) break;
		}

		ArrayList<PeriodicTask> tasks = new ArrayList<>(number);
		ArrayList<Double> periods = new ArrayList<>(number);
		Random random = new Random();
		while (true) {
			tasks.clear();
			periods.clear();
			/* generates random periods */
			while (true) {
				int period = random.nextInt((int) (maxT - minT)) + (int) minT;
				if (!periods.contains(period)) periods.add((double) period);
				if (periods.size() >= number) break;
			}
			periods.sort((p1, p2) -> Double.compare(p1, p2));
			// utils.forEach(x->System.out.println(x));
			for (int i = 0; i < utils.size(); i++) {
				double T = periods.get(i);
				double C = T * utils.get(i);
				double D = T;
				switch (DPolice) {
				case DeadlineEqualPeriod:
					D = T;
					break;
				case DeadlineLessThanPeriod:
					D = random.nextInt((int) (T - C - 1)) + (int) C;
					break;
				case DeadlineBiggerThanPeriod:
					D = random.nextInt((int) (Integer.MAX_VALUE - T - 1)) + (int) (T + 1);
					break;
				case ArbitraryDeadline:
					D = random.nextInt((int) (Integer.MAX_VALUE - (int) (C + 1))) + (int) C;
					break;
				default:
					D = T;
					break;
				}
				PeriodicTask t = new PeriodicTask(-1, T, C, D, "");
				tasks.add(t);
			}
			PriorityUtil.deadlineMonotonicPriorityAssignment(tasks, number);
			if (RTA.schedulabilityTest(tasks)) break;
		}
		// for (int i = 0; i < tasks.size(); i++)
		// System.out.println(tasks.get(i));
		return tasks;
	}
}
// public class LogUniformTest {
//
// public static void main(String[] args) {
// Random random = new Random();
// ArrayList<Integer> uniform = new ArrayList<>();
// ArrayList<Integer> loguniform = new ArrayList<>();
//
// for (int i = 0; i < 10; i++) {
// int period = random.nextInt(999) + 1;
// uniform.add(period);
//
// double a1 = Math.log(1);
// double a2 = Math.log(1001);
// // System.out.println("a1: " + a1 + " a2: " + a2);
// double scaled = random.nextDouble() * (a2 - a1);
// double shifted = scaled + a1;
//
// // System.out.println(shifted);
//
// double exp = Math.exp(shifted);
// // System.out.println("exp: " + exp);
//
// int result = (int) exp;
// result = Math.max(1, result);
// result = Math.min(1000, result);
// // System.out.println("result: " + result);
// loguniform.add(result);
// // System.out.println();
// }
//
// System.out.println("uniform");
// for (int i = 0; i < uniform.size(); i++) {
// System.out.print(uniform.get(i) + " ");
// }
// System.out.println();
// System.out.println("log uniform");
// for (int i = 0; i < loguniform.size(); i++) {
// System.out.print(loguniform.get(i) + " ");
// }
// }
//
// }