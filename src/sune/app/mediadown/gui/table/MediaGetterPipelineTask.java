package sune.app.mediadown.gui.table;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javafx.scene.control.TableView;
import sune.app.mediadown.MediaGetter;
import sune.app.mediadown.concurrent.ListTask;
import sune.app.mediadown.concurrent.ListTask.ListTaskEvent;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.resource.cache.Cache;
import sune.app.mediadown.resource.cache.GlobalCache;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.01.27 */
public final class MediaGetterPipelineTask extends TableWindowPipelineTaskBase<Media, MediaGetterPipelineResult> {
	
	private final MediaGetter getter;
	/** @since 00.02.07 */
	private final URI uri;
	
	public MediaGetterPipelineTask(TableWindow window, MediaGetter getter, URI uri) {
		super(window);
		this.getter = getter;
		this.uri = uri;
	}
	
	@Override
	protected final ListTask<Media> getTask() {
		return ListTask.of((task) -> {
			// TODO: Can be abstracted
			Cache cache = GlobalCache.ofURIs();
			URI key = uri;
			
			if(cache.has(key)) {
				task.add(cache.getChecked(key));
			} else {
				cache.setChecked(key, () -> {
					ListTask<Media> t = getter._getMedia(key, Map.of());
					t.addEventListener(ListTaskEvent.ITEM_ADDED, (p) -> Ignore.callVoid(() -> task.add(Utils.cast(p.b))));
					t.startAndWait();
					return t.list();
				});
			}
		});
	}
	
	@Override
	protected final MediaGetterPipelineResult getResult(TableWindow window, List<Media> result) {
		return new MediaGetterPipelineResult(window, getter, result);
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
		GlobalCache.ofURIs().remove(uri);
	}
	
	/** @since 00.02.07 */
	@Override
	protected void onCancelled() throws Exception {
		beforeReload();
	}
}