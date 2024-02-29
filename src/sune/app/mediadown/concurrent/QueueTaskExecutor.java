package sune.app.mediadown.concurrent;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
	protected final Queue<InternalQueueTask> runningTasks = new ConcurrentLinkedQueue<>();
	protected final StateMutex mtxSubmitted = new StateMutex();
	protected final InternalState state = new InternalState(STATE_INITIAL);
	protected final CounterLock lockTasks;
	
	protected volatile Thread thread;
	protected volatile ExecutorService executor;
	
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
		ExecutorService es;
		if((es = executor) == null) {
			synchronized(this) {
				if((es = executor) == null) {
					es = createExecutor();
					executor = es;
				}
			}
		}
		
		return es;
	}
	
	protected InternalQueueTask createTask(QueueTask<V> task) {
		return new InternalQueueTask(task);
	}
	
	protected Future<V> submitTask(InternalQueueTask task) {
		return executor().submit(task);
	}
	
	/** @since 00.02.09 */
	protected void removeLeadingFinishedTasks() {
		// To avoid search in the queue to remove a task that may be at any position,
		// remove all leading done or cancelled tasks from the queue at once.
		for(InternalQueueTask task;
				!state.is(STATE_STOPPING)
					&& (task = runningTasks.peek()) != null
					&& (task.isDone() || task.isCancelled());
				// Use remove to ensure that the wanted task is removed, not another task
				runningTasks.remove(task));
	}
	
	protected void cancelTask(InternalQueueTask task) {
		removeLeadingFinishedTasks();
	}
	
	/** @since 00.02.09 */
	protected void pauseSubmittedTask(InternalQueueTask task) {
		// Nothing to do
	}
	
	/** @since 00.02.09 */
	protected void pauseDelayedTask(InternalQueueTask task) {
		// Nothing to do
	}
	
	/** @since 00.02.09 */
	protected void resumeSubmittedTask(InternalQueueTask task) {
		// Nothing to do
	}
	
	/** @since 00.02.09 */
	protected void resumeDelayedTask(InternalQueueTask task) {
		submittedTasks.add(task);
		mtxSubmitted.unlock();
	}
	
	/** @since 00.02.09 */
	protected void finishTask(InternalQueueTask task) {
		removeLeadingFinishedTasks();
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
			runningTasks.add(task);
		}
	}
	
	protected void start() {
		Thread t;
		if((t = thread) == null) {
			synchronized(this) {
				if((t = thread) == null) {
					t = Threads.newThreadUnmanaged(this::loop);
					t.start();
					thread = t;
				}
			}
		}
		
		state.set(STATE_STARTED | STATE_RUNNING);
	}
	
	protected void stop(boolean cancel) throws Exception {
		if(isStopping() || isStopped()) {
			return;
		}
		
		// TODO: Make atomic
		state.set(STATE_STOPPING);
		state.unset(STATE_RUNNING);
		
		submittedTasks.clear();
		lockTasks.free();
		mtxSubmitted.unlock();
		
		try {
			ExecutorService es;
			if((es = executor) == null) {
				synchronized(this) {
					if((es = executor) == null) {
						return;
					}
				}
			}
			
			if(cancel) {
				for(InternalQueueTask task : runningTasks) {
					task.cancel();
				}
				
				runningTasks.clear();
			}
			
			es.shutdown();
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} finally {
			// TODO: Make atomic
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
		
		/** @since 00.02.09 */
		protected static final int TASK_STATE_INITIAL   = 0;
		/** @since 00.02.09 */
		protected static final int TASK_STATE_CALLED    = 1 << 1;
		/** @since 00.02.09 */
		protected static final int TASK_STATE_PAUSED    = 1 << 2;
		/** @since 00.02.09 */
		protected static final int TASK_STATE_DONE      = 1 << 3;
		/** @since 00.02.09 */
		protected static final int TASK_STATE_CANCELLED = 1 << 4;
		/** @since 00.02.09 */
		protected static final int TASK_STATE_DELAYED   = 1 << 5;
		
		protected final QueueTask<V> task;
		/** @since 00.02.09 */
		protected final StateMutex mtxCalled = new StateMutex();
		/** @since 00.02.09 */
		protected final InternalState state = new InternalState(TASK_STATE_INITIAL);
		protected volatile Future<V> future;
		protected Exception exception;
		
		public InternalQueueTask(QueueTask<V> task) {
			this.task = Objects.requireNonNull(task);
		}
		
		protected void future(Future<V> future) {
			synchronized(this) {
				this.future = future;
			}
		}
		
		protected V run() throws Exception {
			if(!state.compareAndSetBit(false, TASK_STATE_CALLED)) {
				return null;
			}
			
			// The task should be run but is still paused, actually pause it now.
			if(isPaused()) {
				state.set(TASK_STATE_DELAYED);
				pauseDelayedTask(this);
				return null; // Do not run the task itself
			}
			
			try {
				return task.call();
			} finally {
				mtxCalled.unlock();
			}
		}
		
		@Override
		public void cancel() throws Exception {
			if(!state.compareAndSetBit(false, TASK_STATE_CANCELLED)) {
				return; // Already cancelled
			}
			
			state.unset(TASK_STATE_PAUSED | TASK_STATE_CALLED | TASK_STATE_DELAYED);
			
			Future<V> f;
			if((f = future) != null) {
				synchronized(this) {
					if((f = future) != null) {
						f.cancel(true);
					}
				}
			}
			
			cancelTask(this);
			mtxCalled.unlock();
		}
		
		@Override
		public void pause() throws Exception {
			if(!state.compareAndSetBit(false, TASK_STATE_PAUSED) || isDone() || isCancelled()) {
				return; // Already paused or no need to paused
			}
			
			// Do not pause the task here, since it can be resumed before it is actually run.
			
			if(!state.is(TASK_STATE_CALLED)) {
				pauseSubmittedTask(this);
			}
		}
		
		@Override
		public void resume() throws Exception {
			if(!state.compareAndUnsetBit(true, TASK_STATE_PAUSED) || isDone() || isCancelled()) {
				return; // Already resumed or no need to resume
			}
			
			boolean isDelayed = state.is(TASK_STATE_DELAYED);
			
			// If the task was already actually run, we have to submit it once more.
			if(isDelayed) {
				state.unset(TASK_STATE_CALLED | TASK_STATE_DELAYED);
				resumeDelayedTask(this);
			} else if(!state.is(TASK_STATE_CALLED)) {
				resumeSubmittedTask(this);
			}
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
				
				if(!isPaused()) {
					if(!isCancelled()) {
						state.set(TASK_STATE_DONE);
					}
					
					finishTask(this);
				}
			}
		}
		
		@Override
		public V get() throws Exception {
			if(!mtxCalled.await() || isCancelled()) {
				return null;
			}
			
			Future<V> f;
			if((f = future) == null) {
				synchronized(this) {
					if((f = future) == null) {
						return null;
					}
				}
			}
			
			return f.get();
		}
		
		@Override
		public Exception exception() {
			return exception;
		}
		
		@Override
		public boolean isPaused() {
			return state.is(TASK_STATE_PAUSED);
		}
		
		@Override
		public boolean isCancelled() {
			return state.is(TASK_STATE_CANCELLED);
		}
		
		@Override
		public boolean isDone() {
			return state.is(TASK_STATE_DONE);
		}
	}
	
	@FunctionalInterface
	public static interface QueueTask<V> extends Callable<V> {
	}
	
	public static interface QueueTaskResult<V> {
		
		void cancel() throws Exception;
		/** @since 00.02.09 */
		void pause() throws Exception;
		/** @since 00.02.09 */
		void resume() throws Exception;
		
		V get() throws Exception;
		Exception exception();
		
		/** @since 00.02.09 */
		boolean isPaused();
		/** @since 00.02.09 */
		boolean isDone();
		/** @since 00.02.09 */
		boolean isCancelled();
	}
}