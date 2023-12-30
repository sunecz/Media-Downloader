package sune.app.mediadown.media;

import java.util.List;

/** @since 00.02.05 */
public interface MediaAccessor {
	
	// Recursive media accessor
	MediaAccessor recursive();
	// Direct media accessor
	MediaAccessor direct();
	
	<T extends Media> List<T> allOfType(MediaType type);
	<T extends MediaContainer> List<T> allContainersOfType(MediaType type);
	<T extends Media> T ofType(MediaType type);
	<T extends MediaContainer> T containerOfType(MediaType type);
	
	default <T extends VideoMedia> T video() {
		return ofType(MediaType.VIDEO);
	}
	
	default <T extends AudioMedia> T audio() {
		return ofType(MediaType.AUDIO);
	}
	
	default <T extends SubtitlesMedia> T subtitles() {
		return ofType(MediaType.SUBTITLES);
	}
	
	default VideoMediaContainer videoContainer() {
		return containerOfType(MediaType.VIDEO);
	}
	
	default AudioMediaContainer audioContainer() {
		return containerOfType(MediaType.AUDIO);
	}
	
	List<Media> media();
	
	/** @since 00.02.09 */
	List<Media> allMedia();
}