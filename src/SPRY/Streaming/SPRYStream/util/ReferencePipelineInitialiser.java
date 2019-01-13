package SPRY.Streaming.SPRYStream.util;

import SPRY.Streaming.SPRYStream.SPRYReferencePipeline;

@FunctionalInterface
public interface ReferencePipelineInitialiser<T> {
	public void initialise(SPRYReferencePipeline<T> pipeline);
}
