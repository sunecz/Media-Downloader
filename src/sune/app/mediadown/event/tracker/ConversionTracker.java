package sune.app.mediadown.event.tracker;

public class ConversionTracker extends SimpleTracker {
	
	private double currentTime;
	private double totalTime;
	
	public ConversionTracker(double totalTime) {
		this.totalTime = totalTime;
	}
	
	public void update(double currentTime) {
		this.currentTime = currentTime;
		update();
	}
	
	@Override
	public void visit(TrackerVisitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public String state() {
		return PipelineStates.CONVERSION;
	}
	
	/** @since 00.02.08 */
	@Override
	public double progress() {
		return currentTime / totalTime;
	}
	
	/** @since 00.02.08 */
	public double currentTime() {
		return currentTime;
	}
	
	/** @since 00.02.08 */
	public double totalTime() {
		return totalTime;
	}
}