package SPRY.Tools.DeferrableServerTools;

import java.util.ArrayList;

import SPRY.Tools.Conceptual.DServer;
import SPRY.Tools.Conceptual.PeriodicTask;

/** The purpose of doing this server searching algorithm is that:
 * 	we want the aperiodic task can be executed as fast as possible,
 * 	while we do not have interests with the deadline of it.
 * */

public class HeuristicDServerSearching {
	private String result = "";
	
	public void printResult(){
		System.out.print(result);
	}
	
	public String getResult() {
		return result;
	}
	public ArrayList<DServer> serverSearching(ArrayList<PeriodicTask> tasks){
		return serverSearching(tasks, 1);
	}
	public ArrayList<DServer> serverSearching(ArrayList<PeriodicTask> tasks, double minPeriod) {
		result = "";
		int numberOfTasks = tasks.size();

		ArrayList<Integer> priorities = new ArrayList<>(numberOfTasks);
		ArrayList<PeriodicTask> hardTasks = new ArrayList<>(numberOfTasks); 

		DServerCapacityFinder serverFinder = new DServerCapacityFinder();
		tasks.forEach(t -> {
			hardTasks.add(t);
			priorities.add(t.priority);
		});

		priorities.sort((p1, p2) -> -Integer.compare(p1, p2));

		double[] periods = potentialPeriods(tasks, minPeriod);

		ArrayList<DServer> servers = new ArrayList<>();
		/* This algorithm is not optimal when there is task has D<T,
		 * this is because we greedy search a server
		 * at each priority level. 
		 * E.g., using this algorithm on a task set:
		 * Task1 (P=37, T=5, C=1, D=3)
		 * Task2 (P=26, T=10, C=2, D=10)
		 * will get S(P=38, T=3, C=1.666666, D=3) not the optimal servers:
		 * S1 (P=38, T=5, C=2, D=5)
		 * S2 (P=27, T=10, C=2, D=10)
		 * The reason is that for S, utilisation of server at priority 38 is 55.5%,
		 * but it is only 40% for the optimal servers.
		 * */
		for (int p = 0; p < numberOfTasks; p++) {
			int priority = priorities.get(p) + 1;
			/* search all the possible period */
			DServer opServer = null;
			for (int i = 0; i < periods.length; i++) {
				DServer server = serverFinder.findMaxCapacity(hardTasks, priority, periods[i]);
				if (server != null) {
					if (opServer == null)
						opServer = server;
					else {
						if (server.WCET / server.period > opServer.WCET / opServer.period) {
							opServer = server;
						} else {
							if (server.WCET / server.period == opServer.WCET / opServer.period) {
								if (server.period > opServer.period) opServer = server;
							}
						}
					}
				}
			}
			if (opServer != null) {
				servers.add(opServer);
				hardTasks.add(opServer);
			}
		}

		/* servers' total utilisation */
		double serverUtilisationGain = 0;
		double hpServerUtiGain = 0;
		for (int i = 0; i < servers.size(); i++) {
			serverUtilisationGain += servers.get(i).WCET / servers.get(i).period;
			if (i == 0) hpServerUtiGain = serverUtilisationGain;
		}

		/* system utilisation after applying servers */
		double sysUtilisationGain = 0;
		sysUtilisationGain += serverUtilisationGain;
		for (int i = 0; i < tasks.size(); i++)
			sysUtilisationGain += tasks.get(i).WCET / tasks.get(i).period;
		result += sysUtilisationGain + "\t";

		/* Liu & Layland RM utilisation bound */
		int totalTask = numberOfTasks;
		for (int i = 0; i < servers.size(); i++) {
			DServer s = servers.get(i);
			if (s.WCET > 0.001) totalTask++;
		}
		double pw = (double) (1 / (double) totalTask);
		double LiuLayland = totalTask * (Math.pow(2, pw) - 1);
		result += LiuLayland;

		/* server info */
		for (int i = 0; i < servers.size(); i++){
				result += "\t" + servers.get(i).toString();
		}
		/* hp server percentage */
		double hpServerPercentage = 0;
		if (hpServerUtiGain > 0) hpServerPercentage = (hpServerUtiGain / serverUtilisationGain) * 100;
		result += "\t" + hpServerPercentage;

		result += "\n";
		return servers;
	}

	public double[] potentialPeriods(ArrayList<PeriodicTask> tasks, double minPeriod) {
		ArrayList<Double> periods = new ArrayList<>();

		for (int t = 0; t < tasks.size(); t++) {
			double D = tasks.get(t).deadline;
			for (int i = 2; i <= D / 2; i++) {
				if (D % i == 0) {
					if (!periods.contains((double) i)) {
						periods.add((double) i);
					}
				}
			}
			periods.add(D);
		}
		ArrayList<Double> periodsFiltered = new ArrayList<>(periods.size());
		periods.stream().filter(p -> p >= minPeriod).forEach(p -> periodsFiltered.add(p));

		double[] hps = new double[periodsFiltered.size()];
		for (int i = 0; i < periodsFiltered.size(); i++)
			hps[i] = periodsFiltered.get(i);
		return hps;
	}
}
