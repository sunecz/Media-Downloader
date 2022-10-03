package sune.app.mediadown;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javafx.scene.image.Image;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.WorkerProxy;
import sune.app.mediadown.util.WorkerUpdatableTask;

/** @since 00.02.05 */
public interface MediaGetter {
	
	List<Media> getMedia(URI uri, Map<String, Object> data) throws Exception;
	
	default List<Media> getMedia(URI uri) throws Exception {
		return getMedia(uri, Map.of());
	}
	
	default WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Media, Boolean>, Void> getMedia
			(URI uri, CheckedBiFunction<WorkerProxy, Media, Boolean> function) {
		return getMedia(uri, Map.of(), function);
	}
	
	default WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Media, Boolean>, Void> getMedia
			(URI uri, Map<String, Object> data, CheckedBiFunction<WorkerProxy, Media, Boolean> function) {
		return WorkerUpdatableTask.listVoidTaskChecked(function, () -> getMedia(uri, data));
	}
	
	default boolean isDirectMediaSupported() {
		// Just return false, this method is not needed to be implemented
		return false;
	}
	
	default boolean isCompatibleURL(String url) {
		// Just return false, this method is not needed to be implemented
		return false;
	}
	
	@Deprecated(forRemoval=true)
	default DownloadConfiguration getDownloadConfiguration() {
		// Just return the default configuration, this method is not needed to be implemented
		return DownloadConfiguration.ofDefault();
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