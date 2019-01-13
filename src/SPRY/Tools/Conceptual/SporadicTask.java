package SPRY.Tools.Conceptual;

public class SporadicTask extends PeriodicTask {

	public SporadicTask(int priority, double MIT, double WCET, double deadline, double jitter, String name) {
		super(priority, MIT, WCET, deadline, jitter, name);
	}

	public SporadicTask(int priority, double MIT, double WCET, double deadline, String name) {
		super(priority, MIT, WCET, deadline, name);
	}

	public SporadicTask(SporadicTask p) {
		super(p);
	}

	public SporadicTask(int priority, double MIT, double WCET, double deadline, double jitter, double releaseTime, String name) {
		super(priority, MIT, WCET, deadline, jitter, releaseTime, name);
	}

}
