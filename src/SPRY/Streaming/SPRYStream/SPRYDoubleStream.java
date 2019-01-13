package SPRY.Streaming.SPRYStream;

import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

public interface SPRYDoubleStream extends DoubleStream, SPRYBaseStream<Double> {
	public void forEachDeferred(DoubleConsumer action);
	public void forEachOrderedDeferred(DoubleConsumer action);
	public void toArrayDeferred();
	public void reduceDeferred(double identity, DoubleBinaryOperator op);
	public void reduceDeferred(DoubleBinaryOperator op);
	public <R> void collectDeferred(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner);
	public void sumDeferred();
	public void minDeferred();
	public void maxDeferred();
	public void countDeferred();
	public void averageDeferred();
	public void summaryStatisticsDeferred();
	public void anyMatchDeferred(DoublePredicate predicate);
	public void allMatchDeferred(DoublePredicate predicate);
	public void noneMatchDeferred(DoublePredicate predicate);
	public void findFirstDeferred();
	public void findAnyDeferred();
	public void iteratorDeferred();
	public void spliteratorDeferred();
}
