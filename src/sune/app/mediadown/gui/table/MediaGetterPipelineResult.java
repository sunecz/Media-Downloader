package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.entity.MediaGetter;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.pipeline.Pipeline;

/** @since 00.01.27 */
public final class MediaGetterPipelineResult implements TablePipelineResult<Media, ResolvedMediaPipelineResult> {
	
	private final TableWindow window;
	private final MediaGetter getter;
	private final List<Media> media;
	
	public MediaGetterPipelineResult(TableWindow window, MediaGetter getter, List<Media> media) {
		this.window = window;
		this.getter = getter;
		this.media = media;
	}
	
	@Override
	public final MediaOnlyPipelineTask process(Pipeline pipeline) throws Exception {
		return new MediaOnlyPipelineTask(window, getter, window.waitAndGetSelection(media));
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