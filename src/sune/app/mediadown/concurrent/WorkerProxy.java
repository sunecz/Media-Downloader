package sune.app.mediadown.concurrent;

@Deprecated
public interface WorkerProxy {
	
	void pause();
	void resume();
	void cancel();
	
	boolean isRunning();
	boolean isPaused();
	boolean isCanceled();
	
	static WorkerProxy defaultProxy() {
		return new WorkerProxy() {
			
			@Override public void pause()  {}
			@Override public void resume() {}
			@Override public void cancel() {}
			
			@Override public boolean isRunning()  { return true;  }
			@Override public boolean isPaused()   { return false; }
			@Override public boolean isCanceled() { return false; }
		};
	}
}