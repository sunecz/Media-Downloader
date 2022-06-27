package sune.app.mediadown.download;

import java.util.Collection;
import java.util.Objects;

import sune.app.mediadown.media.Media;
import sune.app.mediadown.util.ObjectHolder;

public final class Downloaders {
	
	private static final ObjectHolder<String, Downloader> holder = new ObjectHolder<>();
	
	// Forbid anyone to create an instance of this class
	private Downloaders() {
	}
	
	public static final void add(String name, Class<? extends Downloader> clazz) { holder.add(name, clazz); }
	public static final Downloader get(String name) { return holder.get(name); }
	public static final Collection<Downloader> all() { return holder.all(); }
	
	public static final Downloader forMedia(Media media) {
		Objects.requireNonNull(media);
		return holder.stream()
				.filter((d) -> d.isDownloadable(media))
				.findFirst().orElse(null);
	}
}