package sune.app.mediadown.pipeline;

import java.nio.file.Path;
import java.util.Objects;

import sune.app.mediadown.download.Download;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.DownloadResult;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.manager.DownloadManager;
import sune.app.mediadown.manager.PositionAwareManagerSubmitResult;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaDownloadContext;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class DownloadPipelineTask
		extends ManagerPipelineTask<DownloadResult, Long>
		implements MediaDownloadContext {
	
	/** @since 00.02.08 */
	private final PipelineMedia media;
	
	private DownloadPipelineTask(PipelineMedia media) {
		this.media = Objects.requireNonNull(media);
	}
	
	/** @since 00.02.08 */
	public static final DownloadPipelineTask of(PipelineMedia media) {
		return new DownloadPipelineTask(media);
	}
	
	@Override
	protected PositionAwareManagerSubmitResult<DownloadResult, Long> submit(Pipeline pipeline) throws Exception {
		PositionAwareManagerSubmitResult<DownloadResult, Long> result = DownloadManager.instance().submit(
			media.media(), media.destination(), media.mediaConfiguration(), media.configuration()
		);
		
		// Notify the media of being submitted
		media.submit();
		
		return result;
	}
	
	@Override
	protected void bindEvents(Pipeline pipeline) throws Exception {
		bindAllEvents(pipeline.getEventRegistry(), result().value().download(), DownloadEvent::values);
	}
	
	@Override
	protected PipelineResult pipelineResult() throws Exception {
		return Utils.cast(result().value().pipelineResult());
	}
	
	@Override protected void doStop() throws Exception { doAction(DownloadResult::download, Download::stop); }
	@Override protected void doPause() throws Exception { doAction(DownloadResult::download, Download::pause); }
	@Override protected void doResume() throws Exception { doAction(DownloadResult::download, Download::resume); }
	
	@Override public boolean isRunning() { return doAction(DownloadResult::download, Download::isRunning, false); }
	@Override public boolean isStarted() { return doAction(DownloadResult::download, Download::isStarted, false); }
	@Override public boolean isDone() { return doAction(DownloadResult::download, Download::isDone, false); }
	@Override public boolean isPaused() { return doAction(DownloadResult::download, Download::isPaused, false); }
	@Override public boolean isStopped() { return doAction(DownloadResult::download, Download::isStopped, false); }
	@Override public boolean isError() { return doAction(DownloadResult::download, Download::isError, false); }
	
	/** @since 00.02.09 */
	@Override public Media media() { return media.media(); }
	/** @since 00.02.09 */
	@Override public Path destination() { return media.destination(); }
	/** @since 00.02.09 */
	@Override public MediaDownloadConfiguration mediaConfiguration() { return media.mediaConfiguration(); }
	/** @since 00.02.09 */
	@Override public DownloadConfiguration configuration() { return media.configuration(); }
}