package sune.app.mediadown.download;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaType;

/** @since 00.02.05 */
public class MediaDownloadConfiguration {
	
	private static MediaDownloadConfiguration DEFAULT;
	
	private final Map<MediaType, List<Media>> selectedMedia;
	
	private MediaDownloadConfiguration(Map<MediaType, List<Media>> selectedMedia) {
		this.selectedMedia = Collections.unmodifiableMap(Objects.requireNonNull(selectedMedia));
	}
	
	public static final MediaDownloadConfiguration ofDefault() {
		return DEFAULT == null ? (DEFAULT = new MediaDownloadConfiguration(Map.of())) : DEFAULT;
	}
	
	public static final MediaDownloadConfiguration of(Map<MediaType, List<Media>> selectedMedia) {
		return new MediaDownloadConfiguration(selectedMedia);
	}
	
	public Map<MediaType, List<Media>> selectedMedia() {
		return selectedMedia;
	}
	
	public <T extends Media> List<T> selectedMedia(MediaType type) {
		@SuppressWarnings("unchecked")
		List<T> list = (List<T>) Optional.ofNullable(selectedMedia.get(type)).orElseGet(List::of);
		return list;
	}
}