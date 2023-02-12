package sune.app.mediadown.entity;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** @since 00.01.27 */
public final class MediaGetters {
	
	// Forbid anyone to create an instance of this class
	private MediaGetters() {
	}
	
	/** @since 00.02.07 */
	public static final MediaGetter fromURI(URI uri) {
		Server server = Servers.fromURI(uri);
		if(server != null) {
			return server;
		}
		
		MediaEngine engine = MediaEngines.fromURI(uri);
		if(engine != null) {
			return engine;
		}
		
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