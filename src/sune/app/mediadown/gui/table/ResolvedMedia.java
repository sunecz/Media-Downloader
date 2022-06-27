package sune.app.mediadown.gui.table;

import java.nio.file.Path;
import java.util.Objects;

import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.media.Media;

/** @since 00.01.27 */
public final class ResolvedMedia {
	
	private final Media media;
	private final Path path;
	/** @since 00.02.05 */
	private final MediaDownloadConfiguration configuration;
	
	public ResolvedMedia(Media media, Path path, MediaDownloadConfiguration configuration) {
		this.media = Objects.requireNonNull(media);
		this.path = Objects.requireNonNull(path).toAbsolutePath();
		this.configuration = Objects.requireNonNull(configuration);
	}
	
	/** @since 00.02.05 */
	public final Media media() {
		return media;
	}
	
	/** @since 00.02.05 */
	public final Path path() {
		return path;
	}
	
	/** @since 00.02.05 */
	public final MediaDownloadConfiguration configuration() {
		return configuration;
	}
}