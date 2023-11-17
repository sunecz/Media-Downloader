package sune.app.mediadown.pipeline;

import sune.app.mediadown.InternalState;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.concurrent.SyncObject;

/** @since 00.02.09 */
public abstract class AbstractPipelineTask implements PipelineTask {
	
	protected final InternalState state = new InternalState();
	protected final SyncObject lockPause = new SyncObject();
	
	protected AbstractPipelineTask() {
	}
	
	protected boolean checkState() {
		if(isPaused()) {
			lockPause.await();
		}
		
		return state.is(TaskStates.RUNNING);
	}
	
	protected abstract PipelineResult doRun(Pipeline pipeline) throws Exception;
	
	protected PipelineResult doRunAlreadyRunning(Pipeline pipeline) throws Exception {
		// By default do nothing
		return null;
	}
	
	protected void doStop() throws Exception {
		// By default do nothing
	}
	
	protected void doPause() throws Exception {
		// By default do nothing
	}
	
	protected void doResume() throws Exception {
		// By default do nothing
	}
	
	protected Exception doError(Pipeline pipeline, Exception exception) {
		// By default do nothing
		return exception;
	}
	
	@Override
	public PipelineResult run(Pipeline pipeline) throws Exception {
		if(state.is(TaskStates.STARTED) && state.is(TaskStates.RUNNING)) {
			return doRunAlreadyRunning(pipeline);
		}
		
		state.set(TaskStates.STARTED);
		state.set(TaskStates.RUNNING);
		
		try {
			PipelineResult result = doRun(pipeline);
			state.set(TaskStates.DONE);
			return result;
		} catch(Exception ex) {
			ex = doError(pipeline, ex);
			state.set(TaskStates.ERROR);
			throw ex; // Forward the exception
		} finally {
			stop();
		}
	}
	
	@Override
	public void stop() throws Exception {
		if(state.is(TaskStates.STOPPED)) {
			return; // Nothing to do
		}
		
		state.unset(TaskStates.RUNNING);
		state.unset(TaskStates.PAUSED);
		lockPause.unlock();
		
		doStop();
		
		if(!state.is(TaskStates.DONE)) {
			state.set(TaskStates.STOPPED);
		}
	}
	
	@Override
	public void pause() throws Exception {
		if(state.is(TaskStates.PAUSED)) {
			return; // Nothing to do
		}
		
		doPause();
		
		state.unset(TaskStates.RUNNING);
		state.set(TaskStates.PAUSED);
	}
	
	@Override
	public void resume() throws Exception {
		if(!state.is(TaskStates.PAUSED)) {
			return; // Nothing to do
		}
		
		doResume();
		
		state.unset(TaskStates.PAUSED);
		state.set(TaskStates.RUNNING);
		lockPause.unlock();
	}
	
	@Override
	public boolean isRunning() {
		return state.is(TaskStates.RUNNING);
	}
	
	@Override
	public boolean isStarted() {
		return state.is(TaskStates.STARTED);
	}
	
	@Override
	public boolean isDone() {
		return state.is(TaskStates.DONE);
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