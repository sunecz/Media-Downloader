package sune.app.mediadown.util;

import sune.app.mediadown.os.OS;

public final class ProcessUtils {
	
	public static final boolean pause(Process process) {
		return OS.current().suspendProcess(process.pid());
	}
	
	public static final boolean resume(Process process) {
		return OS.current().resumeProcess(process.pid());
	}
	
	// Forbid anyone to create an instance of this class
	private ProcessUtils() {
	}
}