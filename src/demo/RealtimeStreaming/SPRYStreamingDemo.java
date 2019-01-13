package demo.RealtimeStreaming;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.realtime.AsyncEventHandler;
import javax.realtime.PriorityParameters;
import javax.realtime.RelativeTime;

import SPRY.ExecutionTimeServer.ProcessingGroup;
import SPRY.Streaming.RealTime.BatchedStream;
import SPRY.Streaming.RealTime.Receiver.RealtimeReceiver;
import SPRY.Streaming.RealTime.Receiver.StringSocketRealtimeReceiver;

public class SPRYStreamingDemo {
	private static final int BUFFER_SIZE = 2048;
	public static ConcurrentSkipListMap<String,Long> res = new ConcurrentSkipListMap<String,Long>();
	public static void main(String[] args){
		new SPRYStreamingDemo().StringPrintPipeline();
	}
	
	public void StringPrintPipeline(){
		int priority = 29;
		ProcessingGroup server = null;
		RelativeTime timeout= new RelativeTime(3000,0); /* timeout */
		
		RelativeTime latency = new RelativeTime(10,0), dataMIT = new RelativeTime(10000,0);
		AsyncEventHandler microBatchDeadlineMissHandler = new AsyncEventHandler(){
			@Override
			public void handleAsyncEvent() {
				super.handleAsyncEvent();
				System.out.println("Micro-Batch Processing Deadline Miss");
			}
		};
		
		AsyncEventHandler dataIncomingMITViolationHandler = new AsyncEventHandler(){
			@Override
			public void handleAsyncEvent() {
				super.handleAsyncEvent();
				System.out.println("Data Incoming MIT Violation");
			}
		};
		
		AsyncEventHandler latencyMissHandler = new AsyncEventHandler(){
			@Override
			public void handleAsyncEvent() {
				super.handleAsyncEvent();
				System.out.println("Latency Miss");
			}
		};
		/* create the receiver */
		RealtimeReceiver<String> receiver = new StringSocketRealtimeReceiver("localhost", 1989, BUFFER_SIZE, new PriorityParameters(priority), server);
		
		BatchedStream<String> textStreaming = new BatchedStream<>(
				receiver,
				timeout,
				priority, 
				p -> p.map(x->x.toUpperCase()).forEachDeferred(x->System.out.println(x)), /* the pipeline */
				server
		);
		
		textStreaming.setBatchProcessingDeadlineMissHandler(microBatchDeadlineMissHandler);
		textStreaming.setDataIncomingMITViolationHandler(dataMIT, dataIncomingMITViolationHandler);		
		textStreaming.setLatencyMissHandler(latency, latencyMissHandler);
		
		textStreaming.start();
	}	
}