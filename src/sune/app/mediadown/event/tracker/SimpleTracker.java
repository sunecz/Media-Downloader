package sune.app.mediadown.event.tracker;

public abstract class SimpleTracker implements Tracker {
	
	protected TrackerManager manager;
	
	@Override
	public String getTextProgress() {
		return null;
	}
	
	@Override
	public void setTrackerManager(TrackerManager manager) {
		if((this.manager == null))
			this.manager = manager;
	}
	
	@Override
	public TrackerManager getTrackerManager() {
		return manager;
	}
}