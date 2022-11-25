package sune.app.mediadown.event.tracker;

/** @since 00.02.08 */
public final class PipelineStates {
	
	public static final String NONE = null;
	public static final String WAIT = "wait";
	public static final String INITIALIZATION = "initialization";
	public static final String DOWNLOAD = "download";
	public static final String CONVERSION = "conversion";
	public static final String STOPPED = "stopped";
	public static final String DONE = "done";
	public static final String ERROR = "error";
	public static final String PAUSED = "paused";
	public static final String RETRY = "retry";
	
	private PipelineStates() {
	}
}