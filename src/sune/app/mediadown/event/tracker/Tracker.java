package sune.app.mediadown.event.tracker;

import sune.app.mediadown.event.EventBindable;

public interface Tracker extends EventBindable<TrackerEvent> {
	
	/** @since 00.02.08 */
	double progress();
	/** @since 00.02.08 */
	String textProgress();
}