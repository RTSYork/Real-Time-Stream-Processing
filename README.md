# Real-Time Stream Processing

## Server Generation
An example of using server generation on a two processors platform, which also hosts two hard real-time tasks, is given below:

    ArrayList<ArrayList<PeriodicTask>> tsOnAllProcessors = new ArrayList<>();
    
		/* hard real-time tasks on processor 0 */
		ArrayList<PeriodicTask> p0 = new ArrayList<>();
		p0.add(new PeriodicTask(11, 20, 10, 20, "t1"));
		tsOnAllProcessors.add(p0);
		/* hard real-time tasks on processor 1 */
		ArrayList<PeriodicTask> p1 = new ArrayList<>();
		p1.add(new PeriodicTask(9, 40, 10, 40, "t2"));
		tsOnAllProcessors.add(p1);

		
		/* the stream processing task, period of 800, deadline of 780*/
		PeriodicTask t_stream = new PeriodicTask(0, 800, 0, 780, "");
		ArrayList<DServer> servers = new ArrayList<>();
		ArrayList<Double> guaranteed_C = new ArrayList<>();
		ServerGenerator sg = new ServerGenerator();
		double prologue = 19, epilogue = 11;
		servers = sg.serverGeneration(t_stream, prologue, epilogue, tsOnAllProcessors, 0, guaranteed_C);
		System.out.println("\nGenerated Servers:");
		servers.forEach(s->System.out.println(s));
The Java code file is located in test/serverGeneration/ folder.
