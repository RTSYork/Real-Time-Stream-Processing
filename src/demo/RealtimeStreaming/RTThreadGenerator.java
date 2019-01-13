package demo.RealtimeStreaming;
import java.util.BitSet;
import java.util.HashMap;

import javax.realtime.AbsoluteTime;
import javax.realtime.Affinity;
import javax.realtime.AsyncEventHandler;
import javax.realtime.Clock;
import javax.realtime.HighResolutionTime;
import javax.realtime.PeriodicParameters;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.ProcessingGroupParameters;
import javax.realtime.ProcessorAffinityException;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;
import javax.realtime.Schedulable;

import SPRY.ExecutionTimeServer.DeferrableServer;

public class RTThreadGenerator {
	private static int THREAD_ID = 0;
	private static boolean stop = false;
	private static long iterations = 0;
	private static HashMap<Thread, Integer> releaseCounter = new HashMap<>();
	
	public static boolean warmupFinished = false;

	public static void main(String[] args) {
		/* work load estimating
		 * Note that, using taskset -c 0
		 * otherwise cost monitoring maybe not working accurately in jamaica */
		int c = 200;
		long load = EstimateWorkLoad(c);
		System.out.println(load);

		AbsoluteTime start = Clock.getRealtimeClock().getTime();
		start = start.add(new RelativeTime(500, 0));
		RealtimeThread t = create(0, 300, load, 37, start);
		RealtimeThread t2 = create(0, 400, load, 21, start);
		t.start();
		t2.start();
	}

	public static RealtimeThread create(int cpu, long period, long load, int prio, AbsoluteTime firstRelease) {
		return create(cpu, period, load, prio, firstRelease, "RealTimeThread " + THREAD_ID++);
	}

	public static RealtimeThread create(int cpu, long period, long load, int prio, AbsoluteTime firstRelease, String name) {
		RelativeTime D, T;
		D = T = new RelativeTime(period, 0);
		
		PriorityParameters priority = new PriorityParameters(prio);

		RealtimeThread thread = new RealtimeThread(priority, null) {
			@Override
			public void run() {
				while (true) {
//					System.out.println(getName());
					waitForNextPeriod();
					int i = 0;
					for (; i < load; i++)
						work();
//					Clock clk = Clock.getRealtimeClock();
//					AbsoluteTime end = clk.getTime();
//					int releaseC = releaseCounter.get(this);
//					long R = (end.subtract(firstRelease).getMilliseconds()) - period*releaseC;
//					releaseCounter.put(this, ++releaseC);
//					System.out.println(String.format("%s\tR = %d ms", getName(), R));// firstRelease.getMilliseconds()+"\t"+end.getMilliseconds()+"\t"+(--releaseC);
				}
			}
		};
		thread.setName(name + " (Priority = " + prio+")");
		
		PeriodicParameters periodicParameters = new PeriodicParameters(null, T, null, D, null, new AsyncEventHandler(){
			@Override
			public void handleAsyncEvent() {
				if (warmupFinished) System.out.println("\t- " + thread.getName() + "\tDeadline Miss");
				thread.schedulePeriodic();
			}
		});
		thread.setReleaseParameters(periodicParameters);
		
		releaseCounter.put(thread, 0);
		/* set affinity */
		BitSet processor = new BitSet();
		processor.set(cpu);
		try {
			Affinity.set(Affinity.generate(processor), thread);
		} catch (ProcessorAffinityException e) {
		}
		return thread;
	}
	
	public static void work() {
		for (int i = 0; i < 5000; i++)
			if (999 * 88 + 938857361 / 32 + 876544 > 0) ;
	}

	public static long EstimateWorkLoad(long wcet_in_ms){
		iterations = 0;
		stop = false;
		
		PriorityParameters max_priority = new PriorityParameters(PriorityScheduler.instance().getMaxPriority());
		PriorityParameters high_priority = new PriorityParameters(PriorityScheduler.instance().getMaxPriority() - 7);
		HighResolutionTime start = Clock.getRealtimeClock().getTime().add(1000,0);
		RelativeTime period = new RelativeTime(wcet_in_ms * 10, 0);
		RelativeTime C = new RelativeTime(wcet_in_ms>=1?wcet_in_ms:1, 0);

		PeriodicParameters release = new PeriodicParameters(start, period, C, period, null, null);
		RealtimeThread estimator = new RealtimeThread(high_priority, release) {
			@Override
			public void run() {
				waitForNextPeriod();
				int i = 0;
				for (; !stop; i++) {
					work();
					if (i > 1000000) break;
				}
				iterations = i;
				//System.out.println("iterations=" + iterations);
			}
		};
		
		/* set affinity */
		BitSet targetProcessor = new BitSet();
		targetProcessor.set(0);
		try {
			Affinity.set(Affinity.generate(targetProcessor), estimator);
		} catch (ProcessorAffinityException e) {
		}
		
		ProcessingGroupParameters p = new ProcessingGroupParameters(start, period, C, period, null, null);
		DeferrableServer ds = new DeferrableServer(p, high_priority, high_priority, max_priority) {
			@Override public void afterExhausted(Schedulable so) {
				stop = true;
			}
		};
		ds.register(estimator);
		
		estimator.start();
		try {
			estimator.join();
		} catch (InterruptedException e1) {
		}
		
		return iterations;
	}
}
