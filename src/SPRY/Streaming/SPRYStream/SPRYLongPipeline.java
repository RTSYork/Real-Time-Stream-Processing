package SPRY.Streaming.SPRYStream;

import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator.OfLong;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.LongStream;
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SPRYLongPipeline extends SPRYAbstractPipeline<Long> implements SPRYLongStream {

	public SPRYLongPipeline(boolean parallel) {
		super(parallel);
	}
	public SPRYLongPipeline(SPRYAbstractPipeline upPipeline) {
		super(upPipeline);
	}

	@Override
	public SPRYLongStream filter(LongPredicate predicate) {
		recordCurrentOperation(OperationType.filter, predicate);
		return new SPRYLongPipeline(this);
	}

	@Override
	public SPRYLongStream map(LongUnaryOperator mapper) {
		recordCurrentOperation(OperationType.map, mapper);
		return new SPRYLongPipeline(this);
	}

	@Override
	public <U> SPRYStream<U> mapToObj(LongFunction<? extends U> mapper) {
		recordCurrentOperation(OperationType.mapToObj, mapper);
		return new SPRYReferencePipeline<U>((SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYIntStream mapToInt(LongToIntFunction mapper) {
		recordCurrentOperation(OperationType.mapToInt, mapper);
		return new SPRYIntPipeline( (SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYDoubleStream mapToDouble(LongToDoubleFunction mapper) {
		recordCurrentOperation(OperationType.mapToDouble, mapper);
		return new SPRYDoublePipeline( (SPRYAbstractPipeline) this);
	}

	@Override
	public SPRYLongStream flatMap(LongFunction<? extends LongStream> mapper) {
		recordCurrentOperation(OperationType.flatMap, mapper);
		return new SPRYLongPipeline(this);
	}

	@Override
	public SPRYLongStream distinct() {
		recordCurrentOperation(OperationType.distinct);
		return new SPRYLongPipeline(this);
	}

	@Override
	public SPRYLongStream sorted() {
		recordCurrentOperation(OperationType.sorted);
		return new SPRYLongPipeline(this);
	}

	@Override
	public SPRYLongStream peek(LongConsumer action) {
		recordCurrentOperation(OperationType.peek, action);
		return new SPRYLongPipeline(this);
	}

	@Override
	public SPRYLongStream limit(long maxSize) {
		recordCurrentOperation(OperationType.limit, maxSize);
		return new SPRYLongPipeline(this);
	}

	@Override
	public SPRYLongStream skip(long n) {
		recordCurrentOperation(OperationType.skip, n);
		return new SPRYLongPipeline(this);
	}

	@Override
	public void forEach(LongConsumer action) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) ((LongStream) sr.Stream).forEach(action);
	}
	
	public void forEachDeferred(LongConsumer action) {
		recordCurrentOperation(OperationType.forEach, action);
		onTerminalOpsInvoked();
	}

	@Override
	public void forEachOrdered(LongConsumer action) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) ((LongStream) sr.Stream).forEachOrdered(action);
	}
	
	public void forEachOrderedDeferred(LongConsumer action) {
		recordCurrentOperation(OperationType.forEachOrdered, action);
		onTerminalOpsInvoked();
	}

	@Override
	public long[] toArray() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).toArray();
		return null;
	}
	
	public void toArrayDeferred() {
		recordCurrentOperation(OperationType.toArray);
		onTerminalOpsInvoked();
	}

	@Override
	public long reduce(long identity, LongBinaryOperator op) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).reduce(identity, op);
		return 0;
	}
	
	public void reduceDeferred(long identity, LongBinaryOperator op) {
		recordCurrentOperation(OperationType.reduce_with_2_arguments, identity, op);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalLong reduce(LongBinaryOperator op) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).reduce(op);
		return null;
	}
	
	public void reduceDeferred(LongBinaryOperator op) {
		recordCurrentOperation(OperationType.reduce_with_1_argument, op);
		onTerminalOpsInvoked();
	}

	@Override
	public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).collect(supplier, accumulator, combiner);
		return null;
	}
	
	public <R> void collectDeferred(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		recordCurrentOperation(OperationType.collect, supplier, accumulator, combiner);
		onTerminalOpsInvoked();
	}

	@Override
	public long sum() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).sum();
		return 0;
	}
	
	public void sumDeferred() {
		recordCurrentOperation(OperationType.sum);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalLong min() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).min();
		return null;
	}
	
	public void minDeferred() {
		recordCurrentOperation(OperationType.min);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalLong max() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).max();
		return null;
	}
	
	public void maxDeferred() {
		recordCurrentOperation(OperationType.max);
		onTerminalOpsInvoked();
	}

	@Override
	public long count() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).count();
		return 0;
	}
	
	public void countDeferred() {
		recordCurrentOperation(OperationType.count);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalDouble average() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).average();
		return null;
	}
	
	public void averageDeferred() {
		recordCurrentOperation(OperationType.average);
		onTerminalOpsInvoked();
	}

	@Override
	public LongSummaryStatistics summaryStatistics() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).summaryStatistics();
		return null;
	}
	
	public void summaryStatisticsDeferred() {
		recordCurrentOperation(OperationType.summaryStatistics);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean anyMatch(LongPredicate predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).anyMatch(predicate);
		return false;
	}
	
	public void anyMatchDeferred(LongPredicate predicate) {
		recordCurrentOperation(OperationType.anyMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean allMatch(LongPredicate predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).allMatch(predicate);
		return false;
	}
	
	public void allMatchDeferred(LongPredicate predicate) {
		recordCurrentOperation(OperationType.allMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public boolean noneMatch(LongPredicate predicate) {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).noneMatch(predicate);
		return false;
	}
	
	public void noneMatchDeferred(LongPredicate predicate) {
		recordCurrentOperation(OperationType.noneMatch, predicate);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalLong findFirst() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).findFirst();
		return null;
	}
	
	public void findFirstDeferred() {
		recordCurrentOperation(OperationType.findFirst);
		onTerminalOpsInvoked();
	}

	@Override
	public OptionalLong findAny() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).findAny();
		return null;
	}
	
	public void findAnyDeferred() {
		recordCurrentOperation(OperationType.findAny);
		onTerminalOpsInvoked();
	}

	@Override
	public SPRYDoubleStream asDoubleStream() {
		recordCurrentOperation(OperationType.asDoubleStream);
		return new SPRYDoublePipeline(this);
	}

	@Override
	public SPRYStream<Long> boxed() {
		recordCurrentOperation(OperationType.boxed);
		return new SPRYReferencePipeline(this);
	}

	@Override
	public SPRYLongStream sequential() {
		recordCurrentOperation(OperationType.sequential);
		return new SPRYLongPipeline(this);
	}

	@Override
	public SPRYLongStream parallel() {
		recordCurrentOperation(OperationType.parallel);
		return new SPRYLongPipeline(this);
	}

	@Override
	public OfLong iterator() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).iterator();
		return null;
	}
	
	public void iteratorDeferred() {
		recordCurrentOperation(OperationType.iterator);
		onTerminalOpsInvoked();
	}

	@Override
	public java.util.Spliterator.OfLong spliterator() {
		StreamRecord sr = processWithoutTerminalOps();
		if (sr != null) return ((LongStream) sr.Stream).spliterator();
		return null;
	}
	
	public void spliteratorDeferred() {
		recordCurrentOperation(OperationType.spliterator);
		onTerminalOpsInvoked();
	}

	@Override
	public SPRYLongStream unordered() {
		recordCurrentOperation(OperationType.unordered);
		return new SPRYLongPipeline(this);
	}

	@Override
	public SPRYLongStream onClose(Runnable closeHandler) {
		recordCurrentOperation(OperationType.onClose, closeHandler);
		return new SPRYLongPipeline(this);
	}
}
