package sune.app.mediadown.convert;

import java.nio.file.Path;
import java.util.Objects;

import sune.app.mediadown.media.Media;

/** @since 00.02.08 */
public final class ConversionMedia {
	
	private final Media media;
	private final Path path;
	private final double duration;
	
	public ConversionMedia(Media media, Path path, double duration) {
		this.media = Objects.requireNonNull(media);
		this.path = Objects.requireNonNull(path).toAbsolutePath();
		this.duration = duration;
	}
	
	public Media media() {
		return media;
	}
	
	public Path path() {
		return path;
	}
	
	public double duration() {
		return duration;
	}
}