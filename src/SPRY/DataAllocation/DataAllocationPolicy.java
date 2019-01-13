package SPRY.DataAllocation;

public interface DataAllocationPolicy {
	/**
	 * given an item_id return index of accepter
	 * */
	public int get(long item_id);
	
	/**
	 * given an item_id return index of accepter
	 * */
	public int get(long item_id, long Total_NumberOf_items);
}
