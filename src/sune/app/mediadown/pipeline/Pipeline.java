package sune.app.mediadown.pipeline;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.IEventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.PipelineEvent;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.History;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.SyncObject;
import sune.app.mediadown.util.Threads;

/** @since 00.01.26 */
public final class Pipeline {
	
	public static final class PipelineEventRegistry {
		
		private final EventRegistry<IEventType> eventRegistry = new EventRegistry<>();
		
		@SuppressWarnings("unchecked")
		public final <E> void call(EventType<? extends IEventType, E> type) {
			eventRegistry.call((EventType<IEventType, E>) type);
		}
		
		@SuppressWarnings("unchecked")
		public final <E> void call(EventType<? extends IEventType, E> type, E value) {
			eventRegistry.call((EventType<IEventType, E>) type, value);
		}
		
		@SuppressWarnings("unchecked")
		public final <E> void add(EventType<? extends IEventType, E> type, Listener<E> listener) {
			eventRegistry.add((EventType<IEventType, E>) type, listener);
		}
		
		@SuppressWarnings("unchecked")
		public final <E> void remove(EventType<? extends IEventType, E> type, Listener<E> listener) {
			eventRegistry.remove((EventType<IEventType, E>) type, listener);
		}
		
		@SuppressWarnings("unchecked")
		public final <E> List<Listener<?>> getListeners(EventType<? extends IEventType, E> type) {
			return eventRegistry.getListeners().get((EventType<IEventType, E>) type);
		}
		
		@SuppressWarnings("unchecked")
		public final <T extends IEventType, E> void bindEvents(EventBindable<T> eventBindable, EventType<T, E> type) {
			List<Listener<?>> listeners = getListeners(type);
			if((listeners == null)) return; // Nothing to do
			for(Listener<?> listener : listeners) {
				eventBindable.addEventListener(type, (Listener<E>) listener);
			}
		}
	}
	
	private final PipelineEventRegistry eventRegistry = new PipelineEventRegistry();
	
	private final SyncObject lockPause = new SyncObject();
	private final SyncObject lockDone = new SyncObject();
	private final AtomicBoolean running = new AtomicBoolean();
	private final AtomicBoolean started = new AtomicBoolean();
	private final AtomicBoolean done = new AtomicBoolean();
	private final AtomicBoolean paused = new AtomicBoolean();
	private final AtomicBoolean stopped = new AtomicBoolean();
	
	private final Queue<PipelineTask<?>> tasks = new LinkedList<>();
	private final AtomicReference<PipelineTask<?>> task = new AtomicReference<>();
	private final AtomicReference<Exception> exception = new AtomicReference<>();
	private Thread thread;
	private PipelineResult<?> input;
	
	/** @since 00.01.27 */
	private final AtomicReference<PipelineResult<?>> resetInput = new AtomicReference<>();
	/** @since 00.01.27 */
	private final AtomicReference<PipelineTask<?>> resetTask = new AtomicReference<>();
	/** @since 00.01.27 */
	private final History<PipelineResult<?>> historyInputs = new History<>();
	/** @since 00.01.27 */
	private final History<PipelineTask<?>> historyTasks = new History<>();
	
	// Hide the constructor for possible future changes
	private Pipeline() {
	}
	
	public static final Pipeline create() {
		// Make the constructor accessible through factory method
		return new Pipeline();
	}
	
	private final void waitIfPaused() {
		if((paused.get())) {
			lockPause.await();
		}
	}
	
	private final PipelineTask<?> getNextTask() throws Exception {
		return input.process(this);
	}
	
	private final void addNextTask() throws Exception {
		// Allow null tasks to end the pipeline, if needed
		tasks.add(getNextTask());
	}
	
	private final void error(Exception ex) {
		exception.set(ex);
		eventRegistry.call(PipelineEvent.ERROR, new Pair<>(this, ex));
	}
	
	private final void markAsDone() {
		done.set(true);
		lockDone.unlock();
	}
	
	private final void invoke() {
		try {
			while(running.get()) {
				waitIfPaused();
				try {
					if((tasks.isEmpty())) {
						// If the input is null, we have nothing to do
						if((input == null || input.isTerminating()))
							break;
					} else {
						// Otherwise process the next task
						PipelineTask<?> localTask = tasks.poll();
						// Allow pipeline to end, if the next task is null
						if((localTask == null))
							break;
						setTask(localTask);
						setInput(localTask.run(this));
						// Terminate the pipeline if necessary
						if((input == null || input.isTerminating()))
							break;
					}
					addNextTask(); // Handle possible null tasks
					loopEnd(); // Allow resets
				} catch(Exception ex) {
					error(ex);
					break;
				}
			}
		} finally {
			markAsDone();
			try {
				stop();
			} catch(Exception ex) {
				error(ex);
			}
		}
	}
	
	private final void doWithTask(CheckedConsumer<PipelineTask<?>> consumer)
			throws Exception {
		PipelineTask<?> localTask;
		if((localTask = task.get()) == null)
			return;
		consumer.accept(localTask);
	}
	
	private final void stopTask() throws Exception {
		doWithTask(PipelineTask::stop);
	}
	
	private final void pauseTask() throws Exception {
		doWithTask(PipelineTask::pause);
	}
	
	private final void resumeTask() throws Exception {
		doWithTask(PipelineTask::resume);
	}
	
	/** @since 00.01.27 */
	private final void loopEnd() {
		PipelineResult<?> input;
		PipelineTask<?> task;
		if((input = resetInput.getAndSet(null)) != null) {
			this.tasks.clear();
			this.task.set(null);
			setInput(input);
		} else if((task = resetTask.getAndSet(null)) != null) {
			this.tasks.clear();
			this.task.set(null);
			addTask(task);
		}
	}
	
	/** @since 00.01.27 */
	private final void setTask(PipelineTask<?> task) {
		this.task.set(task);
		historyTasks.add(task);
		eventRegistry.call(PipelineEvent.UPDATE, new Pair<>(this, task));
	}
	
	public final void setInput(PipelineResult<?> input) {
		this.input = input;
		historyInputs.add(input);
		eventRegistry.call(PipelineEvent.INPUT, new Pair<>(this, input));
	}
	
	/** @since 00.01.27 */
	public final void addTask(PipelineTask<?> task) {
		if((task == null))
			throw new IllegalArgumentException();
		tasks.add(task);
	}
	
	/** @since 00.01.27 */
	public final void reset(PipelineResult<?> input) {
		resetInput.set(input);
	}
	
	/** @since 00.01.27 */
	public final void reset(PipelineTask<?> task) {
		if((task == null))
			throw new IllegalArgumentException();
		resetTask.set(task);
	}
	
	public final void start() throws Exception {
		running.set(true);
		started.set(true);
		paused.set(false);
		stopped.set(false);
		done.set(false);
		eventRegistry.call(PipelineEvent.BEGIN, this);
		thread = Threads.newThread(this::invoke);
		thread.start();
	}
	
	public final void stop() throws Exception {
		running.set(false);
		stopTask();
		paused.set(false);
		lockPause.unlock();
		if(!done.get()) stopped.set(true);
		eventRegistry.call(PipelineEvent.END, this);
	}
	
	public final void pause() throws Exception {
		running.set(false);
		pauseTask();
		paused.set(true);
	}
	
	public final void resume() throws Exception {
		paused.set(false);
		running.set(true);
		resumeTask();
		lockPause.unlock();
	}
	
	/** @since 00.01.27 */
	public final Pipeline waitFor() {
		if(!isDone())
			lockDone.await();
		return this;
	}
	
	public final boolean isRunning() {
		return running.get();
	}
	
	public final boolean isStarted() {
		return started.get();
	}
	
	public final boolean isDone() {
		return done.get();
	}
	
	public final boolean isPaused() {
		return paused.get();
	}
	
	public final boolean isStopped() {
		return stopped.get();
	}
	
	public final <T> void addEventListener(EventType<? extends IEventType, T> type, Listener<T> listener) {
		eventRegistry.add(type, listener);
	}
	
	public final <T> void removeEventListener(EventType<? extends IEventType, T> type, Listener<T> listener) {
		eventRegistry.remove(type, listener);
	}
	
	public final PipelineTask<?> getTask() {
		return task.get();
	}
	
	/** @since 00.01.27 */
	public PipelineResult<?> getResult() {
		return input;
	}
	
	public final Exception getException() {
		return exception.get();
	}
	
	public final PipelineEventRegistry getEventRegistry() {
		return eventRegistry;
	}
	
	/** @since 00.01.27 */
	public final History<PipelineResult<?>> getInputsHistory() {
		return historyInputs;
	}
	
	/** @since 00.01.27 */
	public final History<PipelineTask<?>> getTasksHistory() {
		return historyTasks;
	}
}