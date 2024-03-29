package sune.app.mediadown.gui.table;

import java.util.List;

import javafx.scene.control.TableView;
import sune.app.mediadown.entity.Episode;
import sune.app.mediadown.entity.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.report.ReportContext;
import sune.app.mediadown.resource.cache.GlobalCache;
import sune.app.mediadown.task.ListTask;
import sune.app.mediadown.task.Tasks;
import sune.app.mediadown.util.Utils;

/** @since 00.01.27 */
public final class EpisodePipelineTask extends MediaEnginePipelineTaskBase<Episode, Media> {
	
	public EpisodePipelineTask(TableWindow window, MediaEngine engine, List<Episode> items) {
		super(window, engine, items);
	}
	
	/** @since 00.02.09 */
	private final ReportContext newReportContext() {
		return ReportContext.ofEpisode(items.get(0)); // Only one episode is always selected
	}
	
	@Override
	protected final ListTask<Media> getFunction(Episode item, MediaEngine engine) {
		return Tasks.cachedList(GlobalCache::ofMedia, item, engine::getMedia);
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
		return TablePipelineUtils.newMediaTable(window, this::newReportContext);
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