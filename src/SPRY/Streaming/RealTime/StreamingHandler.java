package SPRY.Streaming.RealTime;

import javax.realtime.BoundAsyncEventHandler;
import javax.realtime.PriorityParameters;

public class StreamingHandler extends BoundAsyncEventHandler {
	@SuppressWarnings("rawtypes")
	private BatchedStream target;

	@SuppressWarnings("rawtypes")
	public StreamingHandler(PriorityParameters priority, BatchedStream target) {
		super(priority, null, null, null, null, false, null);
		this.target = target;
	}
	
	@Override
	public void handleAsyncEvent() {
		//System.out.println("####### Release at: "+System.currentTimeMillis());
		target.releaseBatch();
	}
	
	public void resetNextTimeout(){
		target.resetNextTimeout();
	}
}