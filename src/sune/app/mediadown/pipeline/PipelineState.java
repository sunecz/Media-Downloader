package sune.app.mediadown.pipeline;

import sune.app.mediadown.util.JSONSerializable;

/** @since 00.02.09 */
public interface PipelineState extends JSONSerializable {
	
	Metrics metrics();
}
