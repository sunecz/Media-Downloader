package sune.app.mediadown.initialization;

import sune.app.mediadown.Arguments;

/** @since 00.02.00 */
public interface InitializationState {
	
	public static final double PROGRESS_INDETERMINATE = -1.0;
	
	InitializationState run(Arguments args);
	default String getTitle() {
		return null;
	}
}