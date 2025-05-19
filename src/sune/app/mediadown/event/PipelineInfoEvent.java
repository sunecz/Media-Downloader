package sune.app.mediadown.event;

import sune.app.mediadown.pipeline.PipelineInfoData;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class PipelineInfoEvent implements EventType {
	
	public static final Event<PipelineInfoEvent, PipelineInfoData> UPDATE = new Event<>();
	
	private static Event<PipelineInfoEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private PipelineInfoEvent() {
	}
	
	public static final Event<PipelineInfoEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(UPDATE);
		}
		
		return values;
	}
}
