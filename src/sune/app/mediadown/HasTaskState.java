package sune.app.mediadown;

/** @since 00.02.08 */
public interface HasTaskState {
	
	boolean isRunning();
	boolean isDone();
	boolean isStarted();
	boolean isPaused();
	boolean isStopped();
	boolean isError();
}