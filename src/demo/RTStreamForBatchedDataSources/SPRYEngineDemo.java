package demo.RTStreamForBatchedDataSources;
import java.util.ArrayList;
import java.util.BitSet;

import javax.realtime.Affinity;
import javax.realtime.AsyncEventHandler;
import javax.realtime.ProcessorAffinityException;
import javax.realtime.RelativeTime;

import SPRY.SPRYEngine;
import SPRY.DataAllocation.DataAllocationPolicy;
import SPRY.ExecutionTimeServer.DeferrableServer;

public class SPRYEngineDemo {
	public static final int parallelism = 2;
	public static final int dataSize = 16;
	public static final int PrologueProcessor = 1;
	public static final int priority = 32;
	
	public static void main(String[] args) throws ProcessorAffinityException {
		Thread.currentThread().setPriority(37);
		/* set the release processor */
		BitSet rp = new BitSet();
		rp.set(PrologueProcessor);
		Affinity.set(Affinity.generate(rp), Thread.currentThread());
		
		/* prepare the data */
		ArrayList<Integer> data = new ArrayList<>();
		for (int i = 0; i < dataSize; i++)	data.add(i);

		DeferrableServer[] servers = null;
		DataAllocationPolicy DAP = null;
		SPRYEngine<Integer> spry = new SPRYEngine<>(priority, p -> p.map(x -> {
			double a = 1.1;
			for (int i = 0; i < 10000; i++)
				a = a * 1.1;
			return x;
		}).forEachDeferred(x -> {
			System.out.println(x + "\tBy " + Thread.currentThread().getName() + "\tOn processor: " + Affinity.getCurrentProcessor()
					+ "\trunning at priority "+Thread.currentThread().getPriority());
		}), Affinity.getAvailableProcessors(), DAP, servers);
		
		spry.setMITViolateHandler(new RelativeTime(10000,0), new AsyncEventHandler(){
			@Override
			public void handleAsyncEvent() {
				super.handleAsyncEvent();
				System.out.println("MIT Violation");
			}
		});
		spry.setDeadlineMissHandler(new RelativeTime(1,0), new AsyncEventHandler(){
			@Override
			public void handleAsyncEvent() {
				super.handleAsyncEvent();
				System.out.println("Deadline Miss");
			}
		});
		
		spry.processBatch(data);
		spry.processBatch(data);
	}

}
