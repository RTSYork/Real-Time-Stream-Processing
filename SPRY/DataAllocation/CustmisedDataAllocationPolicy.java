package SPRY.DataAllocation;

import java.util.HashMap;

public class CustmisedDataAllocationPolicy implements DataAllocationPolicy {
	private HashMap<Long, Integer> shuffling = new HashMap<>();

	public void addPair(int CPU_ID, long Item_ID) {
		shuffling.put(Item_ID, CPU_ID);
	}

	@Override
	public int get(long item_id) {
		return shuffling.get(item_id);
	}

	@Override
	public int get(long item_id, long Total_NumberOf_items) {
		return shuffling.get(item_id);
	}
}