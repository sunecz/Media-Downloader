package sune.app.mediadown.event.tracker;

public class WaitTracker extends SimpleTracker {
	
	@Override
	public void visit(TrackerVisitor visitor) {
		visitor.visit(this);
	}
	
	/** @since 00.02.08 */
	@Override
	public double progress() {
		return Double.NaN;
	}
}