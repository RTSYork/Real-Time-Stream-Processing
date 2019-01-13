package demo.StreamingTimeAnalysis;

import java.util.ArrayList;

import SPRY.Tools.StreamScheduling;
import SPRY.Tools.Conceptual.PeriodicTask;
/**
 * Demonstrates scheduling a parallel stream processing task that inputs
 * from live streaming data source.
 * The result shows how the deferrable server are generated. How each stage
 * of the stream processing task's execution is analysed.
 * In addition, the latency is also analysed.
 */
public class CaseStudy {

	public static void main(String[] args) {
		ArrayList<ArrayList<PeriodicTask>> tsOnAllProcessors = new ArrayList<>();

		/* Processor 0 */
		ArrayList<PeriodicTask> p0 = new ArrayList<>();
		p0.add(new PeriodicTask(98, 200, 3, 200, "Weapon Release"));
		p0.add(new PeriodicTask(64, 50, 3, 50, "Weapon Aiming"));
		p0.add(new PeriodicTask(56, 59, 8, 59, "Nav Update"));
		tsOnAllProcessors.add(p0);

		/* Processor 1 */
		ArrayList<PeriodicTask> p1 = new ArrayList<>();
		p1.add(new PeriodicTask(84, 25, 2, 25, "Rader Tracking Filter"));
		p1.add(new PeriodicTask(40, 80, 9, 80, "Display Graphic"));
		p1.add(new PeriodicTask(24, 200, 3, 200, "Nav Steering Cmds"));
		tsOnAllProcessors.add(p1);

		/* Processor 2 */
		ArrayList<PeriodicTask> p2 = new ArrayList<>();
		p2.add(new PeriodicTask(72, 25, 5, 25, "RWR Contact Mgmt"));
		p2.add(new PeriodicTask(20, 200, 1, 200, "Display Stores Update"));
		p2.add(new PeriodicTask(12, 200, 3, 200, "Display Stat Update"));
		tsOnAllProcessors.add(p2);

		/* Processor 3 */
		ArrayList<PeriodicTask> p3 = new ArrayList<>();
		p3.add(new PeriodicTask(68, 40, 1, 40, "Data Bus Poll Device"));
		p3.add(new PeriodicTask(60, 50, 5, 50, "Radar Target Update"));
		p3.add(new PeriodicTask(36, 80, 2, 80, "Display Hook Update"));
		p3.add(new PeriodicTask(32, 100, 5, 100, "Tracking Target Update"));
		p3.add(new PeriodicTask(16, 200, 1, 200, "Display Key Set"));
		p3.add(new PeriodicTask(8, 1000, 1, 1000, "BET E Status Update"));
		p3.add(new PeriodicTask(4, 1000, 1, 1000, "Nav Status"));
		tsOnAllProcessors.add(p3);

		int latency = 480;
		int MIT = 25;
		int MaxBatchSize = 17;
		int C_Item = 40;

		PeriodicTask t_stream = new PeriodicTask(0, (MaxBatchSize - 1) * MIT, 0, (MaxBatchSize - 1) * MIT, "");
		int min_period_filter = 1;

		boolean schedulable = StreamScheduling.scheduleStreamingStream(tsOnAllProcessors, t_stream, 9, 1, MaxBatchSize * C_Item, 2, MaxBatchSize,
				min_period_filter, 0, MIT, latency);
		System.out.println("All the data item meets the latency constraint:\t" + schedulable);
		
		
		/* Used to determine the maximum schedulable batch size*/
		// int Max_BatchSize = 2;
		// for (int i = 2; i <= latency / MIT; i++) {
		// MaxBatchSize = i;
		// t_stream = new PeriodicTask(0, (MaxBatchSize-1) * MIT, 0,
		// (MaxBatchSize-1) * MIT, "");
		// if (!StreamScheduling.scheduleStreamingStream(tsOnAllProcessors,
		// t_stream, 9, 1, MaxBatchSize * C_Item, 2, MaxBatchSize,
		// min_period_filter, 0, MIT,
		// MaxBatchSize, latency)) {
		// } else {
		// Max_BatchSize = i;
		// }
		// }
		// System.out.println("Maximum batch size = \t" + Max_BatchSize);
	}
}
