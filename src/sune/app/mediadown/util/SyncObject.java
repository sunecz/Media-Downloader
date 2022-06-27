package sune.app.mediadown.util;

/**
 * General object for use when basic synchronization is needed.
 * It synchronizes on itself to reduce the memory footprint.
 * @author Sune
 */
public final class SyncObject {
	
	private Throwable exception;
	
	public final boolean await() {
		synchronized(this) {
			try {
				wait();
			} catch(InterruptedException ex) {
				exception = ex;
			}
		}
		return exception != null;
	}
	
	public final void unlock() {
		synchronized(this) {
			notifyAll();
		}
	}
	
	public final Throwable getException() {
		return exception;
	}
}