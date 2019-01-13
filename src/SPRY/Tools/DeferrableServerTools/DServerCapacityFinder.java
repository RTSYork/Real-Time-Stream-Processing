package SPRY.Tools.DeferrableServerTools;

import java.util.ArrayList;

import SPRY.Tools.Timing.RTA;
import SPRY.Tools.Conceptual.DServer;
import SPRY.Tools.Conceptual.PeriodicTask;

/**
 * Find possible deferrable servers, in the system contents hard real-time
 * periodic and sporadic tasks. These servers will not cause any deadline miss
 * of existing periodic or sporadic tasks.
 * 
 * Deadline is bigger than the period is not supported yet.
 */

public class DServerCapacityFinder {
	public double SearchGranularity = 0.00000001;
	public ArrayList<DServer> servers = new ArrayList<>();

	public final static int MicroSecond = 0;
	public final static int NanoSecond = 6;
	private int ServerGranularity = MicroSecond;

	public DServer findMaxCapacity(ArrayList<PeriodicTask> hardPeriodics, int serverPriority, double serverPeriod) {
		return findMaxCapacity(hardPeriodics, serverPriority, serverPeriod, 0);
	}
	
	public DServer findMaxCapacity(ArrayList<PeriodicTask> hardPeriodics, int serverPriority, double serverPeriod, double DSReleaseJitter) {
		servers.clear();
		if (hardPeriodics.size() == 0) return null;
		hardPeriodics.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
		if (!schedulabilityTest(hardPeriodics)) {
			return null;
		}
		
		ArrayList<PeriodicTask> tasks = new ArrayList<>();
		tasks.addAll(hardPeriodics);
		
		double serverDeadline = serverPeriod;
		double capacity = 0;
		PeriodicTask t = null;

		for (int i = 0; i < hardPeriodics.size(); i++) {
			t = hardPeriodics.get(i);
			if (t.priority > serverPriority)
				continue;
			else
				break;
		}
		double MAX = serverPeriod;
		double MIN = MAX;
		if (MIN == 0) return null;
		capacity = MIN;
		DServer server = new DServer(serverPriority, serverPeriod, capacity, serverDeadline);
		server.server_jitter = DSReleaseJitter; /* set the release jitter for the server */
		tasks.add(server);
		tasks.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
		
		boolean found = false;
		if (schedulabilityTest(tasks)) {
			found = true;
			capacity = MIN;
		} else {
			MIN *= 0.5;
			/* granularity */
			double preMin = 0;
			while ((MAX - MIN) >= SearchGranularity) {
				server.WCET = MIN;
				if (schedulabilityTest(tasks)) {
					found = true;
					capacity = MIN;
					preMin = MIN;
					MIN += (MAX - MIN) * 0.5;
				} else {
					MAX = MIN;
					MIN = preMin + 0.5 * (MIN - preMin);
				}
			}
		}
		if (found) {
			/* round up or down according to the granularity */
			/*
			double roundupCapacity = new BigDecimal(capacity).setScale(ServerGranularity, BigDecimal.ROUND_UP).doubleValue();
			server.WCET = roundupCapacity;
			if (schedulabilityTest(tasks))
				capacity = roundupCapacity;
			else
				capacity = new BigDecimal(capacity).setScale(ServerGranularity, BigDecimal.ROUND_DOWN).doubleValue();
			*/
			DServer serverFound = new DServer(serverPriority, serverPeriod, capacity, serverDeadline);
			serverFound.server_jitter = DSReleaseJitter; /* set the release jitter for the server */
			return serverFound;
		} else {
			return null;
		}
	}

	public boolean schedulabilityTest(ArrayList<PeriodicTask> hardRTTasks) {
		return RTA.schedulabilityTest(hardRTTasks);
	}

	public int getServerGranularity() {
		return ServerGranularity;
	}

	public void setServerGranularity(int serverGranularity) {
		ServerGranularity = serverGranularity;
	}
}