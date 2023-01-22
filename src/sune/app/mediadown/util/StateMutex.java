package sune.app.mediadown.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** @since 00.02.04 */
public class StateMutex {
	
	protected final AtomicReference<Throwable> exception = new AtomicReference<>();
	protected final AtomicBoolean unlocked = new AtomicBoolean(false);
	
	protected final boolean await(boolean reset) {
		synchronized(this) {
			boolean success = unlocked.get();
			
			if(!success) {
				try {
					wait();
				} catch(InterruptedException ex) {
					exception.set(ex);
				}
				
				success = exception.get() == null;
			}
			
			if(reset) {
				reset();
			}
			
			return success;
		}
	}
	
	public boolean await() {
		return await(false);
	}
	
	public boolean awaitAndReset() {
		return await(true);
	}
	
	public void unlock() {
		synchronized(this) {
			unlocked.set(true);
			notifyAll();
		}
	}
	
	public void reset() {
		synchronized(this) {
			unlocked.set(false);
		}
	}
	
	public Throwable getException() {
		return exception.get();
	}
	
	public Throwable getExceptionAndReset() {
		return exception.getAndSet(null);
	}
}