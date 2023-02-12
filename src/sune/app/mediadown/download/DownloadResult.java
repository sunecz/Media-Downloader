package sune.app.mediadown.download;

import sune.app.mediadown.pipeline.ConversionPipelineResult;
import sune.app.mediadown.pipeline.PipelineResultable;

/** @since 00.02.08 */
public interface DownloadResult extends PipelineResultable<ConversionPipelineResult> {
	
	Download download();
}