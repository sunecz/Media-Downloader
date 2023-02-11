package sune.app.mediadown;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javafx.scene.image.Image;
import sune.app.mediadown.concurrent.ListTask;
import sune.app.mediadown.concurrent.WorkerProxy;
import sune.app.mediadown.concurrent.WorkerUpdatableTask;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.util.CheckedBiFunction;

/** @since 00.02.05 */
public interface MediaGetter {
	
	@Deprecated
	List<Media> getMedia(URI uri, Map<String, Object> data) throws Exception;
	
	@Deprecated
	default List<Media> getMedia(URI uri) throws Exception {
		return getMedia(uri, Map.of());
	}
	
	@Deprecated
	default WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Media, Boolean>, Void> getMedia
			(URI uri, CheckedBiFunction<WorkerProxy, Media, Boolean> function) {
		return getMedia(uri, Map.of(), function);
	}
	
	@Deprecated
	default WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Media, Boolean>, Void> getMedia
			(URI uri, Map<String, Object> data, CheckedBiFunction<WorkerProxy, Media, Boolean> function) {
		return WorkerUpdatableTask.listVoidTaskChecked(function, () -> getMedia(uri, data));
	}
	
	@Deprecated
	default boolean isDirectMediaSupported() {
		// Just return false, this method is not needed to be implemented
		return false;
	}
	
	@Deprecated
	default boolean isCompatibleURL(String url) {
		// Just return false, this method is not needed to be implemented
		return false;
	}
	
	// TODO: Make non-default
	default ListTask<Media> _getMedia(URI uri, Map<String, Object> data) throws Exception {
		return ListTask.empty();
	}
	
	/** @since 00.02.07 */
	String title();
	/** @since 00.02.07 */
	String url();
	/** @since 00.02.07 */
	String version();
	/** @since 00.02.07 */
	String author();
	/** @since 00.02.07 */
	Image icon();
}