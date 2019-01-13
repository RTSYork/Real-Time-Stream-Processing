package SPRY.Streaming.SPRYStream;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public interface SPRYStream<T> extends Stream<T>, SPRYBaseStream<T> {
	public void forEachDeferred(Consumer<? super T> action);
	public void forEachOrderedDeferred(Consumer<? super T> action);
	public void toArrayDeferred();
	public <A> void toArrayDeferred(IntFunction<A[]> generator);
	public void reduceDeferred(T identity, BinaryOperator<T> accumulator);
	public void reduceDeferred(BinaryOperator<T> accumulator);
	public <U> void reduceDeferred(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);
	public <R> void collectDeferred(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);
	public <R, A> void collectDeferred(Collector<? super T, A, R> collector);
	public void minDeferred(Comparator<? super T> comparator);
	public void maxDeferred(Comparator<? super T> comparator);
	public void countDeferred();
	public void anyMatchDeferred(Predicate<? super T> predicate);
	public void allMatchDeferred(Predicate<? super T> predicate);
	public void noneMatchDeferred(Predicate<? super T> predicate);
	public void findFirstDeferred();
	public void findAnyDeferred();
	public void iteratorDeferred();
	public Spliterator<T> spliteratorDeferred();	
}
