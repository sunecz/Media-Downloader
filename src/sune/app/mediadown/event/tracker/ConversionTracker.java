package sune.app.mediadown.event.tracker;

public class ConversionTracker extends SimpleTracker {
	
	private double currentTime;
	private double totalTime;
	
	public ConversionTracker(double totalTime) {
		this.totalTime = totalTime;
	}
	
	public void update(double currentTime) {
		this.currentTime = currentTime;
		// notify the tracker manager
		manager.update();
	}
	
	@Override
	public double getProgress() {
		return currentTime / totalTime;
	}
	
	public double getCurrentTime() {
		return currentTime;
	}
	
	public double getTotalTime() {
		return totalTime;
	}
}