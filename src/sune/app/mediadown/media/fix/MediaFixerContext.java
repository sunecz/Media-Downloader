package sune.app.mediadown.media.fix;

import sune.app.mediadown.HasTaskState;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.MediaFixEvent;
import sune.app.mediadown.event.tracker.Trackable;

/** @since 00.02.09 */
public interface MediaFixerContext extends EventBindable<MediaFixEvent>, HasTaskState, Trackable, MediaFixContext {
	
	Exception exception();
}