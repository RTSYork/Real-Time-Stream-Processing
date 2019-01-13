package SPRY.Streaming.SPRYStream.util;

@FunctionalInterface
public interface SPRYStreamCallback<R> {
	 /**
     * Performs this operation on the given arguments.
     * This method should update the current existing result with 
     * the given new result.
     * @param newResult the new result
     */
    void update(R newResult);
}
