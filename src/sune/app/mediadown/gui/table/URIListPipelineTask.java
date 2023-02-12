package sune.app.mediadown.gui.table;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sune.app.mediadown.MediaGetter;
import sune.app.mediadown.MediaGetters;
import sune.app.mediadown.concurrent.ListTask;
import sune.app.mediadown.gui.Window;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.resource.cache.GlobalCache;
import sune.app.mediadown.util.Pair;

/** @since 00.02.07 */
public final class URIListPipelineTask
		extends ProgressPipelineTaskBase<Pair<MediaGetter, List<Media>>, URIListPipelineResult, Window<?>> {
	
	private final List<URI> uris;
	private final List<URI> errors = new ArrayList<>();
	
	public URIListPipelineTask(Window<?> window, List<URI> uris) {
		super(window);
		this.uris = uris;
	}
	
	@Override
	protected final ListTask<Pair<MediaGetter, List<Media>>> getTask() {
		return ListTask.of((task) -> {
			errors.clear();
			
			for(URI uri : uris) {
				MediaGetter getter = MediaGetters.fromURI(uri);
				
				if(getter != null) {
					List<Media> list = GlobalCache.ofURIs().getChecked(uri, () -> {
						ListTask<Media> t = getter.getMedia(uri, Map.of());
						t.startAndWait();
						return t.list();
					});
					
					if(!task.add(new Pair<>(getter, list))) {
						break;
					}
				} else {
					errors.add(uri);
				}
			}
		});
	}
	
	@Override
	protected final URIListPipelineResult getResult(Window<?> window, List<Pair<MediaGetter, List<Media>>> result) {
		List<ResolvedMedia> resultMedia = TablePipelineUtils.resolveMediaMultiple(window, result);
		return new URIListPipelineResult(resultMedia);
	}
	
	@Override
	protected String getProgressText(Window<?> window) {
		return window.getTranslation().getSingle("progress.media");
	}
	
	public List<URI> errors() {
		return errors;
	}
}