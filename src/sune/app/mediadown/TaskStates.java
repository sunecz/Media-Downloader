package sune.app.mediadown;

/** @since 00.02.08 */
public final class TaskStates {
	
	public static final int INITIAL = 0;
	public static final int STARTED = 1 << 0;
	public static final int RUNNING = 1 << 1;
	public static final int PAUSED  = 1 << 2;
	public static final int STOPPED = 1 << 3;
	public static final int DONE    = 1 << 4;
	public static final int ERROR   = 1 << 5;
	
	// Forbid anyone to create an instance of this class
	private TaskStates() {
	}
}