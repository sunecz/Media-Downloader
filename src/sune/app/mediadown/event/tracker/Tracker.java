package sune.app.mediadown.event.tracker;

import sune.app.mediadown.event.EventBindable;

public interface Tracker extends EventBindable<TrackerEvent> {
	
	/** @since 00.02.08 */
	double progress();
	
	/** @since 00.02.08 */
	default String textProgress() {
		return String.valueOf(progress());
	}
	
	/** @since 00.02.08 */
	default String state() {
		return null;
	}
	
	/** @since 00.02.08 */
	default void visit(TrackerVisitor visitor) {
		visitor.visit(this);
	}
	
	/** @since 00.02.08 */
	default void view(TrackerView view) {
		// Do nothing by default
	}
}