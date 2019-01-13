package SPRY.Streaming.SPRYStream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import SPRY.Streaming.RealTime.LatencyMonitor;
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SPRYReferencePipeline<T> extends SPRYAbstractPipeline<T> implements SPRYStream<T> {
	
	public SPRYReferencePipeline(boolean parallel){
		super(parallel);
	}
	
	protected SPRYReferencePipeline(SPRYAbstractPipeline<T> upPipeline) {
		super(upPipeline);
	}
	
	@Override
	public SPRYStream<T> filter(Predicate<? super T> predicate) {
		Predicate<? super T> Dpredicate = new Predicate<T>(){
			@Override
			public boolean test(T t) {
				if(predicate.test(t)){
					return true;
				}else{
					LatencyMonitor.removeTimer(t);
					return false;
				}
			}
		};
		recordCurrentOperation(OperationType.filter, Dpredicate);
		return new SPRYReferencePipeline<T>(this);
	}

	@Override
	public <R> SPRYStream<R> map(Function<? super T, ? extends R> mapper) {
		recordCurrentOperation(OperationType.map, mapper);
		return new SPRYReferencePipeline<R>((SPRYAbstractPipeline<R>) this);
	}

	@Override
	public SPRYIntStream mapToInt(ToIntFunction<? super T> mapper) {
		recordCurrentOperation(OperationType.mapToInt, mapper);
		return new SPRYIntPipeline( (SPRYAbstractPipeline<Integer>) this);
	}

	@Override
	public SPRYLongStream mapToLong(ToLongFunction<? super T> mapper) {
		recordCurrentOperation(OperationType.mapToLong, mapper);
		return new SPRYLongPipeline( (SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYDoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
		recordCurrentOperation(OperationType.mapToDouble, mapper);
		return new SPRYDoublePipeline( (SPRYAbstractPipeline) this);
	}

	@Override
	public <R> SPRYStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
		recordCurrentOperation(OperationType.flatMap, mapper);
		return new SPRYReferencePipeline<R>((SPRYReferencePipeline<R>) this);
	}

	@Override
	public SPRYIntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
		recordCurrentOperation(OperationType.flatMapToInt, mapper);
		return new SPRYIntPipeline( (SPRYAbstractPipeline<Integer>) this);
	}

	@Override
	public SPRYLongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
		recordCurrentOperation(OperationType.flatMapToLong, mapper);
		return new SPRYLongPipeline( (SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYDoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
		recordCurrentOperation(OperationType.flatMapToDouble, mapper);
		return new SPRYDoublePipeline( (SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYStream<T> distinct() {
		recordCurrentOperation(OperationType.distinct);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYStream<T> sorted() {
		recordCurrentOperation(OperationType.sorted);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYStream<T> sorted(Comparator<? super T> comparator) {
		recordCurrentOperation(OperationType.sorted_with_argument, comparator);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYStream<T> peek(Consumer<? super T> action) {
		recordCurrentOperation(OperationType.peek, action);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYStream<T> limit(long maxSize) {
		recordCurrentOperation(OperationType.limit, maxSize);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYStream<T> skip(long n) {
		recordCurrentOperation(OperationType.skip, n);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) ((Stream<T>) sr.Stream).forEach(action);
	}
	
	public void forEachDeferred(Consumer<? super T> action) {
		Consumer<? super T> newAction = new Consumer() {
			@Override
			public void accept(Object t) {
				action.accept((T) t);
				/* Latency Miss Testing */
				LatencyMonitor.testLatencyMeet(t);
			}
		};
		recordCurrentOperation(OperationType.forEach, newAction);
		onTerminalOpsInvoked();
	}

	@Override
	public void forEachOrdered(Consumer<? super T> action) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) ((Stream<T>) sr.Stream).forEachOrdered(action);
	}
	
	public void forEachOrderedDeferred(Consumer<? super T> action) {
		recordCurrentOperation(OperationType.forEachOrdered, action);
		onTerminalOpsInvoked();
	}

	@Override
	public Object[] toArray() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).toArray();
		return null;
	}
	
	public void toArrayDeferred() {
		recordCurrentOperation(OperationType.toArray);
		onTerminalOpsInvoked();
	}

	@Override
	public <A> A[] toArray(IntFunction<A[]> generator) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).toArray(generator);	
		return null;
	}
	
	public <A> void toArrayDeferred(IntFunction<A[]> generator) {
		recordCurrentOperation(OperationType.toArray_with_argument, generator);
		onTerminalOpsInvoked();
	}

	@Override
	public T reduce(T identity, BinaryOperator<T> accumulator) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).reduce(identity, accumulator);	
		return null;
	}
	
	public void reduceDeferred(T identity, BinaryOperator<T> accumulator) {
		recordCurrentOperation(OperationType.reduce_with_2_arguments, identity, accumulator);
		onTerminalOpsInvoked();
	}

	@Override
	public Optional<T> reduce(BinaryOperator<T> accumulator) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).reduce(accumulator);	
		return null;
	}
	
	public void reduceDeferred(BinaryOperator<T> accumulator) {
		recordCurrentOperation(OperationType.reduce_with_1_argument, accumulator);
		onTerminalOpsInvoked();
	}

	@Override
	public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).reduce(identity, accumulator, combiner);
		return null;
	}
	
	public <U> void reduceDeferred(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
		recordCurrentOperation(OperationType.reduce_with_3_arguments, identity, accumulator, combiner);
		onTerminalOpsInvoked();
	}

	@Override
	public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).collect(supplier, accumulator, combiner);
		return null;
	}
	
	public <R> void collectDeferred(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
		recordCurrentOperation(OperationType.collect, supplier, accumulator, combiner);
		onTerminalOpsInvoked();
	}

	@Override
	public <R, A> R collect(Collector<? super T, A, R> collector) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).collect(collector);
		return null;
	}
	
	public <R, A> void collectDeferred(Collector<? super T, A, R> collector) {
		recordCurrentOperation(OperationType.collect_with_collector, collector);
		onTerminalOpsInvoked();
	}

	@Override
	public Optional<T> min(Comparator<? super T> comparator) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).min(comparator);
		return null;
	}
	
	public void minDeferred(Comparator<? super T> comparator) {
		recordCurrentOperation(OperationType.min_with_1_argument, comparator);
		onTerminalOpsInvoked();
	}

	@Override
	public Optional<T> max(Comparator<? super T> comparator) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).max(comparator);
		return null;
	}
	
	public void maxDeferred(Comparator<? super T> comparator) {
		recordCurrentOperation(OperationType.max_with_1_argument, comparator);
		onTerminalOpsInvoked();
	}

	@Override
	public long count() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).count();
		return 0;
	}
	
	public void countDeferred() {
		recordCurrentOperation(OperationType.count);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean anyMatch(Predicate<? super T> predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).anyMatch(predicate);
		return false;
	}
	
	public void anyMatchDeferred(Predicate<? super T> predicate) {
		recordCurrentOperation(OperationType.anyMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean allMatch(Predicate<? super T> predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).allMatch(predicate);
		return false;
	}
	
	public void allMatchDeferred(Predicate<? super T> predicate) {
		recordCurrentOperation(OperationType.allMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean noneMatch(Predicate<? super T> predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).noneMatch(predicate);
		return false;
	}
	
	public void noneMatchDeferred(Predicate<? super T> predicate) {
		recordCurrentOperation(OperationType.noneMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public Optional<T> findFirst() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).findFirst();
		return null;
	}
	
	public void findFirstDeferred() {
		recordCurrentOperation(OperationType.findFirst);
		onTerminalOpsInvoked();
	}

	@Override
	public Optional<T> findAny() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).findAny();
		return null;
	}
	
	public void findAnyDeferred() {
		recordCurrentOperation(OperationType.findAny);
		onTerminalOpsInvoked();
	}

	@Override
	public Iterator<T> iterator() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).iterator();
		return null;
	}
	
	public void iteratorDeferred() {
		recordCurrentOperation(OperationType.iterator);
		onTerminalOpsInvoked();
	}

	@Override
	public Spliterator<T> spliterator() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((Stream<T>) sr.Stream).spliterator();
		return null;
	}
	
	public Spliterator<T> spliteratorDeferred() {
		recordCurrentOperation(OperationType.spliterator);
		onTerminalOpsInvoked();
		return null;
	}

	@Override
	public SPRYStream<T> sequential() {
		recordCurrentOperation(OperationType.sequential);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYStream<T> parallel() {
		recordCurrentOperation(OperationType.parallel);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYStream<T> unordered() {
		recordCurrentOperation(OperationType.unordered);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYStream<T> onClose(Runnable closeHandler) {
		recordCurrentOperation(OperationType.onClose, closeHandler);
		return new SPRYReferencePipeline(this);
	}
}
