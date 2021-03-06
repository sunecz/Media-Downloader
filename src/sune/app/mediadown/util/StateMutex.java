package sune.app.mediadown.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** @since 00.02.04 */
public final class StateMutex {
	
	private AtomicReference<Throwable> exception = new AtomicReference<>();
	private AtomicBoolean unlocked = new AtomicBoolean(false);
	
	private final boolean await(boolean reset) {
		synchronized(this) {
			if(unlocked.get())
				return true;
			try {
				wait();
			} catch(InterruptedException ex) {
				exception.set(ex);
			}
			boolean success = exception.get() == null;
			if(reset) reset();
			return success;
		}
	}
	
	public final boolean await() {
		return await(false);
	}
	
	public final boolean awaitAndReset() {
		return await(true);
	}
	
	public final void unlock() {
		synchronized(this) {
			unlocked.set(true);
			notifyAll();
		}
	}
	
	public final void reset() {
		synchronized(this) {
			unlocked.set(false);
		}
	}
	
	public final Throwable getException() {
		return exception.get();
	}
	
	public final Throwable getExceptionAndReset() {
		return exception.getAndSet(null);
	}
}