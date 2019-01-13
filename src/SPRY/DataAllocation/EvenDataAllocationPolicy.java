package SPRY.DataAllocation;

/** Round-robin data allocaiton policy */
public class EvenDataAllocationPolicy implements DataAllocationPolicy {
	private int numberOfWorkers = 1;

	public EvenDataAllocationPolicy(int n) {
		numberOfWorkers = n;
	}

	@Override
	public int get(long item_id) {
		return (int) item_id % numberOfWorkers;
	}

	@Override
	public int get(long item_id, long Total_NumberOf_items) {
		return (int) item_id % numberOfWorkers;
	}
}
