package SPRY.RealtimeForkJoinPool;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.BitSet;
import java.util.concurrent.ForkJoinPool;

import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;

import SPRY.DataAllocation.DataAllocationPolicy;
import SPRY.DataAllocation.EvenDataAllocationPolicy;
import SPRY.ExecutionTimeServer.ProcessingGroup;
import SPRY.RealtimeForkJoinPool.Policy.SchedulingPolicy;

public class RealtimeForkJoinPool extends ForkJoinPool {
	
	public static final int minPriority = 1;
	public static final int maxPriority = PriorityScheduler.instance().getMaxPriority();
	public static final int defaultPriority = PriorityScheduler.instance().getMinPriority();
	public static final boolean defaultAsyncMode = true; // FIFO for pop task from local queue

	private int jobs = 0;
	private final Object lock = new Object();
	private DataAllocationPolicy DataAllocationPolicy;
	public RealtimeForkJoinPool() {
		this(Runtime.getRuntime().availableProcessors(),
				new RealtimeForkJoinWorkerThreadFactory(new PriorityParameters(defaultPriority), SchedulingPolicy.PartitionedScheduling), null,
				defaultAsyncMode);
	}
	
	public RealtimeForkJoinPool(PriorityParameters priority) {
		this(Runtime.getRuntime().availableProcessors(), priority, SchedulingPolicy.PartitionedScheduling, null, defaultAsyncMode);
	}

	public RealtimeForkJoinPool(PriorityParameters priority, ProcessingGroup... server) {
		this(Runtime.getRuntime().availableProcessors(), priority, SchedulingPolicy.PartitionedScheduling, null, defaultAsyncMode, server);
	}

	public RealtimeForkJoinPool(PriorityParameters priority, SchedulingPolicy schdlPolicy, ProcessingGroup... server) {
		this(Runtime.getRuntime().availableProcessors(), priority, schdlPolicy, null, defaultAsyncMode, server);
	}

	public RealtimeForkJoinPool(int parallelism, PriorityParameters priority, SchedulingPolicy schdlPolicy, ProcessingGroup... server) {
		this(parallelism, priority, schdlPolicy, null, defaultAsyncMode, server);
	}

	public RealtimeForkJoinPool(int parallelism, PriorityParameters priority, SchedulingPolicy schdlPolicy, BitSet affinity, ProcessingGroup... server) {
		this(parallelism, priority, schdlPolicy, affinity, defaultAsyncMode, server);
	}

	public RealtimeForkJoinPool(int parallelism, PriorityParameters priority, SchedulingPolicy schdlPolicy, BitSet affinity, boolean asyncMode,
			ProcessingGroup... server) {
		this(parallelism, priority, schdlPolicy, affinity, asyncMode, new EvenDataAllocationPolicy(parallelism), server);
	}

	public RealtimeForkJoinPool(int parallelism, PriorityParameters priority, SchedulingPolicy schdlPolicy, BitSet affinity, boolean asyncMode, DataAllocationPolicy shuffle,
			ProcessingGroup... server) {
		this(parallelism, new RealtimeForkJoinWorkerThreadFactory(priority, schdlPolicy, affinity, server), null, asyncMode);
		this.DataAllocationPolicy = shuffle;
	}
	
	public RealtimeForkJoinPool(int parallelism, PriorityParameters priority, SchedulingPolicy schdlPolicy, BitSet affinity, boolean asyncMode, DataAllocationPolicy shuffle,
			int prologueProcessor, ProcessingGroup... server) {
		this(parallelism, new RealtimeForkJoinWorkerThreadFactory(priority, schdlPolicy, affinity, prologueProcessor, server), null, asyncMode);
		this.DataAllocationPolicy = shuffle;
	}

	public RealtimeForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode) {
		super(parallelism, factory, handler, asyncMode);
		/* Population of worker threads */
		this.createAllWorkerThreads();
	}
	
	/**
	 * Return a pre-defined Real-time ForkJoinPool with a given priority,
	 * where all the worker threads are aperiodic threads.
	 * Return a common pool with Java priority if the given priority is not in
	 * the range of RTSJ.
	 */
	public static ForkJoinPool getRTForkJoinPool(PriorityParameters priorityPmtr) {
		int priority = priorityPmtr.getPriority();
		return getCommonPool(priority);
	}

	public void increaseJobsleftCount(int count) {
		synchronized (lock) {
			jobs += count;
			if (jobs <= 0) lock.notifyAll();
		}
	}

	public void waitForJobFinish() {
		synchronized (lock) {
			if (jobs > 0) try {
				lock.wait();
			} catch (InterruptedException e) {
			}
		}
	}

	public DataAllocationPolicy getAllocationPolicy() {
		if (DataAllocationPolicy == null) return new EvenDataAllocationPolicy(getParallelism());
		return DataAllocationPolicy;
	}

	public void setAllocationPolicy(DataAllocationPolicy shuffle) {
		this.DataAllocationPolicy = shuffle;
	}

	public void enablesStaticTaskAllocation(boolean enabled) {
		this.PreAllocationEnabled = enabled;
	}
}
