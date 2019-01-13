package SPRY.Streaming.SPRYStream.util;

import SPRY.Streaming.SPRYStream.SPRYIntPipeline;

@FunctionalInterface
public interface IntPipelineInitialiser {
	public void initialise(SPRYIntPipeline pipeline);
}
