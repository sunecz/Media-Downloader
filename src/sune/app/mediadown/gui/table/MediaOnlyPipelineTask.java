package sune.app.mediadown.gui.table;

import java.util.List;

import javafx.scene.control.TableView;
import sune.app.mediadown.MediaGetter;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.WorkerProxy;
import sune.app.mediadown.util.WorkerUpdatableTask;

/** @since 00.01.27 */
public final class MediaOnlyPipelineTask extends MediaEnginePipelineTaskBase<Media, Pair<MediaGetter, Media>, ResolvedMediaPipelineResult> {
	
	private final MediaGetter getter;
	
	public MediaOnlyPipelineTask(TableWindow window, MediaGetter getter, List<Media> items) {
		super(window, null, items);
		this.getter = getter;
	}
	
	@Override
	protected final CheckedBiFunction<Media, CheckedBiFunction<WorkerProxy, Pair<MediaGetter, Media>, Boolean>,
			WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Pair<MediaGetter, Media>, Boolean>, Void>> getFunction(MediaEngine engine) {
		return ((media, function) -> WorkerUpdatableTask.voidTaskChecked(null, (proxy, value) -> {
			function.apply(proxy, new Pair<>(getter, media));
		}));
	}
	
	@Override
	protected final ResolvedMediaPipelineResult getResult(TableWindow window, MediaEngine engine, List<Pair<MediaGetter, Media>> result) {
		List<ResolvedMedia> resultResolved = TablePipelineUtils.resolveMedia(window, result,
			(p) -> Utils.fileNameNoType(Utils.url(p.b.uri()).toExternalForm()));
		return new ResolvedMediaPipelineResult(window, resultResolved);
	}
	
	@Override
	protected String getProgressText(TableWindow window) {
		return window.getTranslation().getSingle("progress.media");
	}
	
	@Override
	public final TableView<Pair<MediaGetter, Media>> getTable(TableWindow window) {
		// Notify the handler that nothing should be changed
		return null;
	}
	
	@Override
	public final String getTitle(TableWindow window) {
		// Notify the handler that nothing should be changed
		return null;
	}
	
	@Override
	public final boolean filter(Pair<MediaGetter, Media> item, String text) {
		// Do not filter anything
		return true;
	}
}