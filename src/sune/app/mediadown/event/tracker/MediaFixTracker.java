package sune.app.mediadown.event.tracker;

/** @since 00.02.09 */
public class MediaFixTracker extends SimpleTracker {
	
	private double currentTime;
	private double totalTime;
	private String step;
	private String state;
	
	public MediaFixTracker(double totalTime) {
		this.totalTime = totalTime;
	}
	
	public void update(double currentTime) {
		this.currentTime = currentTime;
		update();
	}
	
	public void updateStep(String step) {
		String state = "tr(md, windows.main.states.media_fix_step." + step.toLowerCase() + ")";
		updateStep(step, state);
	}
	
	public void updateStep(String step, String state) {
		this.step = step;
		this.state = state;
		update();
	}
	
	public void updateState(String state) {
		this.step = null;
		this.state = state;
		update();
	}
	
	@Override
	public void visit(TrackerVisitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public String state() {
		return state;
	}
	
	@Override
	public double progress() {
		return currentTime / totalTime;
	}
	
	public double currentTime() {
		return currentTime;
	}
	
	public double totalTime() {
		return totalTime;
	}
	
	public String step() {
		return step;
	}
}