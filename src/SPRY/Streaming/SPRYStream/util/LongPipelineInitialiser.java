package SPRY.Streaming.SPRYStream.util;

import SPRY.Streaming.SPRYStream.SPRYLongPipeline;

@FunctionalInterface
public interface LongPipelineInitialiser {
	public void initialise(SPRYLongPipeline pipeline);
}
