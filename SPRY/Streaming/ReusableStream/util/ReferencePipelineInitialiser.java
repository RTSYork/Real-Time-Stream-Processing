package SPRY.Streaming.ReusableStream.util;

import SPRY.Streaming.ReusableStream.ReusableReferencePipeline;

@FunctionalInterface
public interface ReferencePipelineInitialiser<T> {
	public void initialise(ReusableReferencePipeline<T> pipeline);
}
