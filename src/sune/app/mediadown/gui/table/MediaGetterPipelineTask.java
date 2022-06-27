package sune.app.mediadown.gui.table;

import java.util.List;
import java.util.Map;

import javafx.scene.control.TableView;
import sune.app.mediadown.MediaGetter;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.CheckedFunction;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.WorkerProxy;
import sune.app.mediadown.util.WorkerUpdatableTask;

/** @since 00.01.27 */
public final class MediaGetterPipelineTask extends TableWindowPipelineTaskBase<Media, MediaGetterPipelineResult> {
	
	private final MediaGetter getter;
	private final String url;
	
	public MediaGetterPipelineTask(TableWindow window, MediaGetter getter, String url) {
		super(window);
		this.getter = getter;
		this.url = url;
	}
	
	@Override
	protected final CheckedFunction<CheckedBiFunction<WorkerProxy, Media, Boolean>,
			WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Media, Boolean>, Void>> getTask() {
		return ((function) -> getter.getMedia(Utils.uri(url), Map.of(), function));
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
}