package SPRY.Streaming.RealTime.Receiver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import javax.realtime.AbsoluteTime;
import javax.realtime.AsyncEvent;
import javax.realtime.AsyncEventHandler;
import javax.realtime.Clock;
import javax.realtime.RelativeTime;

import SPRY.Streaming.RealTime.BatchedStream;
import SPRY.Streaming.RealTime.LatencyMonitor;
import SPRY.Streaming.RealTime.StreamingHandler;
import SPRY.Streaming.Receiver.Receiver;

public abstract class RealtimeReceiver<T> extends Receiver<T>{
	
	protected int bufferSize;
	protected long bufferUsed = 0;
	protected AsyncEvent bufferFullEvent = new AsyncEvent();
	protected StreamingHandler handler = null;
	private boolean retrieved = true;

	private AbsoluteTime lastDataIncome = null;
	private RelativeTime dataMIT = null;
	private AsyncEvent MITViolate = new AsyncEvent();

	private boolean recordLatency = false;
	private BatchedStream<T> stream;
	
	public void setMITViolateHandler(RelativeTime dataMIT, AsyncEventHandler MITViolateHandler){
		this.dataMIT = dataMIT;
		MITViolate.setHandler(MITViolateHandler);
	}
	
	public RealtimeReceiver(int bufferSize){
		this(bufferSize, null, null);
	}
	
	public RealtimeReceiver(int bufferSize, StreamingHandler AEH){
		this(bufferSize, null, AEH);
	}
	
	public RealtimeReceiver(int bufferSize, Predicate<? super T> filter, StreamingHandler handler) {
		this.filter = filter;
		this.bufferSize = bufferSize;
		this.buffer = new ArrayList<T>(bufferSize);
		this.handler = handler;
	}
	
	public long getBufferSize() {	return bufferSize;	}

	public void setBufferSize(int bufferSize) {	this.bufferSize = bufferSize;	}
	
	public void setBufferFullEventHandler(StreamingHandler AEH) {
		this.handler = AEH;
		if (bufferFullEvent != null) bufferFullEvent.setHandler(AEH);
	}	
	
	@Override
	public synchronized void store(T t) {
		AbsoluteTime now;
		if (dataMIT != null) {
			now = Clock.getRealtimeClock().getTime();
			if (lastDataIncome != null) if (now.subtract(lastDataIncome).compareTo(dataMIT) < 0) {
				/* release the MIT violation handler */
				MITViolate.fire();
			}
			lastDataIncome = new AbsoluteTime(now);
		}
		if (recordLatency) LatencyMonitor.record(t, stream);
		super.store(t);
		bufferUsed++;
		/* ******************************************************************
		 * Test retrieved when notify the handler (i.e. onBufferIsFull())
		 * to avoid the situation where release multiple handler,
		 * but the handler hasn't retrieve data yet, it notifies the handler
		 * again and again.
		 * ******************************************************************/
		if (bufferUsed >= bufferSize && retrieved) {
			onBufferIsFull();
			retrieved = false;
		}
	}

	@Override
	public synchronized Collection<T> retrieve() {
		bufferUsed = 0;
		List<T> inputs = buffer;
		buffer = new ArrayList<T>(bufferSize);
		retrieved = true;
		return inputs;
	}
	
	public void onBufferIsFull(){
		if(bufferFullEvent!=null){
			bufferFullEvent.fire();
			if (handler != null) {
				handler.resetNextTimeout();
			}
		}
	}

	public void setLatencyRecording(boolean recordLatency, BatchedStream<T> stream) {
		this.recordLatency = recordLatency;
		this.stream = stream;
	}
}