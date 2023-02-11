package sune.app.mediadown.concurrent;

import java.util.concurrent.atomic.AtomicReference;

import sune.app.mediadown.HasTaskState;
import sune.app.mediadown.InternalState;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.util.Pair;

/** @since 00.02.08 */
public abstract class Task implements EventBindable<EventType>, HasTaskState {
	
	protected final EventRegistry<EventType> eventRegistry = new EventRegistry<>();
	protected final InternalState state = new InternalState(TaskStates.INITIAL);
	protected final SyncObject lockPause = new SyncObject();
	protected final AtomicReference<Exception> exception = new AtomicReference<>();
	protected final StateMutex mtxDone = new StateMutex();
	protected Thread thread;
	
	protected final <V> void call(Event<? extends EventType, V> event) {
		eventRegistry.call(event);
	}
	
	protected final <V> void call(Event<? extends EventType, V> event, V value) {
		eventRegistry.call(event, value);
	}
	
	protected void doRun() {
		try {
			run();
		} catch(Exception ex) {
			state.set(TaskStates.ERROR);
			exception.set(ex);
			call(TaskEvent.ERROR, new Pair<>(this, ex));
		} finally {
			stop(TaskStates.DONE);
			call(TaskEvent.END, this);
		}
	}
	
	protected void stop(int stopState) {
		if(isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.RUNNING);
		state.unset(TaskStates.PAUSED);
		state.set(stopState);
		
		lockPause.unlock();
		mtxDone.unlock();
	}
	
	protected void awaitPaused() {
		if(isPaused()) {
			lockPause.await();
		}
	}
	
	protected abstract void run() throws Exception;
	
	public void start() throws Exception {
		if(isStarted()) {
			return; // Start only once
		}
		
		state.clear(TaskStates.STARTED);
		state.set(TaskStates.RUNNING);
		
		call(TaskEvent.BEGIN, this);
		
		thread = Threads.newThread(this::doRun);
		thread.start();
	}
	
	public void startAndWait() throws Exception {
		if(isStarted()) {
			return; // Start only once
		}
		
		start();
		await();
	}
	
	public void pause() throws Exception {
		if(!isStarted() || isPaused() || isStopped() || isDone()) {
			return;
		}
		
		state.set(TaskStates.PAUSED);
		state.unset(TaskStates.RUNNING);
		
		call(TaskEvent.PAUSE, this);
	}
	
	public void resume() throws Exception {
		if(!isStarted() || !isPaused() || isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.PAUSED);
		state.set(TaskStates.RUNNING);
		
		lockPause.unlock();
		
		call(TaskEvent.RESUME, this);
	}
	
	public void stop() throws Exception {
		if(!isStarted() || isStopped() || isDone()) {
			return;
		}
		
		stop(TaskStates.STOPPED);
	}
	
	public void await() throws Exception {
		mtxDone.await();
		
		Exception ex;
		if((ex = exception.get()) != null) {
			throw ex; // Propagate
		}
	}
	
	public Exception exception() {
		return exception.get();
	}
	
	@Override
	public <V> void addEventListener(Event<? extends EventType, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends EventType, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
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
	
	public static final class TaskEvent implements EventType {
		
		public static final Event<TaskEvent, Task> BEGIN                  = new Event<>();
		public static final Event<TaskEvent, Task> PAUSE                  = new Event<>();
		public static final Event<TaskEvent, Task> RESUME                 = new Event<>();
		public static final Event<TaskEvent, Task> END                    = new Event<>();
		public static final Event<TaskEvent, Pair<Task, Exception>> ERROR = new Event<>();
		
		// TODO: Add missing methods
	}
}