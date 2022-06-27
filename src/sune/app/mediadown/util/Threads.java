package sune.app.mediadown.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Threads {
	
	private static final ExecutorService THREADS = newThreadPool();
	private static final ExecutorService newThreadPool() {
		return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
	
	// Forbid anyone to create an instance of this class
	private Threads() {
	}
	
	public static final void execute(Runnable run) {
		if((isRunning())) {
			THREADS.execute(run);
		}
	}
	
	public static final void destroy() {
		THREADS.shutdownNow();
	}
	
	public static final boolean isRunning() {
		return !THREADS.isShutdown();
	}
}