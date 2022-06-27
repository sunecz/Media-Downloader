package sune.app.mediadown.event.tracker;

public interface Tracker {
	
	double getProgress();
	/** @since 00.02.02 */
	String getTextProgress();
	void setTrackerManager(TrackerManager manager);
	TrackerManager getTrackerManager();
}