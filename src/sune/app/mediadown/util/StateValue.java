package sune.app.mediadown.util;

import java.util.concurrent.atomic.AtomicReference;

/** @since 00.02.08 */
public class StateValue<T> extends StateMutex {
	
	private final AtomicReference<T> value = new AtomicReference<>();
	
	@Override
	public void unlock() {
		unlock(null);
	}
	
	public void unlock(T newValue) {
		synchronized(this) {
			value.set(newValue);
			unlocked.set(true);
			notifyAll();
		}
	}
	
	@Override
	public void reset() {
		synchronized(this) {
			value.set(null);
			unlocked.set(false);
		}
	}
	
	public T value() {
		return value.get();
	}
}