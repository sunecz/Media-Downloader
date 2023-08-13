package sune.app.mediadown.conversion;

import sune.app.mediadown.HasTaskState;
import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.tracker.Trackable;

/** @since 00.02.09 */
public interface ConversionContext extends EventBindable<ConversionEvent>, HasTaskState, Trackable {
	
	ConversionCommand command();
	Exception exception();
}