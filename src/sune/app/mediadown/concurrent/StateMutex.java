package sune.app.mediadown.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** @since 00.02.08 */
public final class StateMutex {
	
	private final AtomicReference<Throwable> exception = new AtomicReference<>();
	private final AtomicBoolean unlocked;
	
	public StateMutex() {
		this.unlocked = new AtomicBoolean(false);
	}
	
	/** @since 00.02.10 */
	public StateMutex(boolean unlocked) {
		this.unlocked = new AtomicBoolean(unlocked);
	}
	
	private final boolean await(boolean reset) {
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
	
	/** @since 00.02.10 */
	public void unlockOne() {
		synchronized(this) {
			unlocked.set(true);
			notify();
		}
	}
	
	public void reset() {
		unlocked.set(false);
	}
	
	public Throwable exception() {
		return exception.get();
	}
	
	public Throwable exceptionAndReset() {
		return exception.getAndSet(null);
	}
	
	public boolean isUnlocked() {
		return unlocked.get();
	}
}