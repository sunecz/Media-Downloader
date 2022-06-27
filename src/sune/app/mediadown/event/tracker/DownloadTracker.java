package sune.app.mediadown.event.tracker;

import java.util.concurrent.atomic.AtomicLong;

import sune.app.mediadown.util.AtomicDouble;

public class DownloadTracker extends SimpleTracker {
	
	private final AtomicLong total;
	private final AtomicLong current;
	private final AtomicLong prevBytes;
	private final AtomicLong prevTime;
	// bytes per second
	private final AtomicDouble speed;
	private final AtomicDouble secondsLeft;
	// other properties
	private final boolean flag_finalTotal;
	
	public DownloadTracker(long totalBytes) {
		this(0L, totalBytes, true);
	}
	
	public DownloadTracker(long totalBytes, boolean finalTotal) {
		this(0L, totalBytes, finalTotal);
	}
	
	public DownloadTracker(long startBytes, long totalBytes) {
		this(startBytes, totalBytes, true);
	}
	
	public DownloadTracker(long startBytes, long totalBytes, boolean finalTotal) {
		total           = new AtomicLong(totalBytes);
		current         = new AtomicLong(startBytes);
		speed           = new AtomicDouble();
		secondsLeft     = new AtomicDouble();
		prevBytes       = new AtomicLong();
		prevTime        = new AtomicLong(-1L);
		flag_finalTotal = finalTotal;
	}
	
	private final void updateManager() {
		manager.update();
	}
	
	public void update(long bytes) {
		// update the current bytes
		current.getAndAdd(bytes);
		// recalculate the speed
		recalcSpeed();
		// notify the tracker manager
		updateManager();
	}
	
	public void updateTotal(long bytes) {
		if((flag_finalTotal))
			// do not update the total, since it is effectively final
			return;
		// update the total bytes
		total.set(bytes);
		// notify the tracker manager
		updateManager();
	}
	
	public void reset() {
		total      .set(0L);
		current    .set(0L);
		speed      .set(0.0);
		secondsLeft.set(0.0);
		prevBytes  .set(0L);
		prevTime   .set(0L);
	}
	
	private final void recalcSpeed() {
		if((prevTime.get() == -1L)) {
			prevTime .set(System.nanoTime());
			prevBytes.set(current.get());
		} else {
			long deltaTime = System.nanoTime() - prevTime.get();
			if((deltaTime >= 1e9)) {
				long deltaBytes = current.get() - prevBytes.get();
				speed      .set((deltaBytes * 1e9) / deltaTime);
				secondsLeft.set((total.get() - current.get()) / speed.get());
				prevTime   .set(System.nanoTime());
				prevBytes  .set(current.get());
			}
		}
	}
	
	public long getCurrent() {
		return current.get();
	}
	
	public long getTotal() {
		return total.get();
	}
	
	@Override
	public double getProgress() {
		double _current = current.get();
		double _total   = total.get();
		return _total != 0.0 ? _current / _total : 0.0;
	}
	
	public double getTimeLeft() {
		return secondsLeft.get();
	}
	
	public double getSpeed() {
		return speed.get();
	}
}