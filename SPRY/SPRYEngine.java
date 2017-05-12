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
import javax.realtime.PriorityParameters;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.realtime.Schedulable;

import SPRY.DataAllocation.DataAllocationPolicy;
import SPRY.ExecutionTimeServer.ProcessingGroup;
import SPRY.RealtimeForkJoinPool.Policy.SchedulingPolicy;
import SPRY.RealtimeForkJoinPool.RealtimeForkJoinPool;
import SPRY.Streaming.ReusableStream.ReusableReferencePipeline;
import SPRY.Streaming.ReusableStream.util.ReferencePipelineInitialiser;
import SPRY.Streaming.ReusableStream.util.ReusbaleStreamCallback;
/**
 * The real-time batch stream processing infrastructure.
 * */

public class SPRYEngine<T> {
	private RealtimeForkJoinPool rtPool = null;
	private ReusableReferencePipeline<T> pipeline = new ReusableReferencePipeline<T>(true);
	private ReusbaleStreamCallback<?> callback = null;
	private ProcessingGroup prologueServer = null;
	private BitSet affinitySettings = null;
	private HashMap<Schedulable, Boolean> registerRecords = new HashMap<>();
	private RelativeTime period = null;
	private RelativeTime deadline = null;
	private AbsoluteTime lastInvoke = null, release = null;
	private AsyncEvent deadlineMiss = new AsyncEvent();
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
		if (period != null) {
			now = Clock.getRealtimeClock().getTime();
			if (lastInvoke != null) if (now.subtract(lastInvoke).compareTo(period) < 0) {
				/* release the MIT violation handler */
				MITViolate.fire();
			}
			lastInvoke = new AbsoluteTime(now);
		}
		if (deadline != null) release = Clock.getRealtimeClock().getTime();
		
		/* set caller to the corresponding server */
		Thread t;
		if (prologueServer != null && (t = Thread.currentThread()) instanceof RealtimeThread) {
			if (registerRecords.get((RealtimeThread) t) == false) {
				registerRecords.put((RealtimeThread) t, true);
				prologueServer.register((RealtimeThread) t);
			}
		}
		final Runnable Driver = new Runnable() {
			@Override
			public void run() {
				pipeline.processData(data, callback);
			}
		};
		if (pipeline != null) {
			try {
				rtPool.submit(Driver).get();
				rtPool.waitForJobFinish();/**/
				
				/* release the deadline miss handler */
				if(deadline!=null){
					now = Clock.getRealtimeClock().getTime();
					if (now.subtract(release).compareTo(period) > 0)
						deadlineMiss.fire();
				}
			} catch (InterruptedException | ExecutionException e) {
			}
		}
	}

	public ReusbaleStreamCallback<?> getCallback() {
		return callback;
	}

	public void setCallback(ReusbaleStreamCallback<?> callback) {
		this.callback = callback;
	}
	
	public void setDeadlineMissHanlder(RelativeTime deadline, AsyncEventHandler deadlineMissHanlder){
		this.deadline = deadline;
		deadlineMiss.setHandler(deadlineMissHanlder);
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
