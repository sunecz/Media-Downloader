package sune.app.mediadown.engine;

import java.net.URI;
import java.util.Collection;

import sune.app.mediadown.util.ObjectHolder;

public final class MediaEngines {
	
	private static final ObjectHolder<String, MediaEngine> holder = new ObjectHolder<>();
	
	// Forbid anyone to create an instance of this class
	private MediaEngines() {
	}
	
	public static final void add(String name, Class<? extends MediaEngine> clazz) { holder.add(name, clazz); }
	public static final MediaEngine get(String name) { return holder.get(name); }
	public static final Collection<MediaEngine> all() { return holder.all(); }
	
	public static final MediaEngine fromURI(URI uri) {
		return holder.stream()
					.filter((o) -> o.isCompatibleURI(uri))
					.findFirst().orElse(null);
	}
}