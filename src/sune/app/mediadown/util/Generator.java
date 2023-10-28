package sune.app.mediadown.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import sune.app.mediadown.concurrent.StateMutex;

/** @since 00.02.09 */
public abstract class Generator<T> implements Iterator<T>, Iterable<T> {
	
	private static final Object NULL = new Object();
	
	private final StateMutex lockYield = new StateMutex();
	private final StateMutex lockNext = new StateMutex();
	private Thread thread;
	private volatile Object value = NULL;
	
	protected Generator() {
	}
	
	private final void runInternal() {
		run();
		value = NULL;
		lockNext.unlock();
	}
	
	private final boolean ensureInitialized() {
		if(thread == null) {
			thread = new Thread(this::runInternal);
			thread.setDaemon(true);
			thread.start();
			return true;
		}
		
		return false;
	}
	
	protected final void yield(T value) {
		this.value = value;
		lockNext.unlock();
		
		if(!lockYield.awaitAndReset()) {
			throw new UncheckedException(new InterruptedException());
		}
	}
	
	protected abstract void run();
	
	@Override
	public boolean hasNext() {
		if(!ensureInitialized()) {
			lockYield.unlock();
		}
		
		if(!lockNext.awaitAndReset()) {
			throw new UncheckedException(new InterruptedException());
		}
		
		Object val = value;
		return val != NULL;
	}
	
	@Override
	public T next() {
		Object val = value;
		
		if(val == NULL) {
			throw new NoSuchElementException();
		}
		
		@SuppressWarnings("unchecked")
		T casted = (T) val;
		return casted;
	}
	
	@Override
	public Iterator<T> iterator() {
		return this;
	}
	
	public Stream<T> stream() {
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED),
			false
		);
	}
}