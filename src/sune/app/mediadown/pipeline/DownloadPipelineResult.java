package sune.app.mediadown.pipeline;

import java.util.List;
import java.util.Objects;

import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.gui.table.ResolvedMedia;

/** @since 00.01.26 */
public final class DownloadPipelineResult implements PipelineResult {
	
	private final boolean needConversion;
	/** @since 00.02.08 */
	private final List<ConversionMedia> inputs;
	/** @since 00.02.08 */
	private final ResolvedMedia output;
	
	/** @since 00.02.08 */
	private DownloadPipelineResult(boolean needConversion, List<ConversionMedia> inputs, ResolvedMedia output) {
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
	
	public static final DownloadPipelineResult noConversion() {
		return new DownloadPipelineResult(false, null, null);
	}
	
	/** @since 00.02.08 */
	public static final DownloadPipelineResult doConversion(List<ConversionMedia> inputs, ResolvedMedia output) {
		return new DownloadPipelineResult(true, checkInputs(inputs), Objects.requireNonNull(output));
	}
	
	@Override
	public final PipelineTask process(Pipeline pipeline) throws Exception {
		// Try to fix the media, if a fixing is requested, before anything else
		if(output.media().metadata().get("media.fix.required", false)) {
			return MediaFixPipelineTask.of(needConversion, inputs, output);
		}
		
		if(needConversion) {
			return ConversionPipelineTask.of(inputs, output);
		}
		
		return TerminatingPipelineTask.getTypedInstance();
	}
	
	@Override
	public final boolean isTerminating() {
		return !needConversion;
	}
	
	/** @since 00.02.09 */
	public List<ConversionMedia> inputs() {
		return inputs;
	}
	
	/** @since 00.02.09 */
	public ResolvedMedia output() {
		return output;
	}
	
	/** @since 00.02.09 */
	public boolean needConversion() {
		return needConversion;
	}
}