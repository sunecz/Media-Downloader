package sune.app.mediadown.pipeline.state;

import sune.app.mediadown.util.JSONSerializable;

/** @since 00.02.09 */
public interface PipelineState extends JSONSerializable {
	
	Metrics metrics();
}
