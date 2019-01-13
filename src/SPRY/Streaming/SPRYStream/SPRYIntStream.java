package SPRY.Streaming.SPRYStream;

import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public interface SPRYIntStream extends IntStream, SPRYBaseStream<Integer> {
	public void forEachDeferred(IntConsumer action);
	public void forEachOrderedDeferred(IntConsumer action);
	public void toArrayDeferred();
	public void reduceDeferred(int identity, IntBinaryOperator op);
	public void reduceDeferred(IntBinaryOperator op);
	public <R> void collectDeferred(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner);
	public void sumDeferred();
	public void minDeferred();
	public void maxDeferred();
	public void countDeferred();
	public void averageDeferred();
	public void summaryStatisticsDeferred();
	public void anyMatchDeferred(IntPredicate predicate);
	public void allMatchDeferred(IntPredicate predicate);
	public void noneMatchDeferred(IntPredicate predicate);
	public void findFirstDeferred();
	public void findAnyDeferred();
	public void iteratorDeferred();
	public void spliteratorDeferred();
}
