package SPRY.DataAllocation;

import java.util.ArrayList;
import java.util.Spliterator;
import java.util.function.Consumer;
/** This Spliterator splits in a FIFO order */
public class RealtimeSpliterator<E> implements Spliterator<E> {
	public ArrayList<Spliterator<E>> partitions = new ArrayList<>();
	public int partitonsLeft = 0;
	public static final int defaultGranularity = 1;
	private long granularity = 1;
	private Spliterator<E> source;
	
	public RealtimeSpliterator(Spliterator<E> source) {
		this(source, defaultGranularity);
	}
	
	private RealtimeSpliterator(Spliterator<E> source, long granularity) {
		this(source, granularity, true);
	}
	
	private RealtimeSpliterator(Spliterator<E> source, long granularity, boolean split){
		partitions = new ArrayList<>();
		this.source = source;
		this.granularity = granularity;
		if(!split)	this.partitions.add(source);
		else	partition(source, partitions);
	}
	
	@Override
	public boolean tryAdvance(Consumer<? super E> action) {
		if (action == null) throw new NullPointerException();
		if (partitions.size() >= 1) {
			partitions.remove(0).forEachRemaining(action);
			return true;
		}
		return false;
	}

	@Override
	public Spliterator<E> trySplit() {
		synchronized (partitions) {
			if (partitions.size() > 1){
				return partitions.remove(0);
			}
		}
		return null;
	}

	@Override
	public long estimateSize() {
		return partitions.stream().mapToLong(s -> s.estimateSize()).sum();
	}

	@Override
	public int characteristics() {
		return source.characteristics();
	}
	
	public void partition(Spliterator<E> right, ArrayList<Spliterator<E>> partitions) {
		Spliterator<E> left = right.trySplit();
		if (left == null) {
			partitions.add(right);
			return;
		} else {
			if (left.estimateSize() <= granularity && right.estimateSize() <= granularity) {
				partitions.add(new RealtimeSpliterator<E>(left, granularity, false));
				partitions.add(new RealtimeSpliterator<E>(right, granularity, false));
				return;
			} else {
				partition(left, partitions);
				partition(right, partitions);
			}
		}
		partitonsLeft = this.partitions.size();
	}
	
	public long getGranularity() {
		return granularity;
	}
}