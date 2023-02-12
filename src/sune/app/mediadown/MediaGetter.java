package sune.app.mediadown;

import java.net.URI;
import java.util.Map;

import javafx.scene.image.Image;
import sune.app.mediadown.concurrent.ListTask;
import sune.app.mediadown.media.Media;

/** @since 00.02.05 */
public interface MediaGetter {
	
	/** @since 00.02.08 */
	ListTask<Media> getMedia(URI uri, Map<String, Object> data) throws Exception;
	/** @since 00.02.08 */
	boolean isCompatibleURI(URI uri);
	
	default boolean isDirectMediaSupported() {
		// Just return false, this method is not needed to be implemented
		return false;
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