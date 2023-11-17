package sune.app.mediadown.pipeline;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

import sune.app.mediadown.concurrent.PositionAwareQueueTaskExecutor.PositionAwareQueueTaskResult;
import sune.app.mediadown.download.Download;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.DownloadResult;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.QueueEvent;
import sune.app.mediadown.event.tracker.Trackable;
import sune.app.mediadown.event.tracker.TrackerEvent;
import sune.app.mediadown.manager.DownloadManager;
import sune.app.mediadown.manager.PositionAwareManagerSubmitResult;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaDownloadContext;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.QueueContext;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.01.26 */
public final class DownloadPipelineTask implements PipelineTask, MediaDownloadContext {
	
	/** @since 00.02.08 */
	private final PipelineMedia media;
	
	private PositionAwareManagerSubmitResult<DownloadResult, Long> result;
	
	private DownloadPipelineTask(PipelineMedia media) {
		this.media = Objects.requireNonNull(media);
	}
	
	/** @since 00.02.09 */
	private static final void callTrackerEventUpdate(EventRegistry<EventType> eventRegistry, Trackable trackable) {
		eventRegistry.call(TrackerEvent.UPDATE, trackable.trackerManager().tracker());
	}
	
	/** @since 00.02.08 */
	public static final DownloadPipelineTask of(PipelineMedia media) {
		return new DownloadPipelineTask(media);
	}
	
	/** @since 00.02.09 */
	private final void bindAllDownloadEvents(EventRegistry<EventType> eventRegistry, Download download) {
		for(Event<DownloadEvent, ?> event : DownloadEvent.values()) {
			download.addEventListener(event, (ctx) -> callTrackerEventUpdate(eventRegistry, (Trackable) ctx));
		}
	}
	
	/** @since 00.02.09 */
	private final <T> T downloadAction(Function<Download, T> action, T defaultValue) {
		DownloadResult downloadResult;
		Download download;
		
		// Check the chain of values to avoid NPE
		if(result == null
				|| (downloadResult = result.value()) == null
				|| (download = downloadResult.download()) == null) {
			return defaultValue;
		}
		
		return action.apply(download);
	}
	
	/** @since 00.02.09 */
	private final void downloadAction(CheckedConsumer<Download> action) throws Exception {
		DownloadResult downloadResult;
		Download download;
		
		// Check the chain of values to avoid NPE
		if(result == null
				|| (downloadResult = result.value()) == null
				|| (download = downloadResult.download()) == null) {
			return;
		}
		
		action.accept(download);
	}
	
	@Override
	public DownloadPipelineResult run(Pipeline pipeline) throws Exception {
		result = DownloadManager.instance().submit(
			media.media(), media.destination(), media.mediaConfiguration(), media.configuration()
		);
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
		bindAllDownloadEvents(eventRegistry, download);
		
		Ignore.Cancellation.call(result::get); // Wait for the download to finish
		return Utils.cast(downloadResult.pipelineResult());
	}
	
	@Override
	public void stop() throws Exception {
		downloadAction(Download::stop);
		
		if(result != null) {
			result.cancel();
		}
	}
	
	@Override
	public void pause() throws Exception {
		downloadAction(Download::pause);
	}
	
	@Override
	public void resume() throws Exception {
		downloadAction(Download::resume);
	}
	
	@Override
	public boolean isRunning() {
		return downloadAction(Download::isRunning, false);
	}
	
	@Override
	public boolean isStarted() {
		return downloadAction(Download::isStarted, false);
	}
	
	@Override
	public boolean isDone() {
		return downloadAction(Download::isDone, false);
	}
	
	@Override
	public boolean isPaused() {
		return downloadAction(Download::isPaused, false);
	}
	
	@Override
	public boolean isStopped() {
		return downloadAction(Download::isStopped, false);
	}
	
	@Override
	public boolean isError() {
		return downloadAction(Download::isError, false);
	}
	
	/** @since 00.02.09 */
	@Override
	public Media media() {
		return media.media();
	}
	
	/** @since 00.02.09 */
	@Override
	public Path destination() {
		return media.destination();
	}
	
	/** @since 00.02.09 */
	@Override
	public MediaDownloadConfiguration mediaConfiguration() {
		return media.mediaConfiguration();
	}
	
	/** @since 00.02.09 */
	@Override
	public DownloadConfiguration configuration() {
		return media.configuration();
	}
}