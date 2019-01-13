package SPRY;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.realtime.AbsoluteTime;
import javax.realtime.Affinity;
import javax.realtime.AsyncEvent;
import javax.realtime.AsyncEventHandler;
import javax.realtime.Clock;
import javax.realtime.OneShotTimer;
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.realtime.Schedulable;

import SPRY.DataAllocation.DataAllocationPolicy;
import SPRY.ExecutionTimeServer.ProcessingGroup;
import SPRY.RealtimeForkJoinPool.Policy.SchedulingPolicy;
import SPRY.RealtimeForkJoinPool.RealtimeForkJoinPool;
import SPRY.Streaming.SPRYStream.SPRYReferencePipeline;
import SPRY.Streaming.SPRYStream.util.ReferencePipelineInitialiser;
import SPRY.Streaming.SPRYStream.util.SPRYStreamCallback;
/**
 * The real-time batch stream processing infrastructure.
 * */

public class SPRYEngine<T> {
	private RealtimeForkJoinPool rtPool = null;
	private SPRYReferencePipeline<T> pipeline = new SPRYReferencePipeline<T>(true);
	private SPRYStreamCallback<?> callback = null;
	private ProcessingGroup prologueServer = null;
	private BitSet affinitySettings = null;
	private HashMap<Schedulable, Boolean> registerRecords = new HashMap<>();
	private RelativeTime period = null;
	private RelativeTime deadline = null;
	private AbsoluteTime lastRelease = null, release = null;
	private AsyncEventHandler deadlineMissHandler;
	private AsyncEvent MITViolate = new AsyncEvent();
	

	/**
	 * The real-time batch stream processing infrastructure interface. The prologue processor is
	 * assumed to be the current running processor, which invokes this method.
	 * 
	 * @param
	 * 			priority
	 *            -- The priority of the stream processing infrastructure
	 * @param
	 * 			initialiser
	 *            -- pipeline initialiser
	 * @param
	 * 			affinities
	 *            -- threads' affinities, also indicates how many processors
	 *            will be used. When null, use all availables.
	 * @param
	 * 			servers
	 *            -- execution-time servers, for each thread, has same order
	 *            with affinities.
	 */
	public SPRYEngine(int priority, ReferencePipelineInitialiser<T> initialiser, BitSet affinities, DataAllocationPolicy DAP,
			ProcessingGroup... servers) {
		this(priority, initialiser, affinities, DAP, getProcessor(Affinity.get(Thread.currentThread()).getProcessors()), servers);
	}

	/**
	 * The real-time batch stream processing infrastructure interface.
	 * 
	 * @param
	 * 			priority
	 *            -- The priority of the stream processing infrastructure
	 * @param
	 * 			initialiser
	 *            -- pipeline initialiser
	 * @param
	 * 			affinities
	 *            -- threads' affinities, also indicates how many processors
	 *            will be used. When null, use all availables.
	 * @param
	 * 			prologueProessor
	 *            -- the prologue processor
	 * @param
	 * 			servers
	 *            -- execution-time servers, for each thread, has same order
	 *            with affinities.
	 */
	public SPRYEngine(int priority, ReferencePipelineInitialiser<T> initialiser, BitSet affinities, DataAllocationPolicy DAP,
			int prologueProessor, ProcessingGroup... servers) {
		initialiser.initialise(pipeline);
		int parallelism = analyseAffinity(affinities);
		rtPool = new RealtimeForkJoinPool(parallelism, new PriorityParameters(priority), SchedulingPolicy.PartitionedScheduling, affinities,
				false, DAP, prologueProessor, servers);
		rtPool.enablesStaticTaskAllocation(true);/**/
		getPrologueServer(affinitySettings, prologueProessor, servers);
	}

	public void processBatch(Collection<T> data) {
		AbsoluteTime now;
		OneShotTimer deadlineMissMonitor = null;
		if (period != null) {
			now = Clock.getRealtimeClock().getTime();
			if (lastRelease != null) if (now.subtract(lastRelease).compareTo(period) < 0) {
				/* release the MIT violation handler */
				MITViolate.fire();
			}
			lastRelease = new AbsoluteTime(now);
		}
		if (deadline != null) {
			release = Clock.getRealtimeClock().getTime();
			deadlineMissMonitor = new OneShotTimer(release.add(deadline), deadlineMissHandler);
			deadlineMissMonitor.start();
		}
		
		/* set caller to the corresponding server */
		Thread t;
		if (prologueServer != null && (t = Thread.currentThread()) instanceof RealtimeThread) {
			if (t != null) if (registerRecords.get((RealtimeThread) t) != null) if (registerRecords.get((RealtimeThread) t) == false) {
				registerRecords.put((RealtimeThread) t, true);
				prologueServer.register((RealtimeThread) t);
			}
		}
		final Runnable Driver = new Runnable() {
			@Override
			public void run() {
				try {
					pipeline.processData(data, callback);
				} catch (Exception e) {
				}
			}
		};
		if (pipeline != null) {
			try {
				rtPool.resetItemArrivalCounter();
				rtPool.submit(Driver).get();
				rtPool.waitForJobFinish();/**/
				
				/* destroy the deadline miss monitor */
				if (deadline != null) {
					now = Clock.getRealtimeClock().getTime();
					if (now.subtract(release).compareTo(deadline) <= 0) {
						if (deadlineMissMonitor != null) {
							try{
								deadlineMissMonitor.stop();
								deadlineMissMonitor.destroy();
							} catch (Exception e) {}
						}
					}
				}
			} catch (InterruptedException | ExecutionException e) {
			}
		}
	}

	public SPRYStreamCallback<?> getCallback() {
		return callback;
	}

	public void setCallback(SPRYStreamCallback<?> callback) {
		this.callback = callback;
	}
	
	public void setDeadlineMissHandler(RelativeTime deadline, AsyncEventHandler deadlineMissHandler){
		this.deadline = deadline;
		this.deadlineMissHandler = deadlineMissHandler;
	}
	
	public void setMITViolateHandler(RelativeTime period, AsyncEventHandler MITViolateHandler){
		this.period = period;
		MITViolate.setHandler(MITViolateHandler);
	}

	private int analyseAffinity(BitSet affinity) {
		if (affinity == null) affinity = Affinity.getAvailableProcessors();
		affinitySettings = affinity;
		/* get how many processors are available */
		int numberOfProcessors = 0;
		for (int i = 0; i < affinity.size(); i++)
			if (affinity.get(i) == true) numberOfProcessors++;
		return numberOfProcessors;
	}

	private static int getProcessor(BitSet affinity) {
		if (affinity == null) affinity = Affinity.getAvailableProcessors();
		for (int i = 0; i < affinity.size(); i++)
			if (affinity.get(i) == true) return i;
		return 0;
	}

	private void getPrologueServer(BitSet affinities, int prologueProessor, ProcessingGroup... servers) {
		int prologueProcessorIndex = 0;
		if (affinities == null) return;
		for (int i = 0; i < affinities.size(); i++) {
			if (affinities.get(i) == true) {
				if (i == prologueProessor) break;
				prologueProcessorIndex++;
			}
		}
		if (servers != null) {
			for (int i = 0; i < servers.length; i++) {
				if (i == prologueProcessorIndex) prologueServer = servers[i];
			}
		}
	}
}
