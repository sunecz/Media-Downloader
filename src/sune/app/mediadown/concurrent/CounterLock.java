package sune.app.mediadown.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/** @since 00.02.08 */
public final class CounterLock {
	
	private final AtomicInteger counter;
	private final Object lock = new Object();
	private final int minValue;
	
	public CounterLock() {
		this(0, 0);
	}
	
	public CounterLock(int initialValue) {
		this(initialValue, 0);
	}
	
	public CounterLock(int initialValue, int minValue) {
		this.counter = new AtomicInteger(initialValue);
		this.minValue = minValue;
	}
	
	public final void increment() {
		synchronized(lock) {
			counter.getAndIncrement();
		}
	}
	
	public final void decrement() {
		synchronized(lock) {
			if(counter.decrementAndGet() <= minValue) {
				lock.notifyAll();
			}
		}
	}
	
	public final boolean await() {
		while(counter.get() > minValue) {
			synchronized(lock) {
				try {
					lock.wait();
				} catch(InterruptedException ex) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	public final void free() {
		counter.set(0);
		
		synchronized(lock) {
			lock.notifyAll();
		}
	}
	
	public final int count() {
		return counter.get();
	}
}