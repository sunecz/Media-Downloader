package sune.app.mediadown.event.tracker;

public class ConversionTracker extends SimpleTracker {
	
	private double currentTime;
	private double totalTime;
	/** @since 00.02.08 */
	private final boolean isMerge;
	
	public ConversionTracker(double totalTime) {
		this(totalTime, false);
	}
	
	/** @since 00.02.08 */
	public ConversionTracker(double totalTime, boolean isMerge) {
		this.totalTime = totalTime;
		this.isMerge = isMerge;
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
		return isMerge ? PipelineStates.MERGE : PipelineStates.CONVERSION;
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