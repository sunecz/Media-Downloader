package sune.app.mediadown.util;

public final class ProcessUtils {
	
	public static final boolean pause(Process process) {
		return PsSuspend.suspend(process.pid());
	}
	
	public static final boolean resume(Process process) {
		return PsSuspend.resume(process.pid());
	}
	
	// Forbid anyone to create an instance of this class
	private ProcessUtils() {
	}
}