package SPRY.Streaming.SPRYStream;

import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SPRYIntPipeline extends SPRYAbstractPipeline<Integer> implements SPRYIntStream{
	
	public SPRYIntPipeline(boolean parallel) {
		super(parallel);
	}

	public SPRYIntPipeline(SPRYAbstractPipeline upPipeline) {
		super(upPipeline);
	}

	@Override
	public SPRYIntStream filter(IntPredicate predicate) {
		recordCurrentOperation(OperationType.filter, predicate);
		return new SPRYIntPipeline(this);
	}

	@Override
	public SPRYIntStream map(IntUnaryOperator mapper) {
		recordCurrentOperation(OperationType.map, mapper);
		return new SPRYIntPipeline(this);
	}

	@Override
	public <U> SPRYStream<U> mapToObj(IntFunction<? extends U> mapper) {
		recordCurrentOperation(OperationType.mapToObj, mapper);
		return new SPRYReferencePipeline<U>((SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYLongStream mapToLong(IntToLongFunction mapper) {
		recordCurrentOperation(OperationType.mapToLong, mapper);
		return new SPRYLongPipeline( (SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYDoubleStream mapToDouble(IntToDoubleFunction mapper) {
		recordCurrentOperation(OperationType.mapToDouble, mapper);
		return new SPRYDoublePipeline( (SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYIntStream flatMap(IntFunction<? extends IntStream> mapper) {
		recordCurrentOperation(OperationType.flatMap, mapper);
		return new SPRYIntPipeline(this);
	}

	@Override
	public SPRYIntStream distinct() {
		recordCurrentOperation(OperationType.distinct);
		return new SPRYIntPipeline(this); 
	}

	@Override
	public SPRYIntStream sorted() {
		recordCurrentOperation(OperationType.sorted);
		return new SPRYIntPipeline(this);
	}

	@Override
	public SPRYIntStream peek(IntConsumer action) {
		recordCurrentOperation(OperationType.peek, action);
		return new SPRYIntPipeline(this);
	}

	@Override
	public SPRYIntStream limit(long maxSize) {
		recordCurrentOperation(OperationType.limit, maxSize);
		return new SPRYIntPipeline(this);
	}

	@Override
	public SPRYIntStream skip(long n) {
		recordCurrentOperation(OperationType.skip, n);
		return new SPRYIntPipeline(this);
	}

	@Override
	public void forEach(IntConsumer action) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) ((IntStream) sr.Stream).forEach(action);
	}
	
	public void forEachDeferred(IntConsumer action) {
		recordCurrentOperation(OperationType.forEach, action);
		onTerminalOpsInvoked();
	}

	@Override
	public void forEachOrdered(IntConsumer action) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) ((IntStream) sr.Stream).forEachOrdered(action);
	}
	
	public void forEachOrderedDeferred(IntConsumer action) {
		recordCurrentOperation(OperationType.forEachOrdered, action);
		onTerminalOpsInvoked();
	}

	@Override
	public int[] toArray() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).toArray();
		return null;
	}
	
	public void toArrayDeferred() {
		recordCurrentOperation(OperationType.toArray);
		onTerminalOpsInvoked();
	}

	@Override
	public int reduce(int identity, IntBinaryOperator op) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).reduce(identity, op);
		return 0;
	}
	
	public void reduceDeferred(int identity, IntBinaryOperator op) {
		recordCurrentOperation(OperationType.reduce_with_2_arguments,identity,op);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalInt reduce(IntBinaryOperator op) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).reduce(op);
		return null;
	}
	
	public void reduceDeferred(IntBinaryOperator op) {
		recordCurrentOperation(OperationType.reduce_with_1_argument,op);
		onTerminalOpsInvoked();
	}

	@Override
	public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).collect(supplier, accumulator, combiner);
		return null;
	}
	
	public <R> void collectDeferred(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		recordCurrentOperation(OperationType.collect, supplier, accumulator, combiner);
		onTerminalOpsInvoked();
	}

	@Override
	public int sum() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).sum();
		return 0;
	}
	
	public void sumDeferred() {
		recordCurrentOperation(OperationType.sum);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalInt min() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).min();
		return null;
	}
	
	public void minDeferred() {
		recordCurrentOperation(OperationType.min);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalInt max() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).max();
		return null;
	}
	
	public void maxDeferred() {
		recordCurrentOperation(OperationType.max);
		onTerminalOpsInvoked();
	}

	@Override
	public long count() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).count();
		return 0;
	}
	
	public void countDeferred() {
		recordCurrentOperation(OperationType.count);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalDouble average() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).average();
		return null;
	}
	
	public void averageDeferred() {
		recordCurrentOperation(OperationType.average);
		onTerminalOpsInvoked();
	}

	@Override
	public IntSummaryStatistics summaryStatistics() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).summaryStatistics();
		return null;
	}
	
	public void summaryStatisticsDeferred() {
		recordCurrentOperation(OperationType.summaryStatistics);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean anyMatch(IntPredicate predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).anyMatch(predicate);
		return false;
	}
	
	public void anyMatchDeferred(IntPredicate predicate) {
		recordCurrentOperation(OperationType.anyMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean allMatch(IntPredicate predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).allMatch(predicate);
		return false;
	}
	
	public void allMatchDeferred(IntPredicate predicate) {
		recordCurrentOperation(OperationType.allMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean noneMatch(IntPredicate predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).noneMatch(predicate);
		return false;
	}
	
	public void noneMatchDeferred(IntPredicate predicate) {
		recordCurrentOperation(OperationType.noneMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalInt findFirst() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).findFirst();
		return null;
	}
	
	public void findFirstDeferred() {
		recordCurrentOperation(OperationType.findFirst);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalInt findAny() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).findAny();
		return null;
	}
	public void findAnyDeferred() {
		recordCurrentOperation(OperationType.findAny);
		onTerminalOpsInvoked();
	}

	@Override
	public SPRYLongStream asLongStream() {
		recordCurrentOperation(OperationType.asLongStream);
		return new SPRYLongPipeline(this);
	}

	@Override
	public SPRYDoubleStream asDoubleStream() {
		recordCurrentOperation(OperationType.asDoubleStream);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public SPRYStream<Integer> boxed() {
		recordCurrentOperation(OperationType.boxed);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYIntStream sequential() {
		recordCurrentOperation(OperationType.sequential);
		return new SPRYIntPipeline(this);
	}

	@Override
	public SPRYIntStream parallel() {
		recordCurrentOperation(OperationType.parallel);
		return new SPRYIntPipeline(this);
	}

	@Override
	public OfInt iterator() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).iterator();
		return null;
	}
	
	public void iteratorDeferred() {
		recordCurrentOperation(OperationType.iterator);
		onTerminalOpsInvoked();
	}

	@Override
	public java.util.Spliterator.OfInt spliterator() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((IntStream) sr.Stream).spliterator();
		return null;
	}
	
	public void spliteratorDeferred() {
		recordCurrentOperation(OperationType.spliterator);
		onTerminalOpsInvoked();
	}

	@Override
	public SPRYIntStream unordered() {
		recordCurrentOperation(OperationType.unordered);
		return new SPRYIntPipeline(this);
	}

	@Override
	public SPRYIntStream onClose(Runnable closeHandler) {
		recordCurrentOperation(OperationType.onClose, closeHandler);
		return new SPRYIntPipeline(this);
	}
}
