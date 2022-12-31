package sune.app.mediadown.search;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import sune.app.mediadown.util.Instantiator;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.01.17 */
public final class SearchEngines {
	
	private static final Map<String, SearchEngine> engines = new LinkedHashMap<>();
	private static final Instantiator<SearchEngine> instantiator = new Instantiator<>();
	
	public static final void add(String name, Class<? extends SearchEngine> clazz) {
		if(!engines.containsKey(name)) {
			SearchEngine engine = Ignore.call(() -> instantiator.newInstance(clazz));
			if((engine == null))
				throw new IllegalStateException("Unable to create an instance of search engine: " + clazz);
			engines.put(name, engine);
		}
	}
	
	public static final SearchEngine get(String name) {
		return engines.get(name);
	}
	
	public static final Collection<SearchEngine> all() {
		return Collections.unmodifiableCollection(engines.values());
	}
	
	// forbid anyone to create an instance of this class
	private SearchEngines() {
	}
}