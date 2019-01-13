package SPRY.RealtimeForkJoinPool;

import java.util.BitSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;

import javax.realtime.Affinity;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;

import SPRY.ExecutionTimeServer.ProcessingGroup;
import SPRY.RealtimeForkJoinPool.Policy.SchedulingPolicy;

public class RealtimeForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {
	private PriorityParameters priority = new PriorityParameters(PriorityScheduler.instance().getMinPriority());
	private ProcessingGroup servers[] = null;
	private int nextAvailableProcessorId = 0;
	private int numberOfProcessors = 0;
	private int[] availableProcessors;
	private SchedulingPolicy schedulingPolicy = SchedulingPolicy.PartitionedScheduling;
	private int PrologueProcessorIndex = 0;

	public RealtimeForkJoinWorkerThreadFactory(PriorityParameters priority, ProcessingGroup... server) {
		this(priority, SchedulingPolicy.PartitionedScheduling, server);
	}

	public RealtimeForkJoinWorkerThreadFactory(PriorityParameters priority, SchedulingPolicy schdlPolicy, ProcessingGroup... server) {
		this(priority, schdlPolicy, null, server);
	}

	public RealtimeForkJoinWorkerThreadFactory(PriorityParameters priority, SchedulingPolicy schdlPolicy, BitSet affinity, ProcessingGroup... server) {
		this(priority, schdlPolicy, affinity, -1, server);
	}

	public RealtimeForkJoinWorkerThreadFactory(PriorityParameters priority, SchedulingPolicy schdlPolicy, BitSet affinity, int PrologueProcessor,
			ProcessingGroup... servers) {
		this.priority = priority;
		this.schedulingPolicy = schdlPolicy;
		getAvailableProcessors(affinity, PrologueProcessor);
		setServers(servers);
	}

	@Override
	public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
		if (servers != null) {
			/* allocates a server for each thread */
			if (servers.length > 0) {
				/* nextAvailableProcessorId is incremented after getNextAffinity() is invoked */
				return new RealtimeForkJoinWorkerThread(pool, priority, servers[(nextAvailableProcessorId % servers.length)], getNextAffinity());
			}
		}
		return new RealtimeForkJoinWorkerThread(pool, priority, null, getNextAffinity());
	}

	/**
	 * Get the available processors, including using the
	 * taskset -c
	 * command in Linux
	 */
	private void getAvailableProcessors(BitSet affinity, int PrologueProcessor) {
		if (affinity == null) affinity = Affinity.getAvailableProcessors();
		if (PrologueProcessor == -1) PrologueProcessor = getProcessor(Affinity.get(Thread.currentThread()).getProcessors());
		/* get how many processors are available */
		numberOfProcessors = 0;
		for (int i = 0; i < affinity.size(); i++)
			if (affinity.get(i) == true) numberOfProcessors++;
		/* init array */
		availableProcessors = new int[numberOfProcessors];
		int availableProcessorsCpy[] = new int[numberOfProcessors];
		/* set values */
		numberOfProcessors = 0;
		for (int i = 0; i < affinity.size(); i++) {
			if (affinity.get(i) == true) {
				availableProcessorsCpy[numberOfProcessors] = i;
				if (i == PrologueProcessor) PrologueProcessorIndex = numberOfProcessors;
				numberOfProcessors++;
			}
		}

		for (int i = 0; i < availableProcessorsCpy.length; i++) {
			availableProcessors[i] = availableProcessorsCpy[(PrologueProcessorIndex + i) % availableProcessorsCpy.length];
		}
	}

	private void setServers(ProcessingGroup servers[]) {
		if (servers == null) return;
		if (servers.length == 0) return;
		this.servers = new ProcessingGroup[servers.length];
		for (int i = 0; i < servers.length; i++) {
			this.servers[i] = servers[(PrologueProcessorIndex + i) % numberOfProcessors];
		}
	}

	/**
	 * return null if using global scheduling
	 */
	private Affinity getNextAffinity() {
		if (this.schedulingPolicy == SchedulingPolicy.GlobalScheduling) { return null; }
		BitSet targetProcessor = new BitSet();
		targetProcessor.set(getNextProcessorIndex());
		return Affinity.generate(targetProcessor);
	}

	private synchronized int getNextProcessorIndex() {
		int processorId = nextAvailableProcessorId;
		++nextAvailableProcessorId;
		return availableProcessors[(processorId % numberOfProcessors)];
	}

	private int getProcessor(BitSet affinity) {
		if (affinity == null) affinity = Affinity.getAvailableProcessors();
		for (int i = 0; i < affinity.size(); i++)
			if (affinity.get(i) == true) return i;
		return 0;
	}
}