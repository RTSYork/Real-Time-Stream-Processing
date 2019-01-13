package SPRY.ExecutionTimeServer;

import java.util.ArrayList;
import java.util.HashMap;

import javax.realtime.ProcessingGroupParameters;
import javax.realtime.Schedulable;

public abstract class ProcessingGroup {
	protected ProcessingGroupParameters PGP;
	/* Real-time Threads that associated with this server */
	protected ArrayList<Schedulable> registeredRealtimeThreads = new ArrayList<>();
	protected HashMap<Schedulable, Boolean> registerRecords = new HashMap<>();

	ProcessingGroup(ProcessingGroupParameters PGP) {
		this.PGP = PGP;
	}

	public ProcessingGroupParameters getPGP() {
		return PGP;
	}

	/**
	 * Register a RealtimeThread to this server, target thread will use the
	 * ProcessingGroupParameters of this server, through
	 * setProcessingGroupParameters(this.getPGP())
	 */
	public void register(Schedulable... rtThreadsToRegister) {
		try {
			for (int i = 0; i < rtThreadsToRegister.length; i++) {
				if (rtThreadsToRegister[i] != null) {
					if (!registerRecords.containsKey(rtThreadsToRegister[i])) {
						registerRecords.put(rtThreadsToRegister[i], true);
						registeredRealtimeThreads.add(rtThreadsToRegister[i]);
						setProcessingGroupParameter(rtThreadsToRegister[i], this.getPGP());
					}
				}
			}
		} catch (NullPointerException e) {
		}
	}
	
	/**
	 * protected method. This method is used to update the thread's PGP
	 */
	protected void setProcessingGroupParameter(Schedulable t, ProcessingGroupParameters p) {
		t.setProcessingGroupParameters(p);
	}

	/**
	 * DeRegister a RealtimeThread to this server, the ProcessingGroupParameters
	 * of this server will be detached
	 */
	public void deRegister(Schedulable... rtThreadsToDeregister) {
		try {
			for (int i = 0; i < rtThreadsToDeregister.length; i++) {
				if (rtThreadsToDeregister[i] != null) {
					rtThreadsToDeregister[i].setProcessingGroupParameters(null);
				}
				if (registerRecords.containsKey(rtThreadsToDeregister[i])) {
					registeredRealtimeThreads.remove(rtThreadsToDeregister[i]);
					registerRecords.remove(rtThreadsToDeregister[i]);
				}
			}
		} catch (NullPointerException e) {
		}
	}

	/**
	 * return associatedRealtimeThreads
	 */
	public ArrayList<Schedulable> getRegisteredRealtimeThreads() {
		return registeredRealtimeThreads;
	}
}
