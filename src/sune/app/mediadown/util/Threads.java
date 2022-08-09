package sune.app.mediadown.util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Threads {
	
	private static final ThreadFactory threadFactory = Executors.defaultThreadFactory();
	
	private static final List<Thread> threads = new LinkedList<>();
	private static final List<ExecutorService> pools = new LinkedList<>();
	private static final ExecutorService executor = Pools.newFixed();
	private static final ExecutorService executorEnsured = Pools.newCached();
	
	private static final AtomicBoolean shuttingDown = new AtomicBoolean();
	private static final AtomicBoolean running = new AtomicBoolean(true);
	
	// Forbid anyone to create an instance of this class
	private Threads() {
	}
	
	/** @since 00.02.07 */
	private static final Thread addThread(Thread thread) {
		threads.add(thread);
		return thread;
	}
	
	public static final void execute(Runnable runnable) {
		executor.execute(runnable);
	}
	
	/** @since 00.02.07 */
	public static final void executeEnsured(Runnable runnable) {
		executorEnsured.execute(runnable);
	}
	
	public static final void destroy() {
		// Guard the access to this method
		if(!shuttingDown.compareAndSet(false, true))
			return;
		
		// Only shutdown if running
		if(running.get()) {
			threads.forEach(Thread::interrupt);
			pools.forEach(ExecutorService::shutdownNow);
			
			// Clean up
			threads.clear();
			pools.clear();
		}
		
		running.set(false);
		shuttingDown.set(false);
	}
	
	public static final boolean isRunning() {
		return running.get() && !shuttingDown.get();
	}
	
	/** @since 00.02.07 */
	public static final int maxNumberOfThreads() {
		return Runtime.getRuntime().availableProcessors();
	}
	
	/** @since 00.02.07 */
	public static final Thread newThread(Runnable runnable) {
		return addThread(threadFactory.newThread(runnable));
	}
	
	/** @since 00.02.07 */
	public static final Thread newThreadUnmanaged(Runnable runnable) {
		return threadFactory.newThread(runnable);
	}
	
	/** @since 00.02.07 */
	public static final class Pools {
		
		// Forbid anyone to create an instance of this class
		private Pools() {
		}
		
		private static final ExecutorService addPool(ExecutorService executor) {
			pools.add(executor);
			return executor;
		}
		
		public static final ExecutorService newCached() {
			return addPool(Executors.newCachedThreadPool(threadFactory));
		}
		
		public static final ExecutorService newFixed() {
			return newFixed(maxNumberOfThreads());
		}
		
		public static final ExecutorService newFixed(int numOfThreads) {
			return addPool(Executors.newFixedThreadPool(Math.max(1, numOfThreads), threadFactory));
		}
		
		public static final ExecutorService newWorkStealing() {
			return newWorkStealing(maxNumberOfThreads());
		}
		
		public static final ExecutorService newWorkStealing(int numOfThreads) {
			return addPool(Executors.newWorkStealingPool(Math.max(1, numOfThreads)));
		}
	}
}