package SPRY.Streaming.SPRYStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator.OfDouble;
import java.util.PrimitiveIterator.OfInt;
import java.util.PrimitiveIterator.OfLong;
import java.util.Spliterator;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
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
import java.util.stream.StreamSupport;

import SPRY.DataAllocation.RealtimeSpliterator;
import SPRY.Streaming.SPRYStream.StreamRecord.StreamType;
import SPRY.Streaming.SPRYStream.util.SPRYStreamCallback;

public abstract class SPRYAbstractPipeline<T> {

	protected SPRYAbstractPipeline<T> upPipeline = null;
	protected SPRYAbstractPipeline<T> downPipeline = null;
	protected ArrayList<Object> functions = new ArrayList<>();
	protected OperationType operationType = null;
	protected boolean parallel = true;
	protected Stream<T> delegateStream = null;
	protected boolean isClosed = false;
	/*
	 * resContainer is used to store the result from each return of the terminal
	 * operation
	 */
	protected SPRYStreamCallback<?> callback = null;
	protected CountDownLatch exitBarrier = new CountDownLatch(1);

	protected Collection<T> dataSource = new ArrayList<>();
	
	public void attach(Collection<T> dataSource){
		this.dataSource = dataSource;
	}

	public SPRYAbstractPipeline(boolean parallel) {
		this.parallel = parallel;
	}

	public SPRYAbstractPipeline(SPRYAbstractPipeline<T> upPipeline) {
		this.parallel = upPipeline.parallel;
		this.upPipeline = upPipeline;
		if (upPipeline != null) {
			upPipeline.downPipeline = this;
		}
	}

	protected enum OperationType {
		filter, map, mapToInt, mapToLong, mapToDouble, flatMap, flatMapToInt, flatMapToLong, flatMapToDouble, distinct, sorted, sorted_with_argument, peek, limit, skip, forEach, forEachOrdered, toArray, toArray_with_argument, reduce_with_1_argument, reduce_with_2_arguments, reduce_with_3_arguments, collect, collect_with_collector, min, max, count, anyMatch, allMatch, noneMatch, findFirst, findAny, builder, empty, of, of_with_array, iterate, generate, concat, mapToObj, sum, min_with_1_argument, max_with_1_argument, sequential, parallel, unordered, onClose, spliterator, iterator, average, summaryStatistics, boxed, asDoubleStream, asLongStream
	}

	/**
	 * The Pipeline of operations are double directed linked list, the nodes are
	 * instance of InfiniteReferencePipeline. This method returns the head node.
	 */
	protected SPRYAbstractPipeline<T> getHead() {
		SPRYAbstractPipeline<T> head = this;
		SPRYAbstractPipeline<T> upPipeline = head.upPipeline;
		while (upPipeline != null) {
			head = upPipeline;
			upPipeline = head.upPipeline;
		}
		return head;
	}

	/**
	 * The Pipeline of operations are double directed linked list, the nodes are
	 * instance of InfiniteReferencePipeline. This method returns the tail node.
	 */
	protected SPRYAbstractPipeline<T> geTail() {
		SPRYAbstractPipeline<T> tail = this;
		SPRYAbstractPipeline<T> downPipeline = tail.downPipeline;
		while (downPipeline != null) {
			tail = downPipeline;
			downPipeline = tail.downPipeline;
		}
		return tail;
	}

	/**
	 * update the Operation, and is corresponding type (e.g. map, or reduce) in
	 * this pipeline node.
	 */
	protected void recordCurrentOperation(OperationType currentOperationType, Object... functions) {
		ArrayList<Object> lambdas = new ArrayList<Object>(Arrays.asList(functions));
		this.functions = lambdas;
		this.operationType = currentOperationType;
	}

	/**
	 * Process a stream with the whole pipeline, including all the intermediate
	 * operations and the terminal operation.
	 */
	protected void processWithWholePipeline(Stream<T> targetStream) {
		// System.out.println(System.currentTimeMillis());
		SPRYAbstractPipeline<T> head = getHead();
		SPRYAbstractPipeline<T> tail = geTail();
		StreamRecord finalStream = null;
		if (head != null) {
			if (head != tail) {
				finalStream = processIntermediateOperations(new StreamRecord(StreamType.Stream, targetStream), head.functions, head.operationType);
			} else {
				processTerminalOperations(new StreamRecord(StreamType.Stream, targetStream), head.functions, head.operationType);
			}
		}

		SPRYAbstractPipeline<T> nextPipeline = head.downPipeline;
		while (nextPipeline != null) {
			if (nextPipeline != tail) {
				finalStream = processIntermediateOperations(finalStream, nextPipeline.functions, nextPipeline.operationType);
			} else {
				processTerminalOperations(finalStream, nextPipeline.functions, nextPipeline.operationType);
			}
			nextPipeline = nextPipeline.downPipeline;
		}
	}
	
	protected StreamRecord processWithoutTerminalOps() {
		Stream<T> targetStream;
		if (parallel)
			targetStream = this.dataSource.parallelStream();
		else
			targetStream = this.dataSource.stream();
		SPRYAbstractPipeline<T> head = getHead();
		SPRYAbstractPipeline<T> tail = geTail();
		StreamRecord finalStream = null;
		
		if (head == null) {
			return new StreamRecord(StreamType.Stream, targetStream);
		}
		
		if (head != null) {
			if (head != tail) {
				finalStream = processIntermediateOperations(new StreamRecord(StreamType.Stream, targetStream), head.functions, head.operationType);
			} else {
				return new StreamRecord(StreamType.Stream, targetStream);
			}
		}

		SPRYAbstractPipeline<T> nextPipeline = head.downPipeline;
		while (nextPipeline != null) {
			if (nextPipeline != tail) {
				finalStream = processIntermediateOperations(finalStream, nextPipeline.functions, nextPipeline.operationType);
			} else {
				return finalStream;
			}
			nextPipeline = nextPipeline.downPipeline;
		}
		return finalStream;
	}

	/**
	 * Transform a stream to a new stream, using the intermediate operations.
	 */
	@SuppressWarnings("unchecked")
	protected <X, R, Y> StreamRecord processIntermediateOperations(StreamRecord s, ArrayList<Object> functions, OperationType currentOperationType) {
		switch (currentOperationType) {
		case filter:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).filter((Predicate<? super X>) functions.get(0)));
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).filter((IntPredicate) functions.get(0)));
			if (s.type == StreamType.LongStream)
				return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).filter((LongPredicate) functions.get(0)));
			if (s.type == StreamType.DoubleStream)
				return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).filter((DoublePredicate) functions.get(0)));
		case map:
			if (s.type == StreamType.Stream)
				return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).map((Function<? super X, ? extends R>) functions.get(0)));
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).map((IntUnaryOperator) functions.get(0)));
			if (s.type == StreamType.LongStream)
				return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).map((LongUnaryOperator) functions.get(0)));
			if (s.type == StreamType.DoubleStream)
				return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).map((DoubleUnaryOperator) functions.get(0)));
		case mapToInt:
			if (s.type == StreamType.Stream)
				return new StreamRecord(StreamType.IntStream, ((Stream<X>) s.Stream).mapToInt((ToIntFunction<? super X>) functions.get(0)));
			if (s.type == StreamType.LongStream)
				return new StreamRecord(StreamType.IntStream, ((LongStream) s.Stream).mapToInt((LongToIntFunction) functions.get(0)));
			if (s.type == StreamType.DoubleStream)
				return new StreamRecord(StreamType.IntStream, ((DoubleStream) s.Stream).mapToInt((DoubleToIntFunction) functions.get(0)));
		case mapToLong:
			if (s.type == StreamType.Stream)
				return new StreamRecord(StreamType.LongStream, ((Stream<X>) s.Stream).mapToLong((ToLongFunction<? super X>) functions.get(0)));
			if (s.type == StreamType.IntStream)
				return new StreamRecord(StreamType.LongStream, ((IntStream) s.Stream).mapToLong((IntToLongFunction) functions.get(0)));
			if (s.type == StreamType.DoubleStream)
				return new StreamRecord(StreamType.LongStream, ((DoubleStream) s.Stream).mapToLong((DoubleToLongFunction) functions.get(0)));
		case mapToDouble:
			if (s.type == StreamType.Stream)
				return new StreamRecord(StreamType.DoubleStream, ((Stream<X>) s.Stream).mapToDouble((ToDoubleFunction<? super X>) functions.get(0)));
			if (s.type == StreamType.IntStream)
				return new StreamRecord(StreamType.DoubleStream, ((IntStream) s.Stream).mapToDouble((IntToDoubleFunction) functions.get(0)));
			if (s.type == StreamType.LongStream)
				return new StreamRecord(StreamType.DoubleStream, ((LongStream) s.Stream).mapToDouble((LongToDoubleFunction) functions.get(0)));
		case flatMap:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream,
					((Stream<X>) s.Stream).flatMap((Function<? super X, ? extends Stream<? extends R>>) functions.get(0)));
			if (s.type == StreamType.IntStream)
				return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).flatMap((IntFunction<? extends IntStream>) functions.get(0)));
			if (s.type == StreamType.LongStream)
				return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).flatMap((LongFunction<? extends LongStream>) functions.get(0)));
			if (s.type == StreamType.DoubleStream)
				return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).flatMap((DoubleFunction<? extends DoubleStream>) functions.get(0)));
		case mapToObj:
			if (s.type == StreamType.IntStream)
				return new StreamRecord(StreamType.Stream, ((IntStream) s.Stream).mapToObj((IntFunction<? extends IntStream>) functions.get(0)));
			if (s.type == StreamType.LongStream)
				return new StreamRecord(StreamType.Stream, ((LongStream) s.Stream).mapToObj((LongFunction<? extends LongStream>) functions.get(0)));
			if (s.type == StreamType.DoubleStream)
				return new StreamRecord(StreamType.Stream, ((DoubleStream) s.Stream).mapToObj((DoubleFunction<? extends DoubleStream>) functions.get(0)));
		case flatMapToInt:
			return new StreamRecord(StreamType.IntStream, ((Stream<X>) s.Stream).flatMapToInt((Function<? super X, ? extends IntStream>) functions.get(0)));
		case flatMapToLong:
			return new StreamRecord(StreamType.LongStream, ((Stream<X>) s.Stream).flatMapToInt((Function<? super X, ? extends IntStream>) functions.get(0)));
		case flatMapToDouble:
			return new StreamRecord(StreamType.DoubleStream, ((Stream<X>) s.Stream).flatMapToInt((Function<? super X, ? extends IntStream>) functions.get(0)));
		case distinct:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).distinct());
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).distinct());
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).distinct());
			if (s.type == StreamType.DoubleStream) return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).distinct());
		case sorted_with_argument:
			if (s.type == StreamType.Stream)
				return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).sorted((Comparator<? super X>) functions.get(0)));
		case sorted:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).sorted());
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).sorted());
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).sorted());
			if (s.type == StreamType.DoubleStream) return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).sorted());
		case peek:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).peek((Consumer<? super X>) functions.get(0)));
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).peek((IntConsumer) functions.get(0)));
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).peek((LongConsumer) functions.get(0)));
			if (s.type == StreamType.DoubleStream)
				return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).peek((DoubleConsumer) functions.get(0)));
		case limit:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).limit((long) functions.get(0)));
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).limit((long) functions.get(0)));
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).limit((long) functions.get(0)));
			if (s.type == StreamType.DoubleStream) return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).limit((long) functions.get(0)));
		case skip:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).skip((long) functions.get(0)));
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).skip((long) functions.get(0)));
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).skip((long) functions.get(0)));
			if (s.type == StreamType.DoubleStream) return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).skip((long) functions.get(0)));
		case sequential:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).sequential());
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).sequential());
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).sequential());
			if (s.type == StreamType.DoubleStream) return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).sequential());
		case parallel:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).parallel());
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).parallel());
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).parallel());
			if (s.type == StreamType.DoubleStream) return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).parallel());
		case unordered:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).unordered());
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).unordered());
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).unordered());
			if (s.type == StreamType.DoubleStream) return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).unordered());
		case onClose:
			if (s.type == StreamType.Stream) return new StreamRecord(StreamType.Stream, ((Stream<X>) s.Stream).onClose((Runnable) functions.get(0)));
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).onClose((Runnable) functions.get(0)));
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).onClose((Runnable) functions.get(0)));
			if (s.type == StreamType.DoubleStream)
				return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).onClose((Runnable) functions.get(0)));
		case boxed:
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.IntStream, ((IntStream) s.Stream).boxed());
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.LongStream, ((LongStream) s.Stream).boxed());
			if (s.type == StreamType.DoubleStream) return new StreamRecord(StreamType.DoubleStream, ((DoubleStream) s.Stream).boxed());
		case asDoubleStream:
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.DoubleStream, ((IntStream) s.Stream).asDoubleStream());
			if (s.type == StreamType.LongStream) return new StreamRecord(StreamType.DoubleStream, ((LongStream) s.Stream).asDoubleStream());
		case asLongStream:
			if (s.type == StreamType.IntStream) return new StreamRecord(StreamType.LongStream, ((IntStream) s.Stream).asLongStream());
		default:
			break;
		}
		return null;
	}

	/**
	 * Evaluate a stream with the terminal operation.
	 */
	@SuppressWarnings("unchecked")
	protected <K, A, R> void processTerminalOperations(StreamRecord s, ArrayList<Object> functions, OperationType currentOperationType) {
		try {
			switch (currentOperationType) {
			case forEach:
				if (s.type == StreamType.Stream) ((Stream<K>) s.Stream).forEach((Consumer<? super K>) functions.get(0));
				if (s.type == StreamType.IntStream) ((IntStream) s.Stream).forEach((IntConsumer) functions.get(0));
				if (s.type == StreamType.LongStream) ((LongStream) s.Stream).forEach((LongConsumer) functions.get(0));
				if (s.type == StreamType.DoubleStream) ((DoubleStream) s.Stream).forEach((DoubleConsumer) functions.get(0));
				break;
			case forEachOrdered:
				if (s.type == StreamType.Stream) ((Stream<K>) s.Stream).forEachOrdered((Consumer<? super K>) functions.get(0));
				if (s.type == StreamType.IntStream) ((IntStream) s.Stream).forEachOrdered((IntConsumer) functions.get(0));
				if (s.type == StreamType.LongStream) ((LongStream) s.Stream).forEachOrdered((LongConsumer) functions.get(0));
				if (s.type == StreamType.DoubleStream) ((DoubleStream) s.Stream).forEachOrdered((DoubleConsumer) functions.get(0));
				break;
			case count:
				long resCount = 0;
				if (s.type == StreamType.Stream) resCount = ((Stream<K>) s.Stream).count();
				if (s.type == StreamType.IntStream) resCount = ((IntStream) s.Stream).count();
				if (s.type == StreamType.LongStream) resCount = ((LongStream) s.Stream).count();
				if (s.type == StreamType.DoubleStream) resCount = ((DoubleStream) s.Stream).count();
				if (callback != null) ((SPRYStreamCallback<Long>) callback).update(resCount);
				break;
			case toArray:
				if (s.type == StreamType.Stream) {
					Object[] restoArray = null;
					restoArray = ((Stream<K>) s.Stream).toArray();
					if (callback != null) ((SPRYStreamCallback<Object[]>) callback).update(restoArray);
				}
				if (s.type == StreamType.IntStream) {
					int[] restoArray = null;
					restoArray = ((IntStream) s.Stream).toArray();
					if (callback != null) ((SPRYStreamCallback<int[]>) callback).update(restoArray);
				}
				if (s.type == StreamType.LongStream) {
					long[] restoArray = null;
					restoArray = ((LongStream) s.Stream).toArray();
					if (callback != null) ((SPRYStreamCallback<long[]>) callback).update(restoArray);
				}
				if (s.type == StreamType.DoubleStream) {
					double[] restoArray = null;
					restoArray = ((DoubleStream) s.Stream).toArray();
					if (callback != null) ((SPRYStreamCallback<double[]>) callback).update(restoArray);
				}
				break;
			case toArray_with_argument:
				if (s.type == StreamType.Stream) {
					A[] restoArray = null;
					restoArray = ((Stream<K>) s.Stream).toArray((IntFunction<A[]>) functions.get(0));
					if (callback != null) ((SPRYStreamCallback<A[]>) callback).update(restoArray);
				}
				break;
			case reduce_with_1_argument:
				if (s.type == StreamType.Stream) {
					Optional<K> res = null;
					res = ((Stream<K>) s.Stream).reduce((BinaryOperator<K>) functions.get(0));
					if (callback != null) ((SPRYStreamCallback<Optional<K>>) callback).update(res);
				}
				if (s.type == StreamType.IntStream) {
					OptionalInt res = null;
					res = ((IntStream) s.Stream).reduce((IntBinaryOperator) functions.get(0));
					if (callback != null) ((SPRYStreamCallback<OptionalInt>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					OptionalLong res = null;
					res = ((LongStream) s.Stream).reduce((LongBinaryOperator) functions.get(0));
					if (callback != null) ((SPRYStreamCallback<OptionalLong>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					OptionalDouble res = null;
					res = ((DoubleStream) s.Stream).reduce((DoubleBinaryOperator) functions.get(0));
					if (callback != null) ((SPRYStreamCallback<OptionalDouble>) callback).update(res);
				}
				break;
			case reduce_with_2_arguments:
				if (s.type == StreamType.Stream) {
					K res;
					res = ((Stream<K>) s.Stream).reduce((K) functions.get(0), (BinaryOperator<K>) functions.get(1));
					if (callback != null) ((SPRYStreamCallback<K>) callback).update(res);
				}
				if (s.type == StreamType.IntStream) {
					int res = 0;
					res = ((IntStream) s.Stream).reduce((int) functions.get(0), (IntBinaryOperator) functions.get(1));
					if (callback != null) ((SPRYStreamCallback<Integer>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					long res = 0;
					res = ((LongStream) s.Stream).reduce((long) functions.get(0), (LongBinaryOperator) functions.get(1));
					if (callback != null) ((SPRYStreamCallback<Long>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					double res = 0;
					res = ((DoubleStream) s.Stream).reduce((double) functions.get(0), (DoubleBinaryOperator) functions.get(1));
					if (callback != null) ((SPRYStreamCallback<Double>) callback).update(res);
				}
				break;
			case reduce_with_3_arguments:
				if (s.type == StreamType.Stream) {
					A res;
					res = ((Stream<K>) s.Stream).reduce((A) functions.get(0), (BiFunction<A, ? super K, A>) functions.get(1),
							(BinaryOperator<A>) functions.get(2));
					if (callback != null) ((SPRYStreamCallback<A>) callback).update(res);
				}
				break;
			case collect:
				if (s.type == StreamType.Stream) {
					A res;
					res = ((Stream<K>) s.Stream).collect((Supplier<A>) functions.get(0), (BiConsumer<A, ? super K>) functions.get(1),
							(BiConsumer<A, A>) functions.get(2));
					if (callback != null) ((SPRYStreamCallback<A>) callback).update(res);
				}
				if (s.type == StreamType.IntStream) {
					A res;
					res = ((IntStream) s.Stream).collect((Supplier<A>) functions.get(0), (ObjIntConsumer<A>) functions.get(1),
							(BiConsumer<A, A>) functions.get(2));
					if (callback != null) ((SPRYStreamCallback<A>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					A res;
					res = ((LongStream) s.Stream).collect((Supplier<A>) functions.get(0), (ObjLongConsumer<A>) functions.get(1),
							(BiConsumer<A, A>) functions.get(2));
					if (callback != null) ((SPRYStreamCallback<A>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					A res;
					res = ((DoubleStream) s.Stream).collect((Supplier<A>) functions.get(0), (ObjDoubleConsumer<A>) functions.get(1),
							(BiConsumer<A, A>) functions.get(2));
					if (callback != null) ((SPRYStreamCallback<A>) callback).update(res);
				}
				break;
			case collect_with_collector:
				if (s.type == StreamType.Stream) {
					R res;
					res = ((Stream<K>) s.Stream).collect((Collector<? super K, A, R>) functions.get(0));
					if (callback != null) ((SPRYStreamCallback<R>) callback).update(res);
				}
				break;
			case min_with_1_argument:
				if (s.type == StreamType.Stream) {
					Optional<K> res;
					res = ((Stream<K>) s.Stream).min((Comparator<? super K>) functions.get(0));
					if (callback != null) ((SPRYStreamCallback<Optional<K>>) callback).update(res);
				}
				break;
			case max_with_1_argument:
				if (s.type == StreamType.Stream) {
					Optional<K> res;
					res = ((Stream<K>) s.Stream).max((Comparator<? super K>) functions.get(0));
					if (callback != null) ((SPRYStreamCallback<Optional<K>>) callback).update(res);
				}
				break;
			case min:
				if (s.type == StreamType.IntStream) {
					OptionalInt res;
					res = ((IntStream) s.Stream).min();
					if (callback != null) ((SPRYStreamCallback<OptionalInt>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					OptionalLong res;
					res = ((LongStream) s.Stream).min();
					if (callback != null) ((SPRYStreamCallback<OptionalLong>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					OptionalDouble res;
					res = ((DoubleStream) s.Stream).min();
					if (callback != null) ((SPRYStreamCallback<OptionalDouble>) callback).update(res);
				}
				break;
			case max:
				if (s.type == StreamType.IntStream) {
					OptionalInt res;
					res = ((IntStream) s.Stream).max();
					if (callback != null) ((SPRYStreamCallback<OptionalInt>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					OptionalLong res;
					res = ((LongStream) s.Stream).max();
					if (callback != null) ((SPRYStreamCallback<OptionalLong>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					OptionalDouble res;
					res = ((DoubleStream) s.Stream).max();
					if (callback != null) ((SPRYStreamCallback<OptionalDouble>) callback).update(res);
				}
				break;
			case sum:
				if (s.type == StreamType.IntStream) {
					int res;
					res = ((IntStream) s.Stream).sum();
					if (callback != null) ((SPRYStreamCallback<Integer>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					long res;
					res = ((LongStream) s.Stream).sum();
					if (callback != null) ((SPRYStreamCallback<Long>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					double res;
					res = ((DoubleStream) s.Stream).sum();
					if (callback != null) ((SPRYStreamCallback<Double>) callback).update(res);
				}
				break;
			case anyMatch:
				boolean resAnyMatch = false;
				if (s.type == StreamType.Stream) resAnyMatch = ((Stream<K>) s.Stream).anyMatch((Predicate<? super K>) functions.get(0));
				if (s.type == StreamType.IntStream) resAnyMatch = ((IntStream) s.Stream).anyMatch((IntPredicate) functions.get(0));
				if (s.type == StreamType.LongStream) resAnyMatch = ((LongStream) s.Stream).anyMatch((LongPredicate) functions.get(0));
				if (s.type == StreamType.DoubleStream) resAnyMatch = ((DoubleStream) s.Stream).anyMatch((DoublePredicate) functions.get(0));
				if (callback != null) ((SPRYStreamCallback<Boolean>) callback).update(resAnyMatch);
				break;
			case allMatch:
				boolean resAllMatch = false;
				if (s.type == StreamType.Stream) resAllMatch = ((Stream<K>) s.Stream).allMatch((Predicate<? super K>) functions.get(0));
				if (s.type == StreamType.IntStream) resAllMatch = ((IntStream) s.Stream).allMatch((IntPredicate) functions.get(0));
				if (s.type == StreamType.LongStream) resAllMatch = ((LongStream) s.Stream).allMatch((LongPredicate) functions.get(0));
				if (s.type == StreamType.DoubleStream) resAllMatch = ((DoubleStream) s.Stream).allMatch((DoublePredicate) functions.get(0));
				if (callback != null) ((SPRYStreamCallback<Boolean>) callback).update(resAllMatch);
				break;
			case noneMatch:
				boolean resNoneMatch = false;
				if (s.type == StreamType.Stream) resNoneMatch = ((Stream<K>) s.Stream).noneMatch((Predicate<? super K>) functions.get(0));
				if (s.type == StreamType.IntStream) resNoneMatch = ((IntStream) s.Stream).noneMatch((IntPredicate) functions.get(0));
				if (s.type == StreamType.LongStream) resNoneMatch = ((LongStream) s.Stream).noneMatch((LongPredicate) functions.get(0));
				if (s.type == StreamType.DoubleStream) resNoneMatch = ((DoubleStream) s.Stream).noneMatch((DoublePredicate) functions.get(0));
				if (callback != null) ((SPRYStreamCallback<Boolean>) callback).update(resNoneMatch);
				break;
			case findFirst:
				if (s.type == StreamType.Stream) {
					Optional<K> res;
					res = ((Stream<K>) s.Stream).findFirst();
					if (callback != null) ((SPRYStreamCallback<Optional<K>>) callback).update(res);
				}
				if (s.type == StreamType.IntStream) {
					OptionalInt res;
					res = ((IntStream) s.Stream).findFirst();
					if (callback != null) ((SPRYStreamCallback<OptionalInt>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					OptionalLong res;
					res = ((LongStream) s.Stream).findFirst();
					if (callback != null) ((SPRYStreamCallback<OptionalLong>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					OptionalDouble res;
					res = ((DoubleStream) s.Stream).findFirst();
					if (callback != null) ((SPRYStreamCallback<OptionalDouble>) callback).update(res);
				}
				break;
			case findAny:
				if (s.type == StreamType.Stream) {
					Optional<K> res;
					res = ((Stream<K>) s.Stream).findAny();
					if (callback != null) ((SPRYStreamCallback<Optional<K>>) callback).update(res);
				}
				if (s.type == StreamType.IntStream) {
					OptionalInt res;
					res = ((IntStream) s.Stream).findAny();
					if (callback != null) ((SPRYStreamCallback<OptionalInt>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					OptionalLong res;
					res = ((LongStream) s.Stream).findAny();
					if (callback != null) ((SPRYStreamCallback<OptionalLong>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					OptionalDouble res;
					res = ((DoubleStream) s.Stream).findAny();
					if (callback != null) ((SPRYStreamCallback<OptionalDouble>) callback).update(res);
				}
				break;
			case iterator:
				if (s.type == StreamType.Stream) {
					Iterator<K> res;
					res = ((Stream<K>) s.Stream).iterator();
					if (callback != null) ((SPRYStreamCallback<Iterator<K>>) callback).update(res);
				}
				if (s.type == StreamType.IntStream) {
					OfInt res;
					res = ((IntStream) s.Stream).iterator();
					if (callback != null) ((SPRYStreamCallback<OfInt>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					OfLong res;
					res = ((LongStream) s.Stream).iterator();
					if (callback != null) ((SPRYStreamCallback<OfLong>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					OfDouble res;
					res = ((DoubleStream) s.Stream).iterator();
					if (callback != null) ((SPRYStreamCallback<OfDouble>) callback).update(res);
				}
				break;
			case spliterator:
				if (s.type == StreamType.Stream) {
					Spliterator<K> res;
					res = ((Stream<K>) s.Stream).spliterator();
					if (callback != null) ((SPRYStreamCallback<Spliterator<K>>) callback).update(res);
				}
				if (s.type == StreamType.IntStream) {
					java.util.Spliterator.OfInt res;
					res = ((IntStream) s.Stream).spliterator();
					if (callback != null) ((SPRYStreamCallback<java.util.Spliterator.OfInt>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					java.util.Spliterator.OfLong res;
					res = ((LongStream) s.Stream).spliterator();
					if (callback != null) ((SPRYStreamCallback<java.util.Spliterator.OfLong>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					java.util.Spliterator.OfDouble res;
					res = ((DoubleStream) s.Stream).spliterator();
					if (callback != null) ((SPRYStreamCallback<java.util.Spliterator.OfDouble>) callback).update(res);
				}
				break;
			case average:
				if (s.type == StreamType.IntStream) {
					OptionalDouble res;
					res = ((IntStream) s.Stream).average();
					if (callback != null) ((SPRYStreamCallback<OptionalDouble>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					OptionalDouble res;
					res = ((LongStream) s.Stream).average();
					if (callback != null) ((SPRYStreamCallback<OptionalDouble>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					OptionalDouble res;
					res = ((DoubleStream) s.Stream).average();
					if (callback != null) ((SPRYStreamCallback<OptionalDouble>) callback).update(res);
				}
				break;
			case summaryStatistics:
				if (s.type == StreamType.IntStream) {
					IntSummaryStatistics res;
					res = ((IntStream) s.Stream).summaryStatistics();
					if (callback != null) ((SPRYStreamCallback<IntSummaryStatistics>) callback).update(res);
				}
				if (s.type == StreamType.LongStream) {
					LongSummaryStatistics res;
					res = ((LongStream) s.Stream).summaryStatistics();
					if (callback != null) ((SPRYStreamCallback<LongSummaryStatistics>) callback).update(res);
				}
				if (s.type == StreamType.DoubleStream) {
					DoubleSummaryStatistics res;
					res = ((DoubleStream) s.Stream).summaryStatistics();
					if (callback != null) ((SPRYStreamCallback<DoubleSummaryStatistics>) callback).update(res);
				}
				break;
			default:
				break;
			}
		} catch (Exception e) {
		}
	}

	public void processUsingPredictableStream(Spliterator<T> dataSource) {		
		// System.out.println("processUsingPredictableStream");
		boolean parallel = geTail().parallel;
		RealtimeSpliterator<T> rtSpliterator = new RealtimeSpliterator<>(dataSource);
		if (parallel)
			delegateStream = StreamSupport.stream(rtSpliterator, true);
		else
			delegateStream = StreamSupport.stream(rtSpliterator, false);
		if (!isClosed) {
			processWithWholePipeline(delegateStream);
		}
	}

	/*
	 * The common method in all sub-classes, which implements the processData
	 * method
	 * defined in ReusableStream, or ReusableLongStream interfaces
	 */
	public void processData(Collection<T> data) throws Exception {
		if(!terminalOpGiven) throw new Exception("No terminal operation");
		processUsingPredictableStream(data.spliterator());
	}
	
	public void processData() throws Exception {
		if(!terminalOpGiven) throw new Exception("No terminal operation");
		processUsingPredictableStream(this.dataSource.spliterator());
	}

	public synchronized void processData(Collection<T> data, SPRYStreamCallback<?> resContainer) throws Exception {
		if(!terminalOpGiven) throw new Exception("No terminal operation");
		this.callback = resContainer;
		processUsingPredictableStream(data.spliterator());
	}

	/**
	 * Method that should be invoked when calling the terminal operation
	 */
	boolean terminalOpGiven = false;
	public void onTerminalOpsInvoked() {
		terminalOpGiven = true;
	}

	/**
	 * Prevent the calling thread to exit.
	 */
	public void awaitForTermination() {
		try {
			exitBarrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public SPRYStreamCallback<?> getCallback() {
		return callback;
	}

	public void setCallback(SPRYStreamCallback<?> callback) {
		this.callback = callback;
	}

	/*
	 * *************************************************************************
	 * ****
	 * The following methods
	 * Help subClass to override (i.e. implement) the method defined in the
	 * Stream
	 * or IntStream interfaces
	 *****************************************************************************/
	public boolean isParallel() {
		return parallel;
	}

	/**
	 * close the last stream that processes lastly submitted data,
	 * then this ResuableStream will not process data any more
	 */
	public void close() {
		isClosed = true;
		delegateStream.close();
		exitBarrier.countDown();
	}
}