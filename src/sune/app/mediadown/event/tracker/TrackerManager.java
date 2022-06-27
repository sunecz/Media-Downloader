package sune.app.mediadown.event.tracker;

public class TrackerManager {
	
	public static interface UpdateListener {
		
		void update();
	}
	
	private UpdateListener listener;
	private Tracker tracker;
	
	public TrackerManager() {
	}
	
	public TrackerManager(UpdateListener listener) {
		setUpdateListener(listener);
	}
	
	public TrackerManager(UpdateListener listener, Tracker tracker) {
		setUpdateListener(listener);
		setTracker(tracker);
	}
	
	public void update() {
		if((listener != null))
			listener.update();
	}
	
	public void setUpdateListener(UpdateListener listener) {
		this.listener = listener;
	}
	
	public void setTracker(Tracker tracker) {
		if((this.tracker != null))
			this.tracker.setTrackerManager(null);
		this.tracker = tracker;
		if((this.tracker != null))
			this.tracker.setTrackerManager(this);
	}
	
	public Tracker getTracker() {
		return tracker;
	}
}