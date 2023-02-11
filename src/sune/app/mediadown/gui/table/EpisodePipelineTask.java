package sune.app.mediadown.gui.table;

import java.util.List;

import javafx.scene.control.TableView;
import sune.app.mediadown.Episode;
import sune.app.mediadown.concurrent.ListTask;
import sune.app.mediadown.concurrent.ListTask.ListTaskEvent;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.resource.cache.Cache;
import sune.app.mediadown.resource.cache.GlobalCache;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.01.27 */
public final class EpisodePipelineTask extends MediaEnginePipelineTaskBase<Episode, Media, EpisodePipelineResult> {
	
	public EpisodePipelineTask(TableWindow window, MediaEngine engine, List<Episode> items) {
		super(window, engine, items);
	}
	
	@Override
	protected final ListTask<Media> getFunction(Episode item, MediaEngine engine) {
		return ListTask.of((task) -> {
			// TODO: Can be abstracted
			Cache cache = GlobalCache.ofMedia();
			Episode key = item;
			
			if(cache.has(key)) {
				task.add(cache.getChecked(key));
			} else {
				cache.setChecked(key, () -> {
					ListTask<Media> t = engine._getMedia(item);
					t.addEventListener(ListTaskEvent.ITEM_ADDED, (p) -> Ignore.callVoid(() -> task.add(Utils.cast(p.b))));
					t.startAndWait();
					return t.list();
				});
			}
		});
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