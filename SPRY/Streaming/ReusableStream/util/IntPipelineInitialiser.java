package SPRY.Streaming.ReusableStream.util;

import SPRY.Streaming.ReusableStream.ReusableIntPipeline;

@FunctionalInterface
public interface IntPipelineInitialiser {
	public void initialise(ReusableIntPipeline pipeline);
}
