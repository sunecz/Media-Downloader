package sune.app.mediadown.event.tracker;

public class PlainTextTracker extends SimpleTracker {
	
	private double progress;
	private String text;
	
	/** @since 00.02.08 */
	public void progress(double progress) {
		this.progress = progress;
		update();
	}
	
	/** @since 00.02.08 */
	public void text(String text) {
		this.text = text;
		update();
	}
	
	/** @since 00.02.08 */
	@Override
	public double progress() {
		return progress;
	}
	
	/** @since 00.02.08 */
	public String text() {
		return text;
	}
}