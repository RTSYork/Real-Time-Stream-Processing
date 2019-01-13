package SPRY.Streaming.Receiver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
/* Used to receive data from a buffer */
public abstract class Receiver<T> {

	protected List<T> buffer = new ArrayList<T>();
	protected Predicate<? super T> filter = null;

	public abstract void onStart();

	public abstract void start();

	public abstract void onStop();

	public abstract void stop();
	
	public Receiver(){
		this(null);
	}
	
	public Receiver(Predicate<? super T> filter){
		buffer = new ArrayList<T>();
		this.filter = filter;
	}

	public synchronized void store(T t) {
		/* Has to be synchronized because when adding t to the buffer, there is 
		 * another thread that tries to invoke retrieve(), and just about to allocate
		 * a new buffer at that time. The add(t) still operates on the old reference,
		 * it can potentially cause problems, for example, if the old buffer is used,
		 * before add(t) finish. t will be lost for processing.
		 */
		if(filter!=null){
			if(filter.test(t))	buffer.add(t);
		}
		else{
			buffer.add(t);
		}
	}

	/**
	 * retrieve all the input since the last time when retrieve() is invoked
	 */
	public synchronized Collection<T> retrieve() {
		/* Has to be synchronized, because of parallel executions. For example,
		 * when two threads invoke this method in the same time, All of them get
		 * the same reference to the stored input, thus, the data will be
		 * processed two times.
		 */
		List<T> inputs = buffer;
		buffer = new ArrayList<T>();
		return inputs;
	}

	public List<T> getBuffer() {
		return buffer;
	}

	public void setBuffer(List<T> buffer) {
		this.buffer = buffer;
	}
	
	public Predicate<? super T> getFilter() {
		return filter;
	}

	public void setFilter(Predicate<? super T> filter) {
		this.filter = filter;
	}
}