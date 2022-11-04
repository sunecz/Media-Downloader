package sune.app.mediadown.gui.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.ProgressWindow;
import sune.app.mediadown.gui.ProgressWindow.ProgressAction;
import sune.app.mediadown.gui.ProgressWindow.ProgressContext;
import sune.app.mediadown.gui.Window;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.CheckedFunction;
import sune.app.mediadown.util.CounterLock;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.SyncObject;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.WorkerProxy;
import sune.app.mediadown.util.WorkerUpdatableTask;

/** @since 00.02.07 */
public abstract class ProgressPipelineTaskBase<T, R extends PipelineResult<?>, W extends Window<?>>
		implements PipelineTask<R> {
	
	protected final W window;
	
	protected final AtomicBoolean running = new AtomicBoolean();
	protected final AtomicBoolean started = new AtomicBoolean();
	protected final AtomicBoolean stopped = new AtomicBoolean();
	protected final AtomicBoolean paused = new AtomicBoolean();
	
	protected final SyncObject lockPause = new SyncObject();
	protected final SyncObject lockResult = new SyncObject();
	
	protected final Set<T> resultSet = new HashSet<>();
	protected final ObservableList<T> result = FXCollections.observableArrayList();
	
	public ProgressPipelineTaskBase(W window) {
		this.window = window;
	}
	
	/** @since 00.02.07 */
	protected void submit(ProgressAction action) {
		// By default, delegate all to the Progress window
		ProgressWindow.submitAction(window, action);
	}
	
	protected abstract CheckedFunction<CheckedBiFunction<WorkerProxy, T, Boolean>,
		WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, T, Boolean>, Void>> getTask();
	protected abstract R getResult(W window, List<T> result);
	protected abstract String getProgressText(W window);
	
	// ----- "Default" abstract methods
	
	/** @since 00.02.07 */
	protected void onCancelled() throws Exception { /* Do nothing by default */ }
	
	// -----
	
	@Override
	public R run(Pipeline pipeline) throws Exception {
		started.set(true);
		running.set(true);
		stopped.set(false);
		paused.set(false);
		resultSet.clear();
		result.clear();
		
		final CounterLock lock = new CounterLock();
		submit(new ProgressAction() {
			
			@Override
			public void action(ProgressContext context) {
				try {
					context.setProgress(ProgressContext.PROGRESS_INDETERMINATE);
					context.setText(getProgressText(window));
					
					getTask().apply((proxy, item) -> {
						if(paused.get()) {
							lockPause.await();
						}
						
						if(stopped.get()) {
							proxy.cancel();
							return false;
						}
						
						FXUtils.thread(() -> {
							// Remove duplicates while adding the items
							if(!resultSet.contains(item)) {
								result.add(item);
								resultSet.add(item);
							}
							lock.decrement();
						});
						
						lock.increment();
						return true;
					}).startAndWaitChecked();
					
					// Check whether the task was cancelled
					if(stopped.get()) {
						onCancelled();
					}
				} catch(Exception ex) {
					MediaDownloader.error(ex);
				} finally {
					context.setProgress(ProgressContext.PROGRESS_DONE);
					running.set(false);
					lockResult.unlock();
				}
			}
			
			@Override
			public void cancel() {
				Utils.ignore(ProgressPipelineTaskBase.this::stop);
			}
		});
		lockResult.await();
		lock.await();
		return getResult(window, result);
	}
	
	@Override
	public void stop() throws Exception {
		stopped.set(true);
	}
	
	@Override
	public void pause() throws Exception {
		paused.set(true);
	}
	
	@Override
	public void resume() throws Exception {
		paused.set(false);
		lockPause.unlock();
	}
	
	@Override
	public boolean isRunning() throws Exception {
		return running.get();
	}
	
	@Override
	public boolean isStarted() throws Exception {
		return started.get();
	}
	
	@Override
	public boolean isDone() throws Exception {
		return !stopped.get();
	}
	
	@Override
	public boolean isPaused() throws Exception {
		return paused.get();
	}
	
	@Override
	public boolean isStopped() throws Exception {
		return stopped.get();
	}
	
	public final ObservableList<Object> getResultList() {
		@SuppressWarnings("unchecked")
		ObservableList<Object> castedResult = (ObservableList<Object>) result;
		return castedResult;
	}
}