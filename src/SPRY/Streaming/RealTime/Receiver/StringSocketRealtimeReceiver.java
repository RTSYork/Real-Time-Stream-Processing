package SPRY.Streaming.RealTime.Receiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.util.BitSet;
import java.util.function.Predicate;

import javax.realtime.Affinity;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.RealtimeThread;

import SPRY.ExecutionTimeServer.ProcessingGroup;

public class StringSocketRealtimeReceiver extends RealtimeReceiver<String> {
	
	private String host = "localhost";
	private int port = 1989;
	private Socket socket = null;
	private BufferedReader reader = null;
	private PriorityParameters priority;

	private ProcessingGroup server = null;
	private RealtimeThread receiverThread = null;
	private boolean running = false;
	private BitSet affinity = null;
	
	public StringSocketRealtimeReceiver(String host, int port, int bufferSize) {
		this(host, port, bufferSize, null, null, null);
	}
	
	public StringSocketRealtimeReceiver(String host, int port, int bufferSize, PriorityParameters priority,
			ProcessingGroup server) {
		this(host, port, bufferSize, priority, server, null);
	}
	
	public StringSocketRealtimeReceiver(String host, int port, int bufferSize, PriorityParameters priority,
			ProcessingGroup server, Predicate<? super String> filter) {
		super(bufferSize, filter, null);
		this.host = host;
		this.port = port;
		this.server = server;
		this.priority = priority;
		if(this.priority==null){
			this.priority=new PriorityParameters(PriorityScheduler.instance().getMinPriority());
		}
	}
	
	public StringSocketRealtimeReceiver(String host, int port, int bufferSize, PriorityParameters priority,
			ProcessingGroup server, Predicate<? super String> filter, BitSet affinity) {
		this(host, port, bufferSize, priority, server, filter);
		this.affinity = affinity;
	}

	@Override
	public void start() {
		if(!running){
			running = true;
			onStart();
			// Start a thread, tries to receive input over a socket connection
			if(receiverThread!=null && server!=null){
				server.deRegister(receiverThread);
			}
			receiverThread=new RealtimeThread(priority) {
				@Override
				public void run() {
					receive();
				}
			};
			if(server!=null) server.register(receiverThread);
			receiverThread.setSchedulingParameters(priority);
			if(affinity!=null){
				try {
					Affinity.set(Affinity.generate(affinity), receiverThread);
				} catch (Exception e) {
					System.out.println("Affinity set failed : " +e.getMessage());
				}
			}
			receiverThread.start();
		}
	}

	@Override
	public void stop() {
		if(running){
			running = false;
			onStop();
			try {
				if(socket!=null)	socket.close();
				if(reader!=null)	reader.close();
			} catch (IOException e) {}
			if(receiverThread!=null){
				if(server!=null)	server.deRegister(receiverThread);
			}
		}
	}
	
	/** 
	 * Create a socket connection and receive data until receiver is stopped
	 */
	private void receive() {
		String userInput = null;
		
		try {
			// connects to the server
			socket = new Socket(host, port);	
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			while ((userInput = reader.readLine()) != null) {
				//System.out.println("Receive data '" + userInput + "'");
				store(userInput);
				//store(userInput+"\tArrived at: "+System.currentTimeMillis());
			}
			reader.close();
			socket.close();
			//process the last data after closing
			if(this.bufferUsed>0){
				this.onBufferIsFull();
			}
			//// Restart in an attempt to connect again when server is active again
			//restart();
		} catch (ConnectException ce) {
			// restart if the server is unreachable
			if(running)	restart();
			else System.out.println("close .......");
		} catch (Throwable t) {		}

	}
	
	public void restart(){
		//System.out.println("restart...");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {}
		running=false;
		start();
	}
	
	public PriorityParameters getPriority() {
		return priority;
	}

	@Override
	public void onStart() {}

	@Override
	public void onStop() {}
}