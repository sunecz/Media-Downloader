package sune.app.mediadown.download;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import sune.app.mediadown.media.Media;
import sune.app.mediadown.media.MediaFormat;
import sune.app.mediadown.media.MediaType;

/** @since 00.02.05 */
public class MediaDownloadConfiguration {
	
	private static MediaDownloadConfiguration DEFAULT;
	
	/** @since 00.02.08 */
	private final MediaFormat outputFormat;
	private final Map<MediaType, List<Media>> selectedMedia;
	
	/** @since 00.02.08 */
	private MediaDownloadConfiguration(MediaFormat outputFormat, Map<MediaType, List<Media>> selectedMedia) {
		this.outputFormat = Objects.requireNonNull(outputFormat);
		this.selectedMedia = Collections.unmodifiableMap(Objects.requireNonNull(selectedMedia));
	}
	
	public static final MediaDownloadConfiguration ofDefault() {
		return DEFAULT == null ? (DEFAULT = new MediaDownloadConfiguration(MediaFormat.UNKNOWN, Map.of())) : DEFAULT;
	}
	
	/** @since 00.02.08 */
	public static final MediaDownloadConfiguration of(MediaFormat outputFormat) {
		return of(outputFormat, Map.of());
	}
	
	/** @since 00.02.08 */
	public static final MediaDownloadConfiguration of(MediaFormat outputFormat,
			Map<MediaType, List<Media>> selectedMedia) {
		return new MediaDownloadConfiguration(outputFormat, selectedMedia);
	}
	
	/** @since 00.02.08 */
	public MediaFormat outputFormat() {
		return outputFormat;
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