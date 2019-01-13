package SPRY.Streaming.SPRYStream.util;

import SPRY.Streaming.SPRYStream.SPRYDoublePipeline;
import SPRY.Streaming.SPRYStream.SPRYIntPipeline;
import SPRY.Streaming.SPRYStream.SPRYLongPipeline;
import SPRY.Streaming.SPRYStream.SPRYReferencePipeline;

public class SPRYStreamSupport {
	public static <T> SPRYReferencePipeline<T> Stream(ReferencePipelineInitialiser<T> initialiser) {
		SPRYReferencePipeline<T> pipeline = new SPRYReferencePipeline<>(true);
		initialiser.initialise(pipeline);
		return pipeline;
	}

	public static SPRYIntPipeline IntStream(IntPipelineInitialiser initialiser) {
		SPRYIntPipeline pipeline = new SPRYIntPipeline(true);
		initialiser.initialise(pipeline);
		return pipeline;
	}

	public static SPRYLongPipeline LongStream(LongPipelineInitialiser initialiser) {
		SPRYLongPipeline pipeline = new SPRYLongPipeline(true);
		initialiser.initialise(pipeline);
		return pipeline;
	}

	public static SPRYDoublePipeline DoubleStream(DoublePipelineInitialiser initialiser) {
		SPRYDoublePipeline pipeline = new SPRYDoublePipeline(true);
		initialiser.initialise(pipeline);
		return pipeline;
	}
}