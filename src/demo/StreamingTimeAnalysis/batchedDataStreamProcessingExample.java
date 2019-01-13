package demo.StreamingTimeAnalysis;

import java.util.ArrayList;

import SPRY.Tools.StreamScheduling;
import SPRY.Tools.Conceptual.PeriodicTask;

/**
 * Demonstrates scheduling a parallel stream processing task that inputs
 * from a batched data source.
 * The result shows how the deferrable server are generated. How each stage
 * of the stream processing task's execution is analysed.
 */
public class batchedDataStreamProcessingExample {

	public static void main(String[] args) {
		ArrayList<ArrayList<PeriodicTask>> tsOnAllProcessors = new ArrayList<>();

		ArrayList<PeriodicTask> p0 = new ArrayList<>();
		p0.add(new PeriodicTask(11, 20, 10, 20, "t1"));
		tsOnAllProcessors.add(p0);

		ArrayList<PeriodicTask> p1 = new ArrayList<>();
		p1.add(new PeriodicTask(9, 40, 10, 40, "t2"));
		tsOnAllProcessors.add(p1);

		ArrayList<PeriodicTask> p2 = new ArrayList<>();
		p2.add(new PeriodicTask(5, 100, 20, 50, "t3"));
		p2.add(new PeriodicTask(3, 100, 40, 100, "t4"));
		tsOnAllProcessors.add(p2);

		PeriodicTask t_stream = new PeriodicTask(0, 800, 0, 780, "");
		int min_period_filter = 1;

		StreamScheduling.scheduleStaticStream(tsOnAllProcessors, t_stream, 18, 1, 360, 11, 12, min_period_filter, 0);
	}
}