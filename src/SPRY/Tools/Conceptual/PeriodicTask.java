package SPRY.Tools.Conceptual;

import java.util.ArrayList;

public class PeriodicTask {
	public double releaseTime = 0;
	public double WCET = 0;
	public double period = 0;
	public double deadline = 0;
	public int priority = 0;
	private String name = "";
	public double jitter;
	public boolean idle = false;

	public PeriodicTask(PeriodicTask p) {
		this(p.priority, p.period, p.WCET, p.deadline, p.jitter, p.releaseTime, p.name);
	}
	
	public PeriodicTask(int priority, double period, double WCET, double deadline) {
		this(priority, period, WCET, deadline, 0, 0, "PeriodicTask");
	}
	
	public PeriodicTask(int priority, double period, double WCET, double deadline, String name) {
		this(priority, period, WCET, deadline, 0, 0, name);
	}
	
	public PeriodicTask(int priority, double period, double WCET, double deadline, double jitter, String name) {
		this(priority, period, WCET, deadline, jitter, 0, name);
	}

	public PeriodicTask(int priority, double period, double WCET, double deadline, double jitter, double releaseTime, String name) {
		this.priority = priority;
		this.period = period;
		this.deadline = deadline;
		this.jitter = jitter;
		this.WCET = WCET;
		this.releaseTime = releaseTime;
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "T = " +  String.format("%1.0f", this.period) + ", C = " +  String.format("%1.3f", this.WCET) + ", D = " +  String.format("%1.0f", this.deadline) + ", Priority = " + this.priority + "  " + this.name;
	}
	
	public String getName(){
		return name + "[" + priority + "]";
	}
	
	public ArrayList<Double> getReleases(double windowStart, double windowEnd) {
		ArrayList<Double> releases = new ArrayList<>();
		double release = windowStart + period - ((windowStart - releaseTime) % period);
		if (((windowStart - releaseTime) % period) == 0)
			release = windowStart;
		while (release < windowEnd) {
			releases.add(release);
			release += period;
		}
		return releases;
	}
}