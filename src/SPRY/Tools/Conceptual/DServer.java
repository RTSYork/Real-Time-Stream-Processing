package SPRY.Tools.Conceptual;

public class DServer extends PeriodicTask {
	public double server_jitter = 0;
	public DServer(int priority, double period, double WCET) {
		this(priority, period, WCET, period, 0, "Server[" + priority + "]");
	}
	
	public DServer(int priority, double period, double WCET, double deadline) {
		this(priority, period, WCET, deadline, 0, "Server[" + priority + "]");
	}

	public DServer(int priority, double period, double WCET, double deadline, double jitter) {
		this(priority, period, WCET, deadline, 0, "Server[" + priority + "]");
	}

	public DServer(int priority, double period, double WCET, double deadline, double jitter, String name) {
		super(priority, period, WCET, deadline, name);
		this.server_jitter = jitter;
	}
}