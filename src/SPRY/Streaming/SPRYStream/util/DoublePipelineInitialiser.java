package SPRY.Streaming.SPRYStream.util;

import SPRY.Streaming.SPRYStream.SPRYDoublePipeline;

@FunctionalInterface
public interface DoublePipelineInitialiser {
	public void initialise(SPRYDoublePipeline pipeline);
}
