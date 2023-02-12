package sune.app.mediadown.entity;

import java.util.Map;

import sune.app.mediadown.media.Media;
import sune.app.mediadown.task.ListTask;

/** @since 00.02.05 */
public interface MediaEngine extends MediaGetter {
	
	/** @since 00.02.08 */
	ListTask<Program> getPrograms() throws Exception;
	/** @since 00.02.08 */
	ListTask<Episode> getEpisodes(Program program) throws Exception;
	
	/** @since 00.02.08 */
	default ListTask<Media> getMedia(Episode episode) throws Exception {
		return getMedia(episode.uri(), Map.of());
	}
}