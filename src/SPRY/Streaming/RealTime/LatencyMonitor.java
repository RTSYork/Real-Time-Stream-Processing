package SPRY.Streaming.RealTime;

import java.util.HashMap;

import javax.realtime.AbsoluteTime;
import javax.realtime.AsyncEvent;
import javax.realtime.AsyncEventHandler;
import javax.realtime.Clock;
import javax.realtime.OneShotTimer;
import javax.realtime.RelativeTime;
/** This class is used internally by SPRY. It records the data processing latency, and its corresponding handlers */
public class LatencyMonitor {
	protected static HashMap<Object, AbsoluteTime> dataToreceivingTime = new HashMap<>();	
	protected static HashMap<BatchedStream<?>, AsyncEvent> streamToEvent = new HashMap<>();
	protected static HashMap<BatchedStream<?>, RelativeTime> streamToLatency = new HashMap<>();
	protected static HashMap<BatchedStream<?>, AsyncEventHandler> streamToHanlder = new HashMap<>();
	protected static HashMap<Object, RelativeTime> dataToLatency = new HashMap<>();
	protected static HashMap<Object, AsyncEvent> dataToEvent = new HashMap<>();
	protected static HashMap<Object, OneShotTimer> dataToTimer = new HashMap<>();
	
	
	public static void addLatencyMissHandler(RelativeTime latency, AsyncEventHandler latencyMissHanlder, BatchedStream<?> stream){
		if (stream != null) {
			streamToLatency.put(stream, latency);
			AsyncEvent latencyMiss = new AsyncEvent();
			latencyMiss.setHandler(latencyMissHanlder);
			streamToEvent.put(stream, latencyMiss);
			streamToHanlder.put(stream, latencyMissHanlder);
		}
	}
	
	public static void record(Object t, BatchedStream<?> stream) {
		if (t != null) {
			AbsoluteTime now = Clock.getRealtimeClock().getTime();
			dataToreceivingTime.put(t, now);
			dataToLatency.put(t, streamToLatency.get(stream));
			dataToEvent.put(t, streamToEvent.get(stream));
			if (streamToHanlder.get(stream) != null) {
				OneShotTimer latencyMissMonitor = new OneShotTimer(now.add(streamToLatency.get(stream)), streamToHanlder.get(stream));
				dataToTimer.put(t, latencyMissMonitor);
				latencyMissMonitor.start();
			}
		}
	}
	
	public static void testLatencyMeet(Object t){
		if (t != null) {
			AbsoluteTime now = Clock.getRealtimeClock().getTime();
			AbsoluteTime recTime = dataToreceivingTime.get(t);
			RelativeTime latency = dataToLatency.get(t);
			AsyncEvent latencyMiss = dataToEvent.get(t);
			if (recTime != null && latency != null && latencyMiss!=null) {
				if (dataToTimer.get(t) != null) {
					try {
						dataToTimer.get(t).stop();
						dataToTimer.get(t).destroy();
					} catch (Exception e) {}
				}
				System.out.println(t + "\tLatency:  " + now.subtract(recTime).getMilliseconds());
				if (now.subtract(recTime).compareTo(latency) > 0) latencyMiss.fire();		
				dataToreceivingTime.remove(t);
				dataToLatency.remove(t);
				dataToEvent.remove(t);
			}
		}
	}
	
	public static void removeTimer(Object t){
		if (dataToTimer.get(t) != null) {
			try {
				dataToTimer.get(t).stop();
				dataToTimer.get(t).destroy();
			} catch (Exception e) {}
			dataToTimer.remove(t);
		}
	}
}
