package SPRY.Streaming.ReusableStream.util;

import SPRY.Streaming.ReusableStream.ReusableDoublePipeline;
import SPRY.Streaming.ReusableStream.ReusableIntPipeline;
import SPRY.Streaming.ReusableStream.ReusableLongPipeline;
import SPRY.Streaming.ReusableStream.ReusableReferencePipeline;

public class ReusableStreamSupport {
	public static <T> ReusableReferencePipeline<T> Stream(ReferencePipelineInitialiser<T> initialiser) {
		ReusableReferencePipeline<T> pipeline = new ReusableReferencePipeline<>(true);
		initialiser.initialise(pipeline);
		return pipeline;
	}

	public static ReusableIntPipeline IntStream(IntPipelineInitialiser initialiser) {
		ReusableIntPipeline pipeline = new ReusableIntPipeline(true);
		initialiser.initialise(pipeline);
		return pipeline;
	}

	public static ReusableLongPipeline LongStream(LongPipelineInitialiser initialiser) {
		ReusableLongPipeline pipeline = new ReusableLongPipeline(true);
		initialiser.initialise(pipeline);
		return pipeline;
	}

	public static ReusableDoublePipeline DoubleStream(DoublePipelineInitialiser initialiser) {
		ReusableDoublePipeline pipeline = new ReusableDoublePipeline(true);
		initialiser.initialise(pipeline);
		return pipeline;
	}
}