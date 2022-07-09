package sune.app.mediadown.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import sune.app.mediadown.util.Cache;
import sune.app.mediadown.util.NoNullCache;

/** @since 00.02.07 */
public final class GlobalCache {
	
	private static final String NAME_PROGRAMS = "program";
	private static final String NAME_EPISODES = "episode";
	private static final String NAME_MEDIA    = "media";
	private static final String NAME_URIS     = "uri";
	
	private static final Map<String, Cache> caches = new HashMap<>();
	
	// Forbid anyone to create an instance of this class
	private GlobalCache() {
	}
	
	public static final Cache of(String name) {
		return caches.computeIfAbsent(Objects.requireNonNull(name), (k) -> new NoNullCache());
	}
	
	public static final Cache ofPrograms() {
		return of(NAME_PROGRAMS);
	}
	
	public static final Cache ofEpisodes() {
		return of(NAME_EPISODES);
	}
	
	public static final Cache ofMedia() {
		return of(NAME_MEDIA);
	}
	
	public static final Cache ofURIs() {
		return of(NAME_URIS);
	}
	
	public static final void clear(String name) {
		Objects.requireNonNull(name);
		Cache cache = caches.remove(name);
		if(cache != null) cache.clear();
	}
	
	public static final void clearAll() {
		caches.values().forEach(Cache::clear);
		caches.clear();
	}
}