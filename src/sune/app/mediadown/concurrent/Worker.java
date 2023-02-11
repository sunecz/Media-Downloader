package sune.app.mediadown.concurrent;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.HasTaskState;
import sune.app.mediadown.InternalState;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.08 */
public final class Worker implements HasTaskState {
	
	private static final int STATE_SUBMITTED = 1 << 6;
	
	private final int numThreads;
	private final ExecutorService executor;
	private final Thread thread;
	
	private final Queue<Callable<?>> callables = new ConcurrentLinkedQueue<>();
	private final Queue<WorkerResult<?>> results = new ConcurrentLinkedQueue<>();
	
	private final InternalState state = new InternalState(TaskStates.INITIAL);
	private final CounterLock lock = new CounterLock();
	private final StateMutex lockSubmit = new StateMutex();
	private final StateMutex lockExecute = new StateMutex();
	private final StateMutex lockCount = new StateMutex();
	private final StateMutex lockPause = new StateMutex();
	
	private Worker(int numThreads) {
		if(numThreads <= 0) {
			throw new IllegalArgumentException("Number of threads must be > 0");
		}
		
		this.numThreads = numThreads;
		executor = createExecutorService(numThreads);
		thread = Threads.newThread(this::loop);
		
		Disposables.add(this::stop);
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
	
	private final void loop() {
		// Loop till this worker is not interrupted
		for(Callable<?> r; isRunning() || isPaused();) {
			// Wait if this worker is paused
			if(isPaused()) {
				lockPause.awaitAndReset();
			}
			
			// Wait if there is no more threads
			if(lock.count() >= numThreads) {
				lockCount.awaitAndReset();
			}
			
			// Wait if nothing submitted
			if(callables.isEmpty()) {
				lockSubmit.await();
			}
			
			// Execute the next callable
			if((r = callables.poll()) != null) {
				execute(r);
			}
		}
	}
	
	private final void start() {
		state.clear(TaskStates.STARTED);
		state.set(TaskStates.RUNNING);
		thread.start();
	}
	
	private final void execute(Callable<?> callable) {
		if(isStopped() || isDone()) {
			return;
		}
		
		lock.increment();
		lockCount.reset();
		
		executor.submit(() -> {
			Object result = null;
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
	
	private final boolean isSubmitted() {
		return state.is(STATE_SUBMITTED);
	}
	
	private final void doStop(int stopState) {
		if(isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.RUNNING);
		state.unset(TaskStates.PAUSED);
		
		state.set(stopState);
		
		lockSubmit.unlock();
		lockExecute.unlock();
		lockCount.unlock();
		lockPause.unlock();
		lock.free();
		
		// Force the executor to be shutdown
		Ignore.callVoid(() -> {
			executor.shutdownNow();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		});
	}
	
	public void submit(Runnable runnable) {
		submit(Utils.callable(Utils.checked(runnable)));
	}
	
	public void submit(Runnable runnable, Object result) {
		submit(Utils.callable(Utils.checked(runnable), result));
	}
	
	public void submit(Callable<?> callable) {
		callables.add(callable);
		state.set(STATE_SUBMITTED);
		lockSubmit.unlock();
	}
	
	public void stop() {
		if(!isStarted() || isStopped() || isDone()) {
			return;
		}
		
		doStop(TaskStates.STOPPED);
	}
	
	public void pause() {
		if(!isStarted() || isPaused() || isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.RUNNING);
		state.set(TaskStates.PAUSED);
	}
	
	public void resume() {
		if(!isStarted() || !isPaused() || isStopped() || isDone()) {
			return;
		}
		
		state.set(TaskStates.RUNNING);
		state.unset(TaskStates.PAUSED);
		lockPause.unlock();
	}
	
	public void waitTillDone() {
		// Check whether anything has been submitted, if not, we're already done.
		if(!isSubmitted() || isStopped() || isDone()) {
			return;
		}
		
		try {
			lockExecute.await();
			
			do {
				// Wait till resumed, if paused
				if(isPaused()) {
					lockPause.awaitAndReset();
				}
				
				// Wait for all the running tasks to be done
				lock.await();
			} while(!(isStopped() || isDone()) && !callables.isEmpty());
		} finally {
			doStop(TaskStates.DONE);
		}
	}
	
	public WorkerResult<?> nextResult() {
		return results.poll();
	}
	
	public boolean hasNextResult() {
		return !results.isEmpty();
	}
	
	@Override
	public boolean isRunning() {
		return state.is(TaskStates.RUNNING);
	}
	
	@Override
	public boolean isDone() {
		return state.is(TaskStates.DONE);
	}
	
	@Override
	public boolean isStarted() {
		return state.is(TaskStates.STARTED);
	}
	
	@Override
	public boolean isPaused() {
		return state.is(TaskStates.PAUSED);
	}
	
	@Override
	public boolean isStopped() {
		return state.is(TaskStates.STOPPED);
	}
	
	@Override
	public boolean isError() {
		return state.is(TaskStates.ERROR);
	}
}