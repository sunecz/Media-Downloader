package sune.app.mediadown.gui.table;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javafx.scene.control.TableView;
import sune.app.mediadown.MediaGetter;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.resource.cache.GlobalCache;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.CheckedFunction;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.WorkerProxy;
import sune.app.mediadown.util.WorkerUpdatableTask;
import sune.app.mediadown.util.WorkerUpdatableTaskUtils;

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
	protected final CheckedFunction<CheckedBiFunction<WorkerProxy, Media, Boolean>,
			WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Media, Boolean>, Void>> getTask() {
		return ((f) -> WorkerUpdatableTaskUtils.cachedListBiTask(GlobalCache.ofURIs(), uri, f,
		                                                         (u, a) -> getter.getMedia(u, Map.of(), a),
		                                                         uri));
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