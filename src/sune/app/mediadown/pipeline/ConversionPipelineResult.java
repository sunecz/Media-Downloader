package sune.app.mediadown.pipeline;

import java.util.List;
import java.util.Objects;

import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.gui.table.ResolvedMedia;

/** @since 00.01.26 */
public final class ConversionPipelineResult implements PipelineResult {
	
	private final boolean needConversion;
	/** @since 00.02.08 */
	private final List<ConversionMedia> inputs;
	/** @since 00.02.08 */
	private final ResolvedMedia output;
	
	/** @since 00.02.08 */
	private ConversionPipelineResult(boolean needConversion, List<ConversionMedia> inputs, ResolvedMedia output) {
		this.needConversion = needConversion;
		this.inputs = inputs;
		this.output = output;
	}
	
	/** @since 00.02.08 */
	private static final List<ConversionMedia> checkInputs(List<ConversionMedia> inputs) {
		if(inputs == null || inputs.isEmpty()) {
			throw new IllegalArgumentException();
		}
		
		return inputs;
	}
	
	public static final ConversionPipelineResult noConversion() {
		return new ConversionPipelineResult(false, null, null);
	}
	
	/** @since 00.02.08 */
	public static final ConversionPipelineResult doConversion(List<ConversionMedia> inputs, ResolvedMedia output) {
		return new ConversionPipelineResult(true, checkInputs(inputs), Objects.requireNonNull(output));
	}
	
	@Override
	public PipelineTask process(Pipeline pipeline) throws Exception {
		if(needConversion) {
			return ConversionPipelineTask.of(inputs, output);
		}
		
		return TerminatingPipelineTask.getTypedInstance();
	}
	
	@Override
	public boolean isTerminating() {
		return !needConversion;
	}
}