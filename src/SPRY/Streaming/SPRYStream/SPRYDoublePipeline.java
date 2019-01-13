package SPRY.Streaming.SPRYStream;

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator.OfDouble;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SPRYDoublePipeline extends SPRYAbstractPipeline<Double> implements SPRYDoubleStream {
	
	public SPRYDoublePipeline(boolean parallel) {
		super(parallel);
	}

	public SPRYDoublePipeline(SPRYAbstractPipeline upPipeline) {
		super(upPipeline);
	}

	@Override
	public SPRYDoubleStream filter(DoublePredicate predicate) {
		recordCurrentOperation(OperationType.filter, predicate);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public SPRYDoubleStream map(DoubleUnaryOperator mapper) {
		recordCurrentOperation(OperationType.map, mapper);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public <U> SPRYStream<U> mapToObj(DoubleFunction<? extends U> mapper) {
		recordCurrentOperation(OperationType.mapToObj, mapper);
		return new SPRYReferencePipeline<U>((SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYIntStream mapToInt(DoubleToIntFunction mapper) {
		recordCurrentOperation(OperationType.mapToInt, mapper);
		return new SPRYIntPipeline( (SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYLongStream mapToLong(DoubleToLongFunction mapper) {
		recordCurrentOperation(OperationType.mapToLong, mapper);
		return new SPRYLongPipeline( (SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYDoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
		recordCurrentOperation(OperationType.flatMap, mapper);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public SPRYDoubleStream distinct() {
		recordCurrentOperation(OperationType.distinct);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public SPRYDoubleStream sorted() {
		recordCurrentOperation(OperationType.sorted);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public SPRYDoubleStream peek(DoubleConsumer action) {
		recordCurrentOperation(OperationType.peek, action);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public SPRYDoubleStream limit(long maxSize) {
		recordCurrentOperation(OperationType.limit, maxSize);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public SPRYDoubleStream skip(long n) {
		recordCurrentOperation(OperationType.skip, n);
		return new SPRYDoublePipeline(this);
	}
	
	@Override
	public void forEach(DoubleConsumer action) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) ((DoubleStream) sr.Stream).forEach(action);
	}
	
	public void forEachDeferred(DoubleConsumer action) {
		recordCurrentOperation(OperationType.forEach, action);
		onTerminalOpsInvoked();
	}

	@Override
	public void forEachOrdered(DoubleConsumer action) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) ((DoubleStream) sr.Stream).forEachOrdered(action);
	}
	
	public void forEachOrderedDeferred(DoubleConsumer action) {
		recordCurrentOperation(OperationType.forEachOrdered, action);
		onTerminalOpsInvoked();
	}

	@Override
	public double[] toArray() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).toArray();
		return null;
	}
	
	public void toArrayDeferred() {
		recordCurrentOperation(OperationType.toArray);
		onTerminalOpsInvoked();
	}

	@Override
	public double reduce(double identity, DoubleBinaryOperator op) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).reduce(identity, op);
		return 0;
	}
	
	public void reduceDeferred(double identity, DoubleBinaryOperator op) {
		recordCurrentOperation(OperationType.reduce_with_2_arguments, identity, op);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalDouble reduce(DoubleBinaryOperator op) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).reduce(op);
		return null;
	}
	
	public void reduceDeferred(DoubleBinaryOperator op) {
		recordCurrentOperation(OperationType.reduce_with_1_argument, op);
		onTerminalOpsInvoked();
	}

	@Override
	public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).collect(supplier, accumulator, combiner);
		return null;
	}
	
	public <R> void collectDeferred(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		recordCurrentOperation(OperationType.collect, supplier, accumulator, combiner);
		onTerminalOpsInvoked();
	}

	@Override
	public double sum() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).sum();
		return 0;
	}
	
	public void sumDeferred() {
		recordCurrentOperation(OperationType.sum);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalDouble min() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).min();
		return null;
	}
	
	public void minDeferred() {
		recordCurrentOperation(OperationType.min);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalDouble max() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).max();
		return null;
	}
	
	public void maxDeferred() {
		recordCurrentOperation(OperationType.max);
		onTerminalOpsInvoked();
	}

	@Override
	public long count() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).count();
		return 0;
	}
	
	public void countDeferred() {
		recordCurrentOperation(OperationType.count);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalDouble average() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).average();
		return null;
	}
	
	public void averageDeferred() {
		recordCurrentOperation(OperationType.average);
		onTerminalOpsInvoked();
	}

	@Override
	public DoubleSummaryStatistics summaryStatistics() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).summaryStatistics();
		return null;
	}	

	public void summaryStatisticsDeferred() {
		recordCurrentOperation(OperationType.summaryStatistics);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean anyMatch(DoublePredicate predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).anyMatch(predicate);
		return false;
	}
	
	public void anyMatchDeferred(DoublePredicate predicate) {
		recordCurrentOperation(OperationType.anyMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean allMatch(DoublePredicate predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).allMatch(predicate);
		return false;
	}
	
	public void allMatchDeferred(DoublePredicate predicate) {
		recordCurrentOperation(OperationType.allMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean noneMatch(DoublePredicate predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).noneMatch(predicate);
		return false;
	}
	
	public void noneMatchDeferred(DoublePredicate predicate) {
		recordCurrentOperation(OperationType.noneMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalDouble findFirst() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).findFirst();
		return null;
	}
	
	public void findFirstDeferred() {
		recordCurrentOperation(OperationType.findFirst);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalDouble findAny() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).findAny();
		return null;
	}
	
	public void findAnyDeferred() {
		recordCurrentOperation(OperationType.findAny);
		onTerminalOpsInvoked();
	}

	@Override
	public SPRYStream<Double> boxed() {
		recordCurrentOperation(OperationType.boxed);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYDoubleStream sequential() {
		recordCurrentOperation(OperationType.sequential);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public SPRYDoubleStream parallel() {
		recordCurrentOperation(OperationType.parallel);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public OfDouble iterator() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).iterator();
		return null;
	}
	
	public void iteratorDeferred() {
		recordCurrentOperation(OperationType.iterator);
		onTerminalOpsInvoked();
	}

	@Override
	public java.util.Spliterator.OfDouble spliterator() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((DoubleStream) sr.Stream).spliterator();
		return null;
	}
	
	public void spliteratorDeferred() {
		recordCurrentOperation(OperationType.spliterator);
		onTerminalOpsInvoked();
	}

	@Override
	public SPRYDoubleStream unordered() {
		recordCurrentOperation(OperationType.unordered);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public SPRYDoubleStream onClose(Runnable closeHandler) {
		recordCurrentOperation(OperationType.onClose, closeHandler);
		return new SPRYDoublePipeline(this);
	}
}
