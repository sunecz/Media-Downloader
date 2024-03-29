package sune.app.mediadown.gui.table;

import java.util.List;

import javafx.scene.control.TableView;
import sune.app.mediadown.entity.Episode;
import sune.app.mediadown.entity.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.task.ListTask;
import sune.app.mediadown.task.Tasks;
import sune.app.mediadown.util.Pair;

/** @since 00.01.27 */
public final class MediaPipelineTask extends MediaEnginePipelineTaskBase<Media, Pair<Episode, Media>> {
	
	private final Episode episode;
	
	public MediaPipelineTask(TableWindow window, MediaEngine engine, Episode episode, List<Media> items) {
		super(window, engine, items);
		this.episode = episode;
	}
	
	@Override
	protected final ListTask<Pair<Episode, Media>> getFunction(Media item, MediaEngine engine) {
		return Tasks.listOfOne(() -> new Pair<>(episode, item));
	}
	
	@Override
	protected final ResolvedMediaPipelineResult getResult(TableWindow window, MediaEngine engine, List<Pair<Episode, Media>> result) {
		List<ResolvedMedia> resultResolved = TablePipelineUtils.resolveMedia(window, result, (p) -> p.a.title());
		return new ResolvedMediaPipelineResult(window, resultResolved);
	}
	
	@Override
	protected String getProgressText(TableWindow window) {
		return window.getTranslation().getSingle("progress.media");
	}
	
	@Override
	public final TableView<Pair<Episode, Media>> getTable(TableWindow window) {
		// Notify the handler that nothing should be changed
		return null;
	}
	
	@Override
	public final String getTitle(TableWindow window) {
		// Notify the handler that nothing should be changed
		return null;
	}
	
	@Override
	public final boolean filter(Pair<Episode, Media> item, String text) {
		// Do not filter anything
		return true;
	}
}