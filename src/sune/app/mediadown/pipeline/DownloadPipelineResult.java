package sune.app.mediadown.pipeline;

import java.nio.file.Path;

import sune.app.mediadown.convert.ConversionConfiguration;
import sune.app.mediadown.media.MediaFormat;

/** @since 00.01.26 */
public final class DownloadPipelineResult implements PipelineResult<ConversionPipelineResult> {
	
	private final boolean needConversion;
	private final ConversionConfiguration configuration;
	private final MediaFormat formatInput;
	private final MediaFormat formatOutput;
	private final Path fileOutput;
	private final Path[] filesInput;
	
	private DownloadPipelineResult(boolean needConversion, ConversionConfiguration configuration,
			MediaFormat formatInput, MediaFormat formatOutput, Path fileOutput, Path[] filesInput) {
		this.needConversion = needConversion;
		this.configuration = configuration;
		this.formatInput = formatInput;
		this.formatOutput = formatOutput;
		this.fileOutput = fileOutput;
		this.filesInput = filesInput;
	}
	
	public static final DownloadPipelineResult noConversion() {
		return new DownloadPipelineResult(false, null, null, null, null, null);
	}
	
	public static final DownloadPipelineResult doConversion(ConversionConfiguration configuration,
			MediaFormat formatInput, MediaFormat formatOutput, Path fileOutput, Path... filesInput) {
		return new DownloadPipelineResult(true, configuration, formatInput, formatOutput, fileOutput, filesInput);
	}
	
	@Override
	public final PipelineTask<ConversionPipelineResult> process(Pipeline pipeline) throws Exception {
		if((needConversion)) {
			return ConversionPipelineTask.of(configuration, formatInput, formatOutput, fileOutput, filesInput);
		}
		return TerminatingPipelineTask.getTypedInstance();
	}
	
	@Override
	public final boolean isTerminating() {
		return !needConversion;
	}
}