package SPRY.ExecutionTimeServer;

import javax.realtime.PriorityParameters;
import javax.realtime.ProcessingGroupParameters;
/** not implemented yet */
public abstract class PollingServer extends AperiodicServer {

	public PollingServer(ProcessingGroupParameters PGP, PriorityParameters SP) {
		super(PGP, SP);
	}
}