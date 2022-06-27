package sune.app.mediadown.event;

import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class PipelineEvent implements IEventType {
	
	public static final EventType<PipelineEvent, Pipeline>                          BEGIN  = new EventType<>();
	public static final EventType<PipelineEvent, Pair<Pipeline, PipelineTask<?>>>   UPDATE = new EventType<>();
	/** @since 00.01.27 */
	public static final EventType<PipelineEvent, Pair<Pipeline, PipelineResult<?>>> INPUT  = new EventType<>();
	public static final EventType<PipelineEvent, Pipeline>                          END    = new EventType<>();
	public static final EventType<PipelineEvent, Pair<Pipeline, Exception>>         ERROR  = new EventType<>();
	public static final EventType<PipelineEvent, Pipeline>                          PAUSE  = new EventType<>();
	public static final EventType<PipelineEvent, Pipeline>                          RESUME = new EventType<>();
	
	private static final EventType<PipelineEvent, ?>[] VALUES = Utils.array(BEGIN, UPDATE, INPUT, END, ERROR, PAUSE, RESUME);
	public  static final EventType<PipelineEvent, ?>[] values() {
		return VALUES;
	}
	
	// Forbid anyone to create an instance of this class
	private PipelineEvent() {
	}
}