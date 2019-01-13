package demo.RealtimeStreaming;
import java.util.BitSet;

import javax.realtime.AbsoluteTime;
import javax.realtime.Affinity;
import javax.realtime.AsyncEventHandler;
import javax.realtime.Clock;
import javax.realtime.PriorityParameters;
import javax.realtime.ProcessingGroupParameters;
import javax.realtime.ProcessorAffinityException;
import javax.realtime.RealtimeThread;
import javax.realtime.RelativeTime;

import SPRY.DataAllocation.CustomisedDataAllocationPolicy;
import SPRY.ExecutionTimeServer.DeferrableServer;
import SPRY.Streaming.RealTime.BatchedStream;
import SPRY.Streaming.RealTime.Receiver.RealtimeReceiver;

/* **************************************************************************************************************
Run using the following commands:
sudo taskset -c 0,1,2,3,4,5 jamaicavmm_slim_bin -Xbootclasspath/p:. -Dsun.boot.library.path=. CaseStudyEvaluation
 * **************************************************************************************************************/

public class CaseStudyEvaluation {

	public static RealtimeReceiver<Integer> receiver;
	public static final int MaxBatchSize = 17;
	public static AbsoluteTime startTime;
	public static int[] delays = { 5, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25 };//TODO
	public static long load = 10; //for mac TODO
	public static int itemId = 0;
	
	public static void main(String[] args) throws InterruptedException {
		Thread.currentThread().setPriority(38);
		if (args.length > 0) load = Long.parseLong(args[0]);
		double start = System.currentTimeMillis();
		for (int i = 0; i < 3 * load; i++)
			work();
		System.out.println(System.currentTimeMillis()-start);
		
		/* start time */
		startTime = Clock.getRealtimeClock().getTime();
		startTime = startTime.add(new RelativeTime(2000, 0));
		
		createHardRealtimeTasks(startTime);		
		createStreamProcessingTask();
		
		if (Clock.getRealtimeClock().getTime().compareTo(startTime) > 0)
			System.out.println("start time should be delayed");
	}
	
	private static void createStreamProcessingTask() {
		DeferrableServer[] servers = createServers(startTime);
		
		receiver = new RealtimeReceiver<Integer>(MaxBatchSize) {
			@Override
			public void onStart() {}
			@Override
			public void start() {}
			@Override
			public void onStop() {}
			@Override
			public void stop() {}			
		};
		
		RelativeTime timeout = new RelativeTime(400, 0); /* timeout */
		/* Allocates processor 1, 2, 3, 4 */
		BitSet affinities = new BitSet();
		affinities.set(1);
		affinities.set(2);
		affinities.set(3);
		affinities.set(4);
		CustomisedDataAllocationPolicy DAP = new CustomisedDataAllocationPolicy();
		DAP.addPairs(1, 2, 4, 8, 12, 16);
		DAP.addPairs(2, 3, 7, 10, 14);
		DAP.addPairs(3, 1, 5, 9, 13);
		DAP.addPairs(4, 0, 6, 11, 15);
		
		int prologueProessor = 1;
		
		BatchedStream<Integer> streaming = new BatchedStream<Integer>(
				receiver,
				timeout,
				11, 
				p -> p.forEachDeferred(x->{
					/* simulate the image generation work load */
					for (int i = 0; i < 40 * load; i++)
						work();
					//System.out.println(x + "\t" + Affinity.get(Thread.currentThread()).getProcessors());
					return;
				}),
				affinities, DAP, prologueProessor, servers
		);
		streaming.start(startTime);
		
		
		AsyncEventHandler latencyMissHandler = new AsyncEventHandler(){
			@Override
			public void handleAsyncEvent() {
				super.handleAsyncEvent();
				System.out.println("Latency Miss");
			}
		};
		
		RealtimeThread sender = new RealtimeThread(){
			@Override
			public void run(){
				try {
					sleep(startTime); /* sleep until startTime */
					
					/* warm up */
					for (int i = 0; i < 25; i++) {
						sleep(new RelativeTime(25, 0));
						receiver.store(-1);
					}
					sleep(500);
					streaming.setLatencyMissHandler(new RelativeTime(480, 0), latencyMissHandler);
					sleep(startTime.add(1600, 0));
					
					RTThreadGenerator.warmupFinished = true;
					for (int i = 0; i < delays.length; i++) {
						sleep(new RelativeTime(delays[i], 0));
						receiver.store(itemId++);
					}
					
					sleep(1000);
					System.exit(0);
				} catch (InterruptedException e) {}
			}
		};
		/* sender runs on processor 0 */
		BitSet senderAffinity = new BitSet();
		senderAffinity.set(0);
		try {
			Affinity.set(Affinity.generate(senderAffinity), sender);
		} catch (ProcessorAffinityException e) {
		}
		sender.start();
	}

	private static DeferrableServer[] createServers(AbsoluteTime startTime) {
		DeferrableServer S0 = createServer(startTime, 400, 314, 19); /* 55 --> 19 */
		DeferrableServer S1 = createServer(startTime, 400, 317, 18); /* 23 --> 18 */
		DeferrableServer S2 = createServer(startTime, 200, 156, 27); /* 71 --> 27 */
		DeferrableServer S3 = createServer(startTime, 100, 78, 22); /* 35 --> 22 */
		
		DeferrableServer servers[] = new DeferrableServer[4];
		servers[0]=S0;servers[1]=S1;servers[2]=S2;servers[3]=S3;
		return servers;
	}

	private static DeferrableServer createServer(AbsoluteTime startTime, int T, int C, int prioirty) {
		RelativeTime period, deadline;
		period = deadline = new RelativeTime(T,0);
		RelativeTime capacity = new RelativeTime(C,0);
		ProcessingGroupParameters pgp = new ProcessingGroupParameters(startTime, period, capacity, deadline, null, null);
		DeferrableServer s = new DeferrableServer(pgp, new PriorityParameters(prioirty), new PriorityParameters(10), new PriorityParameters(37));
		return s;
	}

	private static void createHardRealtimeTasks(AbsoluteTime start) {
		/* GAP tasks in processor 1 */
		int CPUID = 1;
		RealtimeThread WeaponRelease = /* 98 --> 28 */
				RTThreadGenerator.create(CPUID, 200, load * 3, 28, start, "Weapon Release");
		RealtimeThread WeaponAiming = /* 64 --> 24 */
				RTThreadGenerator.create(CPUID, 50, load * 3, 24, start, "Weapon Aiming");
		RealtimeThread NavUpdate = /* 56 --> 20 */
				RTThreadGenerator.create(CPUID, 59, load * 8, 20, start, "Nav Update");
		
		/* GAP tasks in processor 2 */
		CPUID = 2;
		RealtimeThread RaderTrackingFilter = /* 84 --> 28 */
				RTThreadGenerator.create(CPUID, 25, load * 2, 28, start, "Rader Tracking Filter");
		RealtimeThread DisplayGraphic = /* 40 --> 24 */
				RTThreadGenerator.create(CPUID, 80, load * 9, 24, start, "Display Graphic");
		RealtimeThread NavSteeringCmds = /* 24 --> 20 */
				RTThreadGenerator.create(CPUID, 200, load * 3, 20, start, "Nav Steering Cmds");
		
		/* GAP tasks in processor 3 */
		CPUID = 3;
		RealtimeThread RWRContactMgmt = /* 72 --> 28 */
				RTThreadGenerator.create(CPUID, 25, load * 5, 28, start, "RWR Contact Mgmt");
		RealtimeThread DisplayStoresUpdate = /* 20 --> 24 */
				RTThreadGenerator.create(CPUID, 200, load, 24, start, "Display Stores Update");
		RealtimeThread DisplayStatUpdate = /* 12 --> 20 */
				RTThreadGenerator.create(CPUID, 200, load * 3, 20, start, "Display Stat Update");
		
		/* GAP tasks in processor 4 */
		CPUID = 4;
		RealtimeThread DataBusPollDevice = /* 68 --> 28 */
				RTThreadGenerator.create(CPUID, 40, load, 28, start, "Data Bus Poll Device");
		RealtimeThread RadarTargetUpdate = /* 60 --> 26 */
				RTThreadGenerator.create(CPUID, 50, load * 5, 26, start, "Radar Target Update");
		RealtimeThread DisplayHookUpdate = /* 36 --> 24 */
				RTThreadGenerator.create(CPUID, 80, load * 2, 24, start, "Display Hook Update");
		RealtimeThread TrackingTargetUpdate = /* 32 --> 20 */
				RTThreadGenerator.create(CPUID, 100, load * 5, 20, start, "Tracking Target Update");
		RealtimeThread DisplayKeySet = /* 16 --> 18 */
				RTThreadGenerator.create(CPUID, 200, load, 18, start, "Display Key Set");
		RealtimeThread BETEStatusUpdate = /* 8 --> 16 */
				RTThreadGenerator.create(CPUID, 1000, load, 16, start, "BET E Status Update");
		RealtimeThread NavStatus = /* 4 --> 14 */
				RTThreadGenerator.create(CPUID, 1000, load, 14, start, "Nav Status");
		
		
		/*start all GAP tasks */
		WeaponRelease.start();
		WeaponAiming.start();
		NavUpdate.start();

		RaderTrackingFilter.start();
		DisplayGraphic.start();
		NavSteeringCmds.start();

		RWRContactMgmt.start();
		DisplayStoresUpdate.start();
		DisplayStatUpdate.start();

		DataBusPollDevice.start();
		RadarTargetUpdate.start();
		DisplayHookUpdate.start();
		TrackingTargetUpdate.start();
		DisplayKeySet.start();
		BETEStatusUpdate.start();
		NavStatus.start();
	}

	public static void work() {
		for (int i = 0; i < 5400; i++)
			if (999 * 88 + 938857361 / 32 + 876544 > 0) ;
	}
}