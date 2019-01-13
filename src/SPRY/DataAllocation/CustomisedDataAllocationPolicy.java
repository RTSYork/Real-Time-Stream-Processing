package SPRY.DataAllocation;

import java.util.HashMap;
/** CustomisedDataAllocationPolicy allows users to define their 
 * customised data allocation policy */
public class CustomisedDataAllocationPolicy implements DataAllocationPolicy {
	private HashMap<Long, Integer> allocationTable = new HashMap<>();

	public void addPair(int CPU_ID, long Item_ID) {
		allocationTable.put(Item_ID, CPU_ID);
	}

	public void addPairs(int CPU_ID, long... Item_IDs) {
		if (Item_IDs != null) for (int i = 0; i < Item_IDs.length; i++)
			allocationTable.put(Item_IDs[i], CPU_ID);
	}

	@Override
	public int get(long item_id) {
		return allocationTable.get(item_id);
	}

	@Override
	public int get(long item_id, long Total_NumberOf_items) {
		return allocationTable.get(item_id);
	}
}