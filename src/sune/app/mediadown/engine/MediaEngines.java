package sune.app.mediadown.engine;

import java.util.Collection;

import sune.app.mediadown.util.ObjectHolder;
import sune.app.mediadown.util.Utils;

public final class MediaEngines {
	
	private static final ObjectHolder<String, MediaEngine> holder = new ObjectHolder<>();
	
	// Forbid anyone to create an instance of this class
	private MediaEngines() {
	}
	
	public static final void add(String name, Class<? extends MediaEngine> clazz) { holder.add(name, clazz); }
	public static final MediaEngine get(String name) { return holder.get(name); }
	public static final Collection<MediaEngine> all() { return holder.all(); }
	
	public static final MediaEngine getMediaEngineFromURL(String url) {
		return Utils.isValidURL(url)
					? holder.stream()
						.filter((o) -> o.isCompatibleURL(url))
						.findFirst().orElse(null)
					: null;
	}
}