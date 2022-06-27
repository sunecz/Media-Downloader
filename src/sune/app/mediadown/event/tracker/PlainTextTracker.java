package sune.app.mediadown.event.tracker;

public class PlainTextTracker extends SimpleTracker {
	
	private double progress;
	private String text;
	
	public void setProgress(double progress) {
		this.progress = progress;
		// notify the tracker manager
		manager.update();
	}
	
	public void setText(String text) {
		this.text = text;
		// notify the tracker manager
		manager.update();
	}
	
	@Override
	public double getProgress() {
		return progress;
	}
	
	public String getText() {
		return text;
	}
}