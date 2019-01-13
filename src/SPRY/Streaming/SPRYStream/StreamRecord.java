package SPRY.Streaming.SPRYStream;

class StreamRecord {
	public enum StreamType {
		IntStream, Stream, LongStream, DoubleStream
	}

	public Object Stream = null;
	public StreamType type = StreamType.Stream;

	public StreamRecord(StreamType t, Object stream) {
		type = t;
		Stream = stream;
	}
}
