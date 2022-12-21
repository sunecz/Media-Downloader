package sune.app.mediadown.event.tracker;

/** @since 00.02.08 */
public interface TrackerView {
	
	void progress(double progress);
	void state(String state);
	void current(String current);
	void total(String total);
	void speed(String speed);
	void timeLeft(String timeLeft);
	void information(String information);
	
	double progress();
	String state();
	String current();
	String total();
	String speed();
	String timeLeft();
	String information();
}