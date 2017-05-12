package SPRY.Streaming.RealTime;

import java.util.HashMap;

import javax.realtime.AbsoluteTime;
import javax.realtime.AsyncEvent;
import javax.realtime.AsyncEventHandler;
import javax.realtime.Clock;
import javax.realtime.RelativeTime;

public class LatencyMonitor {
	protected static HashMap<Object, AbsoluteTime> dataToreceivingTime = new HashMap<>();	
	protected static HashMap<BatchedStream<?>, AsyncEvent> streamToEvent = new HashMap<>();
	protected static HashMap<BatchedStream<?>, RelativeTime> streamToLatency = new HashMap<>();
	protected static HashMap<Object, RelativeTime> dataToLatency = new HashMap<>();
	protected static HashMap<Object, AsyncEvent> dataToEvent = new HashMap<>();
	
	public static void addLatencyMissHandler(RelativeTime latency, AsyncEventHandler latencyMissHanlder, BatchedStream<?> stream){
		if (stream != null) {
			streamToLatency.put(stream, latency);
			AsyncEvent latencyMiss = new AsyncEvent();
			latencyMiss.setHandler(latencyMissHanlder);
			streamToEvent.put(stream, latencyMiss);
		}
	}
	
	public static void record(Object t, BatchedStream<?> stream) {
		if (t != null) {
			dataToreceivingTime.put(t, Clock.getRealtimeClock().getTime());
			dataToLatency.put(t, streamToLatency.get(stream));
			dataToEvent.put(t, streamToEvent.get(stream));
		}
	}
	
	public static void testLatencyMeet(Object t){
		if (t != null) {
			AbsoluteTime now = Clock.getRealtimeClock().getTime();
			AbsoluteTime recTime = dataToreceivingTime.get(t);
			RelativeTime latency = dataToLatency.get(t);
			AsyncEvent latencyMiss = dataToEvent.get(t);
			if (recTime != null && latency != null && latencyMiss!=null) {
				//System.out.println("Latency:\t" + now.subtract(recTime));
				if (now.subtract(recTime).compareTo(latency) > 0) latencyMiss.fire();		
				dataToreceivingTime.remove(t);
				dataToLatency.remove(t);
				dataToEvent.remove(t);
			}
		}
	}
}
