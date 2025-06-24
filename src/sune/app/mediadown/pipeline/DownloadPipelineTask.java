package sune.app.mediadown.pipeline;

import java.nio.file.Path;
import java.util.Objects;

import sune.app.mediadown.download.Download;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.DownloadInitialState;
import sune.app.mediadown.download.DownloadResult;
import sune.app.mediadown.download.DownloadState;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.manager.DownloadManager;
import sune.app.mediadown.manager.ManagerSubmitResult;
import sune.app.mediadown.manager.PositionAwareManagerSubmitResult;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaDownloadContext;
import sune.app.mediadown.pipeline.state.Metrics;
import sune.app.mediadown.pipeline.state.PipelineState;
import sune.app.mediadown.pipeline.state.PipelineStates;
import sune.app.mediadown.util.JSON.JSONCollection;
import sune.app.mediadown.util.JSON.JSONObject;
import sune.app.mediadown.util.Utils;

/** @since 00.01.26 */
public final class DownloadPipelineTask
		extends ManagerPipelineTask<DownloadResult, Long>
		implements MediaDownloadContext {
	
	/** @since 00.02.08 */
	private final PipelineMedia media;
	/** @since 00.02.09 */
	private final DownloadInitialState initialState;
	/** @since 00.02.09 */
	private final DownloadPipelineState state;
	
	private DownloadPipelineTask(PipelineMedia media, DownloadInitialState initialState) {
		this.media = Objects.requireNonNull(media);
		this.initialState = initialState;
		this.state = new DownloadPipelineState();
	}
	
	/** @since 00.02.08 */
	public static final DownloadPipelineTask of(PipelineMedia media) {
		return new DownloadPipelineTask(media, null);
	}
	
	/** @since 00.02.09 */
	public static final DownloadPipelineTask of(PipelineMedia media, DownloadInitialState initialState) {
		return new DownloadPipelineTask(media, initialState);
	}
	
	@Override
	protected PositionAwareManagerSubmitResult<DownloadResult, Long> submit(Pipeline pipeline) throws Exception {
		PositionAwareManagerSubmitResult<DownloadResult, Long> result = DownloadManager.instance().submit(
			media.media(),
			media.destination(),
			media.mediaConfiguration(),
			media.configuration(),
			initialState
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
	
	@Override protected void doStop() throws Exception { doActionCast(DownloadResult::download, Download::stop); }
	@Override protected void doPause() throws Exception { doActionCast(DownloadResult::download, Download::pause); }
	@Override protected void doResume() throws Exception { doActionCast(DownloadResult::download, Download::resume); }
	
	@Override public boolean isRunning() { return doActionCast(DownloadResult::download, Download::isRunning, false); }
	@Override public boolean isStarted() { return doActionCast(DownloadResult::download, Download::isStarted, false); }
	@Override public boolean isDone() { return doActionCast(DownloadResult::download, Download::isDone, false); }
	@Override public boolean isPaused() { return doActionCast(DownloadResult::download, Download::isPaused, false); }
	@Override public boolean isStopped() { return doActionCast(DownloadResult::download, Download::isStopped, false); }
	@Override public boolean isError() { return doActionCast(DownloadResult::download, Download::isError, false); }
	
	/** @since 00.02.09 */
	@Override public Media media() { return media.media(); }
	/** @since 00.02.09 */
	@Override public Path destination() { return media.destination(); }
	/** @since 00.02.09 */
	@Override public MediaDownloadConfiguration mediaConfiguration() { return media.mediaConfiguration(); }
	/** @since 00.02.09 */
	@Override public DownloadConfiguration configuration() { return media.configuration(); }
	
	/** @since 00.02.09 */
	@Override
	public PipelineState state() {
		return state;
	}
	
	/** @since 00.02.09 */
	private final class DownloadPipelineState implements PipelineState {
		
		private static final String TYPE = "download";
		private final JSONCollection serialized;
		
		private DownloadPipelineState() {
			serialized = JSONCollection.ofObject(
				"type", JSONObject.ofString(TYPE),
				"input", PipelineStates.Serialization.serialize(media)
			);
		}
		
		private final DownloadState downloadState() {
			ManagerSubmitResult<DownloadResult, Long> result = result();
			return result == null ? null : result.value().download().state();
		}
		
		@Override
		public Metrics metrics() {
			DownloadState state = downloadState();
			return state == null ? null : state.metrics();
		}
		
		@Override
		public JSONCollection serialize() {
			DownloadState state = downloadState();
			JSONCollection copy = serialized.deepCopy();
			copy.set("state", state == null ? JSONObject.ofNull() : state.serialize());
			return copy;
		}
	}
}