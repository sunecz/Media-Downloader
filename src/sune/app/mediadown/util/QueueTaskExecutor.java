package sune.app.mediadown.util;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import sune.app.mediadown.InternalState;

/** @since 00.02.08 */
public class QueueTaskExecutor<V> {
	
	protected static final int STATE_INITIAL  = 0;
	protected static final int STATE_STARTED  = 1 << 0;
	protected static final int STATE_RUNNING  = 1 << 1;
	protected static final int STATE_STOPPING = 1 << 2;
	protected static final int STATE_STOPPED  = 1 << 3;
	
	protected final int maxTaskCount;
	protected final Queue<InternalQueueTask> submittedTasks = new ConcurrentLinkedQueue<>();
	protected final Queue<InternalQueueTask> queuedTasks = new ConcurrentLinkedQueue<>();
	protected final StateMutex mtxSubmitted = new StateMutex();
	protected final InternalState state = new InternalState(STATE_INITIAL);
	protected final CounterLock lockTasks;
	
	protected Thread thread;
	protected ExecutorService executor;
	
	public QueueTaskExecutor(int maxTaskCount) {
		this.maxTaskCount = checkMaxTaskCount(maxTaskCount);
		this.lockTasks = new CounterLock(0, maxTaskCount - 1);
	}
	
	protected static final int checkMaxTaskCount(int maxTaskCount) {
		if(maxTaskCount <= 0) {
			throw new IllegalArgumentException("Maximum task count must be > 0");
		}
		
		return maxTaskCount;
	}
	
	protected ExecutorService createExecutor() {
		return Threads.Pools.newFixed(maxTaskCount);
	}
	
	protected ExecutorService executor() {
		if(executor == null) {
			synchronized(this) {
				if(executor == null) {
					executor = createExecutor();
				}
			}
		}
		
		return executor;
	}
	
	protected InternalQueueTask createTask(QueueTask<V> task) {
		return new InternalQueueTask(task);
	}
	
	protected Future<V> submitTask(InternalQueueTask task) {
		return executor().submit(task);
	}
	
	protected void cancelTask(InternalQueueTask task) {
		// Do nothing by default
	}
	
	protected void loop() {
		while(true) {
			InternalQueueTask task = null;
			
			while(isRunning()
					&& lockTasks.await() // Wait for available slots
					&& (task = submittedTasks.poll()) == null) {
				mtxSubmitted.awaitAndReset();
			}
			
			if(task == null) {
				break; // Not running, exit the loop
			}
			
			lockTasks.increment();
			Future<V> future = submitTask(task);
			
			task.future(future);
			queuedTasks.add(task);
		}
	}
	
	protected void start() {
		if(thread == null) {
			synchronized(this) {
				if(thread == null) {
					thread = Threads.newThreadUnmanaged(this::loop);
					thread.start();
				}
			}
		}
		
		state.set(STATE_STARTED);
		state.set(STATE_RUNNING);
	}
	
	protected void stop(boolean cancel) throws Exception {
		if(isStopping() || isStopped()) {
			return;
		}
		
		state.set(STATE_STOPPING);
		state.unset(STATE_RUNNING);
		
		submittedTasks.clear();
		lockTasks.free();
		mtxSubmitted.unlock();
		
		try {
			ExecutorService es = null;
			synchronized(this) {
				es = executor;
			}
			
			if(es != null) {
				if(cancel) {
					for(InternalQueueTask task : queuedTasks) {
						task.cancel();
					}
				}
				
				es.shutdown();
				es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			}
		} finally {
			state.set(STATE_STOPPED);
			state.unset(STATE_STOPPING);
		}
	}
	
	public QueueTaskResult<V> submit(QueueTask<V> task) {
		if(!isStarted()) {
			start();
		}
		
		if(!isRunning()) {
			return null;
		}
		
		InternalQueueTask internalTask = createTask(task);
		
		submittedTasks.add(internalTask);
		mtxSubmitted.unlock();
		
		return internalTask;
	}
	
	public void stop() throws Exception {
		stop(true);
	}
	
	public void await() throws Exception {
		stop(false);
	}
	
	public boolean isStarted() {
		return state.is(STATE_STARTED);
	}
	
	public boolean isRunning() {
		return state.is(STATE_RUNNING);
	}
	
	public boolean isStopping() {
		return state.is(STATE_STOPPING);
	}
	
	public boolean isStopped() {
		return state.is(STATE_STOPPED);
	}
	
	protected class InternalQueueTask implements QueueTask<V>, QueueTaskResult<V> {
		
		protected final QueueTask<V> task;
		protected final StateMutex mtxQueued = new StateMutex();
		protected final AtomicBoolean isCancelled = new AtomicBoolean();
		protected Future<V> future;
		protected Exception exception;
		
		public InternalQueueTask(QueueTask<V> task) {
			this.task = Objects.requireNonNull(task);
		}
		
		protected void future(Future<V> future) {
			this.future = future;
			mtxQueued.unlock();
		}
		
		protected V run() throws Exception {
			if(isCancelled()) {
				return null;
			}
			
			return task.call();
		}
		
		protected boolean isCancelled() {
			return isCancelled.get();
		}
		
		protected boolean isQueued() {
			return mtxQueued.isUnlocked();
		}
		
		@Override
		public void cancel() throws Exception {
			if(!isCancelled.compareAndSet(false, true)) {
				return; // Already cancelled
			}
			
			if(future != null) {
				future.cancel(true);
			}
			
			cancelTask(this);
			
			mtxQueued.unlock();
		}
		
		@Override
		public V call() throws Exception {
			try {
				return run();
			} catch(Exception ex) {
				exception = ex;
				throw ex; // Propagate
			} finally {
				lockTasks.decrement();
			}
		}
		
		@Override
		public boolean awaitQueued() {
			mtxQueued.await();
			
			return !isCancelled();
		}
		
		@Override
		public V get() throws Exception {
			return awaitQueued() ? future.get() : null;
		}
		
		@Override
		public Exception exception() {
			return exception;
		}
	}
	
	@FunctionalInterface
	public static interface QueueTask<V> extends Callable<V> {
	}
	
	public static interface QueueTaskResult<V> {
		
		boolean awaitQueued();
		void cancel() throws Exception;
		
		V get() throws Exception;
		Exception exception();
	}
}