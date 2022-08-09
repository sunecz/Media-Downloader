package sune.app.mediadown.util;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.MediaDownloader;

public final class Worker {
	
	private final int numThreads;
	private final ExecutorService executor;
	private final CounterLock lock;
	private final Thread thread;
	
	private final Queue<Callable<?>> callables = new ConcurrentLinkedQueue<>();
	private final Queue<WorkerResult<?>> results = new ConcurrentLinkedQueue<>();
	
	private final SyncObject lockSubmit  = new SyncObject();
	private final SyncObject lockCount   = new SyncObject();
	private final SyncObject lockPause   = new SyncObject();
	private final StateMutex lockExecute = new StateMutex();
	
	private final AtomicBoolean running     = new AtomicBoolean();
	private final AtomicBoolean paused      = new AtomicBoolean();
	private final AtomicBoolean interrupted = new AtomicBoolean();
	private final AtomicBoolean submitted   = new AtomicBoolean();
	
	private Worker(int numThreads) {
		if(numThreads <= 0)
			throw new IllegalArgumentException("Number of threads has to be > 0");
		this.numThreads = numThreads;
		executor  = createExecutorService(numThreads);
		lock      = new CounterLock();
		thread    = Threads.newThread(this::loop);
		Disposables.add(this::interrupt);
	}
	
	private final void start() {
		running.set(true);
		thread.start();
	}
	
	private final void loop() {
		// Loop till this worker is not interrupted
		for(Callable<?> r; running.get() || paused.get();) {
			// Wait if this worker is paused
			if(!interrupted.get() && paused.get()) {
				synchronized(lockPause) {
					if(!interrupted.get() && paused.get()) {
						lockPause.await();
					}
				}
			}
			// Wait if there is no more threads
			if(!interrupted.get() && lock.count() >= numThreads) {
				synchronized(lockCount) {
					if(!interrupted.get() && lock.count() >= numThreads) {
						lockCount.await();
					}
				}
			}
			if(!interrupted.get()) {
				synchronized(lockSubmit) {
					// Wait if nothing submitted
					if(callables.isEmpty()) {
						lockSubmit.await();
					}
					// Execute the next callable
					if((r = callables.peek()) != null) {
						callables.poll();
					}
				}
				if(r != null) {
					execute(r);
				}
			}
		}
	}
	
	private final void execute(Callable<?> callable) {
		lock.increment();
		executor.submit(() -> {
			Object    result    = null;
			Exception exception = null;
			try {
				// Set the result of the call
				result = callable.call();
			} catch(Exception ex) {
				// Otherwise set the exception
				exception = ex;
			} finally {
				results.add(new WorkerResult<>(result, exception));
				lock.decrement();
				lockCount.unlock();
			}
		});
		lockExecute.unlock();
	}
	
	private static final ExecutorService createExecutorService(int numThreads) {
		return numThreads > 0 ? Threads.Pools.newFixed(numThreads) : Threads.Pools.newCached();
	}
	
	public static final Worker createWorker(int numThreads) {
		Worker worker = new Worker(numThreads);
		worker.start(); // Immediately start this worker
		return worker;
	}
	
	public static final Worker createWorker() {
		return createWorker(1);
	}
	
	public static final Worker createThreadedWorker() {
		return createWorker(MediaDownloader.configuration().acceleratedDownload());
	}
	
	public final void submit(Runnable runnable) {
		submit(Utils.callable(Utils.checked(runnable)));
	}
	
	public final void submit(Runnable runnable, Object result) {
		submit(Utils.callable(Utils.checked(runnable), result));
	}
	
	public final void submit(Callable<?> callable) {
		synchronized(lockSubmit) {
			callables.add(callable);
			submitted.set(true);
			lockSubmit.unlock();
		}
	}
	
	public final void pause() {
		running.set(false);
		paused.set(true);
	}
	
	public final void resume() {
		paused.set(false);
		running.set(true);
		lockPause.unlock();
	}
	
	public final void waitTillDone() {
		if(!submitted.get())
			return;
		lockExecute.await();
		do {
			// Wait till resumed if paused
			if(!interrupted.get() && paused.get()) {
				synchronized(lockPause) {
					if(!interrupted.get() && paused.get()) {
						lockPause.await();
					}
				}
			}
			lock.await();
			synchronized(lockSubmit) {
				if(callables.isEmpty())
					break;
			}
		} while(!interrupted.get());
	}
	
	public final void interrupt() {
		if(!running.get())
			return;
		running.set(false);
		paused.set(false);
		interrupted.set(true);
		// Notify all the locks
		lockPause.unlock();
		lockCount.unlock();
		lockSubmit.unlock();
		lockExecute.unlock();
		lock.free();
		executor.shutdownNow();
	}
	
	public final WorkerResult<?> nextResult() {
		return results.poll();
	}
	
	public final boolean hasNextResult() {
		return !results.isEmpty();
	}
	
	public final boolean isRunning() {
		return running.get();
	}
	
	public final boolean isPaused() {
		return paused.get();
	}
	
	public final boolean isInterrupted() {
		return interrupted.get();
	}
}