package SPRY.Streaming.RealTime;

import java.util.BitSet;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import javax.realtime.AbsoluteTime;
import javax.realtime.Affinity;
import javax.realtime.AsyncEventHandler;
import javax.realtime.PeriodicTimer;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.ProcessorAffinityException;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;

import SPRY.SPRYEngine;
import SPRY.DataAllocation.DataAllocationPolicy;
import SPRY.ExecutionTimeServer.ProcessingGroup;
import SPRY.Streaming.RealTime.Receiver.RealtimeReceiver;
import SPRY.Streaming.SPRYStream.SPRYReferencePipeline;
import SPRY.Streaming.SPRYStream.util.ReferencePipelineInitialiser;
import SPRY.Streaming.SPRYStream.util.SPRYStreamCallback;

public class BatchedStream<T>{
	protected RealtimeReceiver<T> receiver;
	protected StreamingHandler AEH;
	protected PeriodicTimer timer;
	protected SPRYReferencePipeline<T> pipeline = null;
	protected SPRYStreamCallback<?> callback = null;
	protected PriorityParameters priority = null;
	protected boolean running = false;
	protected CountDownLatch barrier = new CountDownLatch(0);
	protected SPRYEngine<T> engine = null;
	protected RelativeTime microBatchingTimeout = null;
	protected RelativeTime dataMIT = null;
	protected AsyncEventHandler engineDeadlineMissHanlder = null;

	public BatchedStream(RealtimeReceiver<T> receiver, RelativeTime interval, int priority, ReferencePipelineInitialiser<T> initialiser,
			ProcessingGroup... servers) {
		this(receiver, interval, priority, initialiser, null, null, getProcessor(Affinity.get(Thread.currentThread()).getProcessors()), servers);
	}

	public BatchedStream(RealtimeReceiver<T> receiver, RelativeTime interval, int priority, ReferencePipelineInitialiser<T> initialiser,
			BitSet affinities, DataAllocationPolicy DAP, int prologueProessor, ProcessingGroup... servers) {
		this.microBatchingTimeout = interval;
		this.receiver = receiver;
		this.pipeline = new SPRYReferencePipeline<T>(true);
		initialiser.initialise(pipeline);
		this.priority = new PriorityParameters(priority);
		AEH = createAEH();
		timer = new PeriodicTimer(null, interval, AEH);
		receiver.setBufferFullEventHandler(AEH);
		
		BitSet affinity = affinities;
		if(affinity==null) affinity = Affinity.getAvailableProcessors();
		engine = new SPRYEngine<>(priority, initialiser, affinity, DAP, prologueProessor, servers);
		/* set handler affinity and server */
		int prologuePIndex = 0;
		for (int i = 0; i < affinity.size(); i++) {
			if (affinity.get(i) == true) {
				if (i == prologueProessor) break;
				prologuePIndex++;
			}
		}
		if (servers != null) {
			if (servers.length > prologuePIndex) {
				ProcessingGroup s = servers[prologuePIndex];
				if (s != null) s.register(AEH);
			}
		}
		BitSet rp = new BitSet();
		rp.set(prologueProessor);
		try {
			Affinity.set(Affinity.generate(rp),AEH);
		} catch (ProcessorAffinityException e) {}
	}
	
	public void start(){
		if (!running) {
			running = true;
			if (timer != null) timer.start(); /* start the timer */
			if (receiver != null) receiver.start(); /* start the receiver */
			barrier = new CountDownLatch(1);
		}
	}
	
	public void start(AbsoluteTime startTime) {
		if (!running) {
			timer = new PeriodicTimer(startTime, this.microBatchingTimeout, AEH);
			start();
		}
	}
	
	public void stop(){
		if(running){
			running=false;
			if(timer!=null)	timer.destroy();
			if(receiver!=null)	receiver.stop();
			barrier.countDown();
		}
	}

	public void releaseBatch(){
//		System.out.println("-----------------------------------\nBatch at time: "+System.currentTimeMillis()+
//							"\n-----------------------------------");
//		System.out.println("-----------------------------------");
		Collection<T> data = receiver.retrieve();
		if (data != null) {
			if (data.size() > 0) {
				//System.out.println(data);
				engine.processBatch(data);
			}
		}
	}
	
	private StreamingHandler createAEH(){
		/* the priority for AsynchronizedEventHandler */
		PriorityParameters priority = this.priority;
		if(priority == null){
			int prio = PriorityScheduler.instance().getMinPriority();
			if (Thread.currentThread() instanceof RealtimeThread) {
				prio = ((PriorityParameters) RealtimeThread.currentRealtimeThread().getSchedulingParameters()).getPriority();
			}
			priority = new PriorityParameters(prio);
		}
		StreamingHandler handler = new StreamingHandler(priority, this);
		return handler;
	}
	
	public void resetNextTimeout(){
		if (timer != null) {
			try {
				timer.stop();
				timer.reschedule(timer.getInterval());
				timer.start();
			} catch (java.lang.IllegalStateException e) {
				System.out.println("resetNextTimeout failed : " + e.getMessage());
			}
		}
	}
	
	public void setBatchProcessingDeadlineMissHandler(AsyncEventHandler deadlineMissHanlder){
		engineDeadlineMissHanlder = deadlineMissHanlder;
		if (dataMIT != null) {
			long batchSize = receiver.getBufferSize();
			long a = dataMIT.getMilliseconds() * batchSize;
			long b = microBatchingTimeout.getMilliseconds();
			long MIT = Math.min(a, b);
			engine.setDeadlineMissHandler(new RelativeTime(MIT, 0), deadlineMissHanlder);
		}
		engine.setDeadlineMissHandler(microBatchingTimeout, deadlineMissHanlder);
	}

	public void setDataIncomingMITViolationHandler(RelativeTime dataMIT, AsyncEventHandler MITViolateHandler) {
		receiver.setMITViolateHandler(dataMIT, MITViolateHandler);
		if (engineDeadlineMissHanlder == null)
			this.dataMIT = dataMIT;
		else {
			long batchSize = receiver.getBufferSize();
			long a = dataMIT.getMilliseconds() * batchSize;
			long b = microBatchingTimeout.getMilliseconds();
			if (a < b) engine.setDeadlineMissHandler(new RelativeTime(a, 0), engineDeadlineMissHanlder);
		}
	}
	
	public void setLatencyMissHandler(RelativeTime latency, AsyncEventHandler latencyMissHanlder){
		LatencyMonitor.addLatencyMissHandler(latency, latencyMissHanlder, this);
		receiver.setLatencyRecording(true, this);
	}
	
	public void awaitForTermination(){
		try {
			barrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public SPRYStreamCallback<?> getCallback() {
		return callback;
	}
	public void setCallback(SPRYStreamCallback<?> callback) {
		this.callback = callback;
		engine.setCallback(callback);
	}
	
	private static int getProcessor(BitSet affinity) {
		if (affinity == null) affinity = Affinity.getAvailableProcessors();
		for (int i = 0; i < affinity.size(); i++)
			if (affinity.get(i) == true) return i;
		return 0;
	}
}