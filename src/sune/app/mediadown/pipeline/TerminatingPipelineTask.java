package sune.app.mediadown.pipeline;

/** @since 00.01.26 */
public final class TerminatingPipelineTask implements PipelineTask {
	
	private static final TerminatingPipelineTask INSTANCE = new TerminatingPipelineTask();
	
	public static final TerminatingPipelineTask getInstance() {
		return (TerminatingPipelineTask) INSTANCE;
	}
	
	@SuppressWarnings("unchecked")
	public static final <T extends PipelineTask> T getTypedInstance() {
		return (T) INSTANCE;
	}
	
	// Forbid anyone to create an instance of this class
	private TerminatingPipelineTask() {
	}
	
	@Override
	public TerminatingPipelineResult run(Pipeline pipeline) throws Exception {
		return TerminatingPipelineResult.getTypedInstance();
	}
	
	@Override public void stop() throws Exception { /* Do nothing */ }
	@Override public void pause() throws Exception { /* Do nothing */ }
	@Override public void resume() throws Exception { /* Do nothing */ }
	@Override public boolean isRunning() { return false; }
	@Override public boolean isStarted() { return true; }
	@Override public boolean isDone() { return true; }
	@Override public boolean isPaused() { return false; }
	@Override public boolean isStopped() { return false; }
	@Override public boolean isError() { return false; }
}