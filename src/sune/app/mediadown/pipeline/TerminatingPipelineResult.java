package sune.app.mediadown.pipeline;

/** @since 00.01.26 */
public final class TerminatingPipelineResult implements PipelineResult {
	
	private static final TerminatingPipelineResult INSTANCE = new TerminatingPipelineResult();
	
	public static final TerminatingPipelineResult getInstance() {
		return (TerminatingPipelineResult) INSTANCE;
	}
	
	@SuppressWarnings("unchecked")
	public static final <T extends PipelineResult> T getTypedInstance() {
		return (T) INSTANCE;
	}
	
	// Forbid anyone to create an instance of this class
	private TerminatingPipelineResult() {
	}
	
	@Override
	public PipelineTask process(Pipeline pipeline) throws Exception {
		return TerminatingPipelineTask.getTypedInstance();
	}
	
	@Override
	public boolean isTerminating() {
		return true;
	}
}