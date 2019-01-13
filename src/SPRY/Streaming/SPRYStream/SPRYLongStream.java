package SPRY.Streaming.SPRYStream;

import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.LongStream;

public interface SPRYLongStream extends LongStream, SPRYBaseStream<Long> {
	public void forEachDeferred(LongConsumer action);
	public void forEachOrderedDeferred(LongConsumer action);
	public void toArrayDeferred();
	public void reduceDeferred(long identity, LongBinaryOperator op);
	public void reduceDeferred(LongBinaryOperator op);
	public <R> void collectDeferred(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner);
	public void sumDeferred();
	public void minDeferred();
	public void maxDeferred();
	public void countDeferred();
	public void averageDeferred();
	public void summaryStatisticsDeferred();
	public void anyMatchDeferred(LongPredicate predicate);
	public void allMatchDeferred(LongPredicate predicate);
	public void noneMatchDeferred(LongPredicate predicate);
	public void findFirstDeferred();
	public void findAnyDeferred();
	public void iteratorDeferred();
	public void spliteratorDeferred();
}
