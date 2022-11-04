package sune.app.mediadown.event.tracker;

/** @since 00.02.08 */
public interface TrackerVisitor {
	
	void visit(Tracker tracker);
	
	default void visit(ConversionTracker tracker) { visit(tracker); }
	default void visit(DownloadTracker   tracker) { visit(tracker); }
	default void visit(PlainTextTracker  tracker) { visit(tracker); }
	default void visit(WaitTracker       tracker) { visit(tracker); }
}