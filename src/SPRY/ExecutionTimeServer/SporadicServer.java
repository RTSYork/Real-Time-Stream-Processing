package SPRY.ExecutionTimeServer;

import javax.realtime.PriorityParameters;
import javax.realtime.ProcessingGroupParameters;
/** not implemented yet */
public abstract class SporadicServer extends AperiodicServer {
	private PriorityParameters backgroud;
	private int MaxReplenishments;

	public SporadicServer(ProcessingGroupParameters PGP, PriorityParameters SP, PriorityParameters backgroud, int MaxReplenishments) {
		super(PGP, SP);
		this.backgroud = backgroud;
		this.MaxReplenishments = MaxReplenishments;
	}

	public PriorityParameters getBackgroud() {
		return backgroud;
	}

	public int getMaxReplenishments() {
		return MaxReplenishments;
	}

}
