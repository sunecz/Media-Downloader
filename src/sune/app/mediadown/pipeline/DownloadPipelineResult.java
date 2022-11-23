package sune.app.mediadown.pipeline;

import java.util.List;
import java.util.Objects;

import sune.app.mediadown.convert.ConversionMedia;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.util.Metadata;

/** @since 00.01.26 */
public final class DownloadPipelineResult implements PipelineResult<ConversionPipelineResult> {
	
	private final boolean needConversion;
	/** @since 00.02.08 */
	private final ResolvedMedia output;
	/** @since 00.02.08 */
	private final List<ConversionMedia> inputs;
	/** @since 00.02.08 */
	private final Metadata metadata;
	
	/** @since 00.02.08 */
	private DownloadPipelineResult(boolean needConversion, ResolvedMedia output, List<ConversionMedia> inputs,
			Metadata metadata) {
		this.needConversion = needConversion;
		this.output = output;
		this.inputs = inputs;
		this.metadata = metadata;
	}
	
	/** @since 00.02.08 */
	private static final List<ConversionMedia> checkInputs(List<ConversionMedia> inputs) {
		if(inputs == null || inputs.isEmpty()) {
			throw new IllegalArgumentException();
		}
		
		return inputs;
	}
	
	public static final DownloadPipelineResult noConversion() {
		return new DownloadPipelineResult(false, null, null, null);
	}
	
	/** @since 00.02.08 */
	public static final DownloadPipelineResult doConversion(ResolvedMedia output, List<ConversionMedia> inputs,
			Metadata metadata) {
		return new DownloadPipelineResult(true, Objects.requireNonNull(output), checkInputs(inputs), metadata);
	}
	
	@Override
	public final PipelineTask<ConversionPipelineResult> process(Pipeline pipeline) throws Exception {
		if(needConversion) {
			return ConversionPipelineTask.of(output, inputs, metadata);
		}
		
		return TerminatingPipelineTask.getTypedInstance();
	}
	
	@Override
	public final boolean isTerminating() {
		return !needConversion;
	}
}