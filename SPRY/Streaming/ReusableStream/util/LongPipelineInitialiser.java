package SPRY.Streaming.ReusableStream.util;

import SPRY.Streaming.ReusableStream.ReusableLongPipeline;

@FunctionalInterface
public interface LongPipelineInitialiser {
	public void initialise(ReusableLongPipeline pipeline);
}
