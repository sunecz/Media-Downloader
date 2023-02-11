package sune.app.mediadown.gui.table;

import java.util.List;

import javafx.scene.control.TableView;
import sune.app.mediadown.Episode;
import sune.app.mediadown.concurrent.WorkerProxy;
import sune.app.mediadown.concurrent.WorkerUpdatableTask;
import sune.app.mediadown.concurrent.WorkerUpdatableTaskUtils;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.resource.cache.GlobalCache;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.Utils;

/** @since 00.01.27 */
public final class EpisodePipelineTask extends MediaEnginePipelineTaskBase<Episode, Media, EpisodePipelineResult> {
	
	public EpisodePipelineTask(TableWindow window, MediaEngine engine, List<Episode> items) {
		super(window, engine, items);
	}
	
	@Override
	protected final CheckedBiFunction<Episode, CheckedBiFunction<WorkerProxy, Media, Boolean>,
			WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Media, Boolean>, Void>> getFunction(MediaEngine engine) {
		return ((episode, f) -> WorkerUpdatableTaskUtils.cachedListBiTask(GlobalCache.ofMedia(), episode,
		                                                                  f, engine::getMedia, episode));
	}
	
	@Override
	protected final EpisodePipelineResult getResult(TableWindow window, MediaEngine engine, List<Media> result) {
		return new EpisodePipelineResult(window, engine, items.get(0), result);
	}
	
	@Override
	protected String getProgressText(TableWindow window) {
		return window.getTranslation().getSingle("progress.media");
	}
	
	@Override
	public final TableView<Media> getTable(TableWindow window) {
		return TablePipelineUtils.newMediaTable(window);
	}
	
	@Override
	public final String getTitle(TableWindow window) {
		return window.getTranslation().getSingle("tables.media.title");
	}
	
	@Override
	public final boolean filter(Media item, String text) {
		return Utils.normalize(item.metadata().get("title", "")).toLowerCase().contains(text);
	}
	
	/** @since 00.02.07 */
	public boolean canReload() {
		return true;
	}
	
	/** @since 00.02.07 */
	@Override
	public void beforeReload() {
		items.forEach(GlobalCache.ofMedia()::remove);
	}
	
	/** @since 00.02.07 */
	@Override
	protected void onCancelled() throws Exception {
		beforeReload();
	}
}