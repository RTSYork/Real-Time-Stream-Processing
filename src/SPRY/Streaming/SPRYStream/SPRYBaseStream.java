package SPRY.Streaming.SPRYStream;

import java.util.Collection;

import SPRY.Streaming.SPRYStream.util.SPRYStreamCallback;
/**
 * All the methods that are used for extending Java 8 stream to handle streaming data 
 * are declared here
 * */
public interface SPRYBaseStream<T>{
	/**
	 * Process the data with a stream using all the intermediate 
	 * operations and the terminal operation.  */
	public void processData(Collection<T> data)  throws Exception;
	/**
	 * Process the data with a stream using all the intermediate 
	 * operations and the terminal operation.  */
	public void processData(Collection<T> data, SPRYStreamCallback<?> resContainer)  throws Exception;

	
	public void processData() throws Exception;

}
