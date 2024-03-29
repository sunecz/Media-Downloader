package sune.app.mediadown.event.tracker;

import javafx.scene.control.ProgressBar;

/** @since 00.02.08 */
public final class PipelineProgress {
	
	public static final double NONE = Double.NEGATIVE_INFINITY;
	public static final double MIN = 0.0;
	public static final double MAX = 1.0;
	public static final double INDETERMINATE = ProgressBar.INDETERMINATE_PROGRESS;
	public static final double PROCESSING = Double.MAX_VALUE;
	public static final double RESET = Double.POSITIVE_INFINITY;
	
	private PipelineProgress() {
	}
}