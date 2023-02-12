package sune.app.mediadown.concurrent;

/**
 * General object for use when basic synchronization is needed.
 * It synchronizes on itself to reduce the memory footprint.
 * @author Sune
 */
public final class SyncObject {
	
	public final boolean await() {
		boolean success = true;
		
		synchronized(this) {
			try {
				wait();
			} catch(InterruptedException ex) {
				success = false;
			}
		}
		
		return success;
	}
	
	public final void unlock() {
		synchronized(this) {
			notifyAll();
		}
	}
}