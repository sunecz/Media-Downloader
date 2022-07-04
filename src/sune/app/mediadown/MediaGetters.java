package sune.app.mediadown;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.engine.MediaEngines;
import sune.app.mediadown.server.Server;
import sune.app.mediadown.server.Servers;

/** @since 00.01.27 */
public final class MediaGetters {
	
	// Forbid anyone to create an instance of this class
	private MediaGetters() {
	}
	
	/** @since 00.02.07 */
	public static final MediaGetter fromURI(URI uri) {
		return fromURL(Objects.requireNonNull(uri).toString());
	}
	
	public static final MediaGetter fromURL(String url) {
		Server server = Servers.getServerFromURL(url);
		if((server != null)) return server;
		MediaEngine engine = MediaEngines.getMediaEngineFromURL(url);
		if((engine != null)) return engine;
		return null;
	}
	
	public static final Collection<MediaGetter> all() {
		List<MediaGetter> getters = new ArrayList<>();
		Collection<Server> servers = Servers.all();
		Collection<MediaEngine> engines = MediaEngines.all().stream()
				.filter(MediaEngine::isDirectMediaSupported)
				.collect(Collectors.toList());
		getters.addAll(servers);
		getters.addAll(engines);
		return getters;
	}
}