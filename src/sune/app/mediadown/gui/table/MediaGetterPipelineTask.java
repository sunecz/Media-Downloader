package sune.app.mediadown.gui.table;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javafx.scene.control.TableView;
import sune.app.mediadown.concurrent.ListTask;
import sune.app.mediadown.concurrent.Tasks;
import sune.app.mediadown.entity.MediaGetter;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.resource.cache.GlobalCache;
import sune.app.mediadown.util.Utils;

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
		return Tasks.cachedList(GlobalCache::ofURIs, uri, (k) -> getter.getMedia(k, Map.of()));
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