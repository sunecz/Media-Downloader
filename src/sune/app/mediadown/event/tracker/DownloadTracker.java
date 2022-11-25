package sune.app.mediadown.event.tracker;

import java.util.concurrent.atomic.AtomicLong;

import sune.app.mediadown.util.AtomicDouble;

public class DownloadTracker extends SimpleTracker {
	
	private final AtomicLong total;
	private final AtomicLong current;
	private final AtomicLong prevBytes;
	private final AtomicLong prevTime;
	private final AtomicDouble speedBPS;
	private final AtomicDouble secondsLeft;
	
	public DownloadTracker() {
		this(0L, -1L);
	}
	
	public DownloadTracker(long totalBytes) {
		this(0L, totalBytes);
	}
	
	public DownloadTracker(long startBytes, long totalBytes) {
		total       = new AtomicLong(totalBytes);
		current     = new AtomicLong(startBytes);
		speedBPS    = new AtomicDouble();
		secondsLeft = new AtomicDouble();
		prevBytes   = new AtomicLong();
		prevTime    = new AtomicLong(-1L);
	}
	
	private final void recalcSpeed() {
		if(prevTime.get() == -1L) {
			prevTime .set(System.nanoTime());
			prevBytes.set(current.get());
		} else {
			long deltaTime = System.nanoTime() - prevTime.get();
			
			if(deltaTime >= 1e9) {
				long deltaBytes = current.get() - prevBytes.get();
				
				speedBPS   .set((deltaBytes * 1e9) / deltaTime);
				secondsLeft.set((total.get() - current.get()) / speedBPS.get());
				prevTime   .set(System.nanoTime());
				prevBytes  .set(current.get());
			}
		}
	}
	
	public void update(long bytes) {
		current.getAndAdd(bytes);
		recalcSpeed();
		update();
	}
	
	public void updateTotal(long bytes) {
		total.set(bytes);
		update();
	}
	
	public void reset() {
		total      .set(0L);
		current    .set(0L);
		speedBPS   .set(0.0);
		secondsLeft.set(0.0);
		prevBytes  .set(0L);
		prevTime   .set(0L);
	}
	
	@Override
	public void visit(TrackerVisitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public String state() {
		return PipelineStates.DOWNLOAD;
	}
	
	/** @since 00.02.08 */
	public long current() {
		return current.get();
	}
	
	/** @since 00.02.08 */
	public long total() {
		return total.get();
	}
	
	/** @since 00.02.08 */
	@Override
	public double progress() {
		double _current = current.get();
		double _total   = total.get();
		return _total != 0.0 ? _current / _total : 0.0;
	}
	
	/** @since 00.02.08 */
	public double secondsLeft() {
		return secondsLeft.get();
	}
	
	/** @since 00.02.08 */
	public double speed() {
		return speedBPS.get();
	}
}