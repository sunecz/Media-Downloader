package sune.app.mediadown.event.tracker;

/** @since 00.02.08 */
public final class PipelineStates {
	
	public static final String NONE           = null;
	public static final String WAIT           = "tr(md, windows.main.states.wait)";
	public static final String INITIALIZATION = "tr(md, windows.main.states.initialization)";
	public static final String DOWNLOAD       = "tr(md, windows.main.states.download)";
	public static final String CONVERSION     = "tr(md, windows.main.states.conversion)";
	public static final String MERGE          = "tr(md, windows.main.states.merge)";
	public static final String MEDIA_FIX      = "tr(md, windows.main.states.media_fix)";
	public static final String STOPPED        = "tr(md, windows.main.states.stopped)";
	public static final String DONE           = "tr(md, windows.main.states.done)";
	public static final String ERROR          = "tr(md, windows.main.states.error)";
	public static final String PAUSED         = "tr(md, windows.main.states.paused)";
	public static final String RETRY          = "tr(md, windows.main.states.retry)";
	public static final String QUEUED         = "tr(md, windows.main.states.queued)";
	/** @since 00.02.09*/
	public static final String PAUSING        = "tr(md, windows.main.states.synthetic.pausing)";
	/** @since 00.02.09*/
	public static final String RESUMING       = "tr(md, windows.main.states.synthetic.resuming)";
	/** @since 00.02.09*/
	public static final String STOPPING       = "tr(md, windows.main.states.synthetic.stopping)";
	/** @since 00.02.09*/
	public static final String RETRYING       = "tr(md, windows.main.states.synthetic.retrying)";
	
	private PipelineStates() {
	}
}