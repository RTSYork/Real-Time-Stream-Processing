package SPRY.Streaming.ReusableStream.util;

import SPRY.Streaming.ReusableStream.ReusableDoublePipeline;

@FunctionalInterface
public interface DoublePipelineInitialiser {
	public void initialise(ReusableDoublePipeline pipeline);
}
