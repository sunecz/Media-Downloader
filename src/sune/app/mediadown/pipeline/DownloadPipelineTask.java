package sune.app.mediadown.pipeline;

import java.util.Objects;

import sune.app.mediadown.Download;
import sune.app.mediadown.download.DownloadResult;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.QueueEvent;
import sune.app.mediadown.manager.DownloadManager;
import sune.app.mediadown.manager.PositionAwareManagerSubmitResult;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.PositionAwareQueueTaskExecutor.PositionAwareQueueTaskResult;
import sune.app.mediadown.util.QueueContext;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.01.26 */
public final class DownloadPipelineTask implements PipelineTask<DownloadPipelineResult> {
	
	/** @since 00.02.08 */
	private final PipelineMedia media;
	
	private PositionAwareManagerSubmitResult<DownloadResult, Long> result;
	
	private DownloadPipelineTask(PipelineMedia media) {
		this.media = Objects.requireNonNull(media);
	}
	
	/** @since 00.02.08 */
	public static final DownloadPipelineTask of(PipelineMedia media) {
		return new DownloadPipelineTask(media);
	}
	
	@Override
	public final DownloadPipelineResult run(Pipeline pipeline) throws Exception {
		result = DownloadManager.instance().submit(media.media(), media.destination(), media.mediaConfiguration(),
			media.configuration());
		DownloadResult downloadResult = result.value();
		QueueContext context = result.context();
		
		// Notify the media of being submitted
		media.submit();
		
		// Notify the pipeline if the position in a queue changed
		PositionAwareQueueTaskResult<Long> positionAwareTaskResult = Utils.cast(result.taskResult());
		positionAwareTaskResult.queuePositionProperty().addListener((o, ov, queuePosition) -> {
			pipeline.getEventRegistry().call(
				QueueEvent.POSITION_UPDATE,
				new Pair<>(context, queuePosition.intValue())
			);
		});
		
		pipeline.getEventRegistry().call(
			QueueEvent.POSITION_UPDATE,
			new Pair<>(context, positionAwareTaskResult.queuePosition())
		);
		
		// Bind all events from the pipeline
		EventRegistry<EventType> eventRegistry = pipeline.getEventRegistry();
		Download download = downloadResult.download();
		eventRegistry.bindAll(download, DownloadEvent.values());
		
		Ignore.Cancellation.call(result::get); // Wait for the download to finish
		return (DownloadPipelineResult) downloadResult.pipelineResult();
	}
	
	private final Download download() {
		return result.value().download();
	}
	
	@Override
	public final void stop() throws Exception {
		download().stop();
		result.cancel();
	}
	
	@Override
	public final void pause() throws Exception {
		download().pause();
	}
	
	@Override
	public final void resume() throws Exception {
		download().resume();
	}
	
	@Override
	public final boolean isRunning() {
		return download().isRunning();
	}
	
	@Override
	public final boolean isStarted() {
		return download().isStarted();
	}
	
	@Override
	public final boolean isDone() {
		return download().isDone();
	}
	
	@Override
	public final boolean isPaused() {
		return download().isPaused();
	}
	
	@Override
	public final boolean isStopped() {
		return download().isStopped();
	}
}