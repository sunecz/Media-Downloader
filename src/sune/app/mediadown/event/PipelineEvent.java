package sune.app.mediadown.event;

import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class PipelineEvent implements EventType {
	
	public static final Event<PipelineEvent, Pipeline>                       BEGIN  = new Event<>();
	public static final Event<PipelineEvent, Pair<Pipeline, PipelineTask>>   UPDATE = new Event<>();
	/** @since 00.01.27 */
	public static final Event<PipelineEvent, Pair<Pipeline, PipelineResult>> INPUT  = new Event<>();
	public static final Event<PipelineEvent, Pipeline>                       END    = new Event<>();
	public static final Event<PipelineEvent, Pair<Pipeline, Exception>>      ERROR  = new Event<>();
	public static final Event<PipelineEvent, Pipeline>                       PAUSE  = new Event<>();
	public static final Event<PipelineEvent, Pipeline>                       RESUME = new Event<>();
	
	private static Event<PipelineEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private PipelineEvent() {
	}
	
	public static final Event<PipelineEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(BEGIN, UPDATE, INPUT, END, ERROR, PAUSE, RESUME);
		}
		
		return values;
	}
}