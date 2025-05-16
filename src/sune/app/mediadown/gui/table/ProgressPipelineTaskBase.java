package sune.app.mediadown.gui.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sune.app.mediadown.InternalState;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.concurrent.CounterLock;
import sune.app.mediadown.concurrent.StateMutex;
import sune.app.mediadown.gui.ProgressWindow;
import sune.app.mediadown.gui.ProgressWindow.ProgressAction;
import sune.app.mediadown.gui.ProgressWindow.ProgressContext;
import sune.app.mediadown.gui.util.FXUtils;
import sune.app.mediadown.gui.Window;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.task.ListTask;
import sune.app.mediadown.task.ListTask.ListTaskEvent;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.07 */
public abstract class ProgressPipelineTaskBase<T, W extends Window<?>> implements PipelineTask {
	
	protected final W window;
	
	protected final InternalState state = new InternalState(TaskStates.INITIAL);
	protected final StateMutex mtxDone = new StateMutex();
	
	protected final Set<T> resultSet = new HashSet<>();
	protected final ObservableList<T> result = FXCollections.observableArrayList();
	
	private volatile ListTask<T> task;
	
	public ProgressPipelineTaskBase(W window) {
		this.window = window;
	}
	
	/** @since 00.02.07 */
	protected void submit(ProgressAction action) {
		// By default, delegate all to the Progress window
		ProgressWindow.submitAction(window, action);
	}
	
	protected abstract ListTask<T> getTask();
	protected abstract PipelineResult getResult(W window, List<T> result);
	protected abstract String getProgressText(W window);
	
	// ----- "Default" abstract methods
	
	/** @since 00.02.07 */
	protected void onCancelled() throws Exception { /* Do nothing by default */ }
	
	// -----
	
	private final void stop(int stopState) throws Exception {
		if(isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.RUNNING);
		state.unset(TaskStates.PAUSED);
		state.set(stopState);
		
		try {
			if(task != null) {
				task.stop();
			}
		} finally {
			mtxDone.unlock();
		}
	}
	
	@Override
	public PipelineResult run(Pipeline pipeline) throws Exception {
		state.clear(TaskStates.STARTED);
		state.set(TaskStates.RUNNING);
		
		resultSet.clear();
		result.clear();
		
		submit(new ProgressAction() {
			
			@Override
			public void action(ProgressContext context) {
				try {
					final CounterLock lock = new CounterLock();
					context.setProgress(ProgressContext.PROGRESS_INDETERMINATE);
					context.setText(getProgressText(window));
					
					task = getTask();
					task.addEventListener(ListTaskEvent.ADD, (pair) -> {
						T item = Utils.cast(pair.b);
						
						FXUtils.thread(() -> {
							// Remove duplicates while adding the items
							if(!resultSet.contains(item)) {
								result.add(item);
								resultSet.add(item);
							}
							
							lock.decrement();
						});
						
						lock.increment();
					});
					
					task.startAndWait();
					lock.await();
					
					if(task.isStopped()) {
						onCancelled();
					}
				} catch(Exception ex) {
					MediaDownloader.error(ex);
				} finally {
					try {
						stop(TaskStates.DONE);
					} catch(Exception ex) {
						MediaDownloader.error(ex);
					} finally {
						context.setProgress(ProgressContext.PROGRESS_DONE);
					}
				}
			}
			
			@Override
			public void cancel() {
				Ignore.callVoid(ProgressPipelineTaskBase.this::stop);
			}
		});
		
		mtxDone.awaitAndReset();
		
		return getResult(window, result);
	}
	
	@Override
	public void stop() throws Exception {
		if(!isStarted() || isStopped() || isDone()) {
			return;
		}
		
		if(task != null) {
			task.stop();
		}
		
		stop(TaskStates.STOPPED);
	}
	
	@Override
	public void pause() throws Exception {
		if(!isStarted() || isPaused() || isStopped() || isDone()) {
			return;
		}
		
		state.set(TaskStates.PAUSED);
		state.unset(TaskStates.RUNNING);
		
		if(task != null) {
			task.pause();
		}
	}
	
	@Override
	public void resume() throws Exception {
		if(!isStarted() || !isPaused() || isStopped() || isDone()) {
			return;
		}
		
		state.unset(TaskStates.PAUSED);
		state.set(TaskStates.RUNNING);
		
		if(task != null) {
			task.resume();
		}
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
	
	public final ObservableList<Object> getResultList() {
		return Utils.cast(result);
	}
}