package sune.app.mediadown.pipeline;

/** @since 00.01.26 */
public final class TerminatingPipelineTask implements PipelineTask<PipelineResult<?>> {
	
	private static final PipelineTask<PipelineResult<?>> INSTANCE = new TerminatingPipelineTask();
	
	public static final TerminatingPipelineTask getInstance() {
		return (TerminatingPipelineTask) INSTANCE;
	}
	
	@SuppressWarnings("unchecked")
	public static final <T extends PipelineResult<?>> PipelineTask<T> getTypedInstance() {
		return (PipelineTask<T>) INSTANCE;
	}
	
	// Forbid anyone to create an instance of this class
	private TerminatingPipelineTask() {
	}
	
	@Override
	public PipelineResult<?> run(Pipeline pipeline) throws Exception {
		return TerminatingPipelineResult.getTypedInstance();
	}
	
	@Override public final void stop() throws Exception { /* Do nothing */ }
	@Override public final void pause() throws Exception { /* Do nothing */ }
	@Override public final void resume() throws Exception { /* Do nothing */ }
	@Override public final boolean isRunning() { return false; }
	@Override public final boolean isStarted() { return true; }
	@Override public final boolean isDone() { return true; }
	@Override public final boolean isPaused() { return false; }
	@Override public final boolean isStopped() { return false; }
}