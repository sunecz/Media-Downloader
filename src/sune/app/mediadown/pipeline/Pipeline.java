package sune.app.mediadown.pipeline;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import sune.app.mediadown.HasTaskState;
import sune.app.mediadown.InternalState;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.PipelineEvent;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.History;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.SyncObject;
import sune.app.mediadown.util.Threads;

/** @since 00.01.26 */
public final class Pipeline implements EventBindable<EventType>, HasTaskState {
	
	/** @since 00.02.08 */
	private final InternalState state = new InternalState(TaskStates.INITIAL);
	private final EventRegistry<EventType> eventRegistry = new EventRegistry<>();
	private final SyncObject lockPause = new SyncObject();
	private final SyncObject lockDone = new SyncObject();
	
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
		if(isPaused()) {
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
		state.set(TaskStates.ERROR);
		exception.set(ex);
		eventRegistry.call(PipelineEvent.ERROR, new Pair<>(this, ex));
	}
	
	private final void invoke() throws Exception {
		try {
			while(isRunning()) {
				waitIfPaused();
				
				if(tasks.isEmpty()) {
					// If the input is null, we have nothing to do
					if(input == null || input.isTerminating()) {
						break;
					}
				} else {
					// Otherwise process the next task
					PipelineTask<?> localTask = tasks.poll();
					// Allow pipeline to end, if the next task is null
					if(localTask == null) {
						break;
					}
					
					setTask(localTask);
					setInput(localTask.run(this));
					
					// Terminate the pipeline if necessary
					if(input == null || input.isTerminating()) {
						break;
					}
				}
				
				addNextTask(); // Handle possible null tasks
				loopEnd(); // Allow resets
			}
		} finally {
			doStop(TaskStates.DONE);
			lockDone.unlock();
			eventRegistry.call(PipelineEvent.END, this);
		}
	}
	
	/** @since 00.02.08 */
	private final void runnable() {
		try {
			invoke();
		} catch(Exception ex) {
			error(ex);
		}
	}
	
	private final void doWithTask(CheckedConsumer<PipelineTask<?>> consumer) throws Exception {
		PipelineTask<?> localTask;
		if((localTask = task.get()) == null) {
			return;
		}
		
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
		if((input = resetInput.getAndSet(null)) != null) {
			this.tasks.clear();
			this.task.set(null);
			setInput(input);
			return; // Do not continue
		}
		
		PipelineTask<?> task;
		if((task = resetTask.getAndSet(null)) != null) {
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
	
	/** @since 00.02.08 */
	private final void doStop(int stopState) throws Exception {
		if(isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.RUNNING);
		state.unset(TaskStates.PAUSED);
		
		state.set(stopState);
		
		stopTask();
		lockPause.unlock();
	}
	
	public final void setInput(PipelineResult<?> input) {
		this.input = input;
		historyInputs.add(input);
		eventRegistry.call(PipelineEvent.INPUT, new Pair<>(this, input));
	}
	
	/** @since 00.01.27 */
	public final void addTask(PipelineTask<?> task) {
		tasks.add(Objects.requireNonNull(task));
	}
	
	/** @since 00.01.27 */
	public final void reset(PipelineResult<?> input) {
		resetInput.set(input);
	}
	
	/** @since 00.01.27 */
	public final void reset(PipelineTask<?> task) {
		resetTask.set(Objects.requireNonNull(task));
	}
	
	public final void start() throws Exception {
		state.clear(TaskStates.STARTED);
		state.set(TaskStates.RUNNING);
		
		thread = Threads.newThread(this::runnable);
		thread.start();
		
		eventRegistry.call(PipelineEvent.BEGIN, this);
	}
	
	public final void stop() throws Exception {
		if(!isStarted() || isStopped() || isDone()) {
			return;
		}
		
		doStop(TaskStates.STOPPED);
	}
	
	public final void pause() throws Exception {
		if(!isStarted() || isPaused() || isStopped() || isDone()) {
			return;
		}
		
		state.set(TaskStates.PAUSED);
		state.unset(TaskStates.RUNNING);
		
		pauseTask();
		
		eventRegistry.call(PipelineEvent.PAUSE, this);
	}
	
	public final void resume() throws Exception {
		if(!isStarted() || !isPaused() || isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.PAUSED);
		state.set(TaskStates.RUNNING);
		lockPause.unlock();
		
		resumeTask();
		
		eventRegistry.call(PipelineEvent.RESUME, this);
	}
	
	/** @since 00.01.27 */
	public final Pipeline waitFor() {
		if(!isDone()) {
			lockDone.await();
		}
		
		return this;
	}
	
	@Override
	public final boolean isRunning() {
		return state.is(TaskStates.RUNNING);
	}
	
	@Override
	public final boolean isDone() {
		return state.is(TaskStates.DONE);
	}
	
	@Override
	public final boolean isStarted() {
		return state.is(TaskStates.STARTED);
	}
	
	@Override
	public final boolean isPaused() {
		return state.is(TaskStates.PAUSED);
	}
	
	@Override
	public final boolean isStopped() {
		return state.is(TaskStates.STOPPED);
	}
	
	@Override
	public boolean isError() {
		return state.is(TaskStates.ERROR);
	}
	
	@Override
	public final <V> void addEventListener(Event<? extends EventType, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public final <V> void removeEventListener(Event<? extends EventType, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
	
	public final PipelineTask<?> getTask() {
		return task.get();
	}
	
	/** @since 00.01.27 */
	public final PipelineResult<?> getResult() {
		return input;
	}
	
	public final Exception getException() {
		return exception.get();
	}
	
	public final EventRegistry<EventType> getEventRegistry() {
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