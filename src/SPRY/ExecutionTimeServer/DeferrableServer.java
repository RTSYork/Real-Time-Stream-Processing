package SPRY.ExecutionTimeServer;

import java.util.ArrayList;
import java.util.HashMap;

import javax.realtime.AsyncEventHandler;
import javax.realtime.PeriodicTimer;
import javax.realtime.PriorityParameters;
import javax.realtime.PriorityScheduler;
import javax.realtime.ProcessingGroupParameters;
import javax.realtime.Schedulable;
/**
 * DeferrableServer allows multiple threads to register to itself,
 * so that all the threads execute certain amount time in total per period.
 * For example, T1, and T2 registered in a DeferrableServer(capacity=10s, period=100s):
 * T2 has executed 10s, T1 cannot execute anymore.
 * */
public class DeferrableServer extends AperiodicServer {
	private PriorityParameters backgroud;
	private PeriodicTimer timer;
	private HashMap<Schedulable, Integer> originalPrioritys = new HashMap<>();
	private PriorityParameters handlerPriority;
	
	private AsyncEventHandler originalOverRunHandler;
	private ProcessingGroupParameters delegationPGP;
	
	public DeferrableServer(ProcessingGroupParameters PGP, PriorityParameters SP, PriorityParameters backgroud) {
		this(PGP, SP, backgroud, new PriorityParameters(PriorityScheduler.instance().getMaxPriority()));
	}

	public DeferrableServer(ProcessingGroupParameters PGP, PriorityParameters SP, PriorityParameters backgroud, PriorityParameters handlerPriority) {
		super(PGP, SP);
		this.backgroud = backgroud;
		this.handlerPriority = handlerPriority;
		if (this.handlerPriority == null) this.handlerPriority = new PriorityParameters(PriorityScheduler.instance().getMaxPriority());
		/* get the original CostOverRunHandler from its ProcessingGroupParameters,
		 * constructs a delegating CostOverRunHandler handler, which will be used to
		 * replace the original CostOverRunHandler with itself */
		this.originalOverRunHandler = PGP.getCostOverrunHandler();
		delegationPGP = new ProcessingGroupParameters(
				PGP.getStart(),
				PGP.getPeriod(),
				PGP.getCost(),
				PGP.getDeadline(),
				PGP.getCostOverrunHandler(),
				PGP.getDeadlineMissHandler());
		delegationPGP.setCostOverrunHandler(new DelegateOverRunHandler(this, originalOverRunHandler));
		/* start the monitoring timer */ 
		TimerHandler tHandler = new TimerHandler(this);
		timer = new PeriodicTimer(delegationPGP.getStart(), delegationPGP.getPeriod(), tHandler);
		timer.start();
	}
	
	public PriorityParameters getBackgroud() {
		return backgroud;
	}
	
	public PriorityParameters getHandlerPriority() {
		return handlerPriority;
	}
	
	/* Override the setProcessingGroupParameter method, so that, when a thread tries to register
	 * to this server, the delegation handler is used */
	@Override
	protected void setProcessingGroupParameter(Schedulable t, ProcessingGroupParameters p){
		/* save original priority */
		if (originalPrioritys.get(t) == null) originalPrioritys.put(t, ((PriorityParameters) t.getSchedulingParameters()).getPriority());
		/* set the new processing group parameters */
		t.setProcessingGroupParameters(delegationPGP);
		t.setSchedulingParameters(this.getPP());
	}
	
	/**
	 * Also remove its original priority from the record
	 * */
	@Override
	public void deRegister(Schedulable... rtThreadsToDeregister) {
		super.deRegister(rtThreadsToDeregister);
		try {
			for (int i = 0; i < rtThreadsToDeregister.length; i++) {
				if (rtThreadsToDeregister[i] != null) {
					int originalPriority = originalPrioritys.get(rtThreadsToDeregister[i]);
					PriorityParameters priorityp = new PriorityParameters(originalPriority);
					rtThreadsToDeregister[i].setSchedulingParameters(priorityp);
					originalPrioritys.remove(rtThreadsToDeregister[i]);
				}
			}
		} catch (Exception e) {}
	}
	
	/**
	 * atReplenish(Schedulable so) will be invoked immediately once the
	 * server's next period arrives. Before any operation being performed by the server. 
	 */
	public void atReplenish(Schedulable so) {	}

	/**
	 * afterExhausted(Schedulable so) will be invoked during server's capacity exhausted,
	 * but after the operation has been performed by the server
	 */
	public void afterExhausted(Schedulable so) {	}
}

/* Handler for Timer */
class TimerHandler extends AsyncEventHandler {
	private DeferrableServer DServer;

	public TimerHandler(DeferrableServer p) {
		super(p.getHandlerPriority(), null, null, null, null, false, null);
		this.DServer = p;
	}

	@Override
	public void handleAsyncEvent() {		
		try {
			ArrayList<Schedulable> threads = DServer.getRegisteredRealtimeThreads();
			for (int i = 0; i < threads.size(); i++) {
				Schedulable targetRTThread = threads.get(i);
				if (targetRTThread != null) {
					DServer.atReplenish(targetRTThread);
					
					int serverPriority = DServer.getPP().getPriority();
					/*
					 * Promote the priority to server's priority if current
					 * priority bigger than the server's one, indicates the
					 * priority is promoted by resource accessing control
					 * protocols, e.g. priority ceiling, thus, skipped
					 */
					if (((PriorityParameters) targetRTThread.getSchedulingParameters()).getPriority() < serverPriority){
						targetRTThread.setSchedulingParameters(DServer.getPP());
						//System.out.println(targetRTThread + "'s priority up to "+DServer.getPP().getPriority());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Timer in Deferrable Server exception: " + e.getMessage());
		}
	}
}

/* delegation Overrun Handler */
class DelegateOverRunHandler extends AsyncEventHandler {
	private DeferrableServer DServer;
	private AsyncEventHandler originalOverRunHandler;

	public DelegateOverRunHandler(DeferrableServer ds, AsyncEventHandler originalOverRunHandler) {
		super(ds.getHandlerPriority(), null, null, null, null, false, null);
		this.DServer = ds;
		this.originalOverRunHandler = originalOverRunHandler;
	}

	@Override
	public void handleAsyncEvent() {
		/* set all threads' priority to background priority */		
		try {
			ArrayList<Schedulable> threads = DServer.getRegisteredRealtimeThreads();
			for (int i = 0; i < threads.size(); i++) {
				Schedulable targetRTThread = threads.get(i);
				if (targetRTThread != null) {
					targetRTThread.setSchedulingParameters(DServer.getBackgroud());
					//System.out.println(targetRTThread + "'s priority lower to "+DServer.getBackgroud().getPriority());
				}
				DServer.afterExhausted(targetRTThread);
				/* perform the original handler's operation */
				if (originalOverRunHandler != null) {
					originalOverRunHandler.handleAsyncEvent();
				}
			}
		} catch (Exception e) {
			System.out.println("Deferrable Server Execption:" + e.getMessage());
		}
	}
}