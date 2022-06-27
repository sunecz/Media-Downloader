package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.Episode;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.pipeline.Pipeline;

/** @since 00.01.27 */
public final class EpisodePipelineResult implements TablePipelineResult<Media, ResolvedMediaPipelineResult> {
	
	private final TableWindow window;
	private final MediaEngine engine;
	private final Episode episode;
	private final List<Media> media;
	
	public EpisodePipelineResult(TableWindow window, MediaEngine engine, Episode episode, List<Media> media) {
		this.window = window;
		this.engine = engine;
		this.episode = episode;
		this.media = media;
	}
	
	@Override
	public final MediaPipelineTask process(Pipeline pipeline) throws Exception {
		return new MediaPipelineTask(window, engine, episode, window.waitAndGetSelection(media));
	}
	
	@Override
	public final boolean isTerminating() {
		return false;
	}
	
	@Override
	public final List<Media> getValue() {
		return media;
	}
}