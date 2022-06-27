package sune.app.mediadown.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ObjectHolder<K, V> implements Iterable<V> {
	
	private final Map<K, V> objects = new LinkedHashMap<>();
	private final Instantiator<V> instantiator = new Instantiator<>();
	
	public void add(K name, Class<? extends V> clazz) {
		if((objects.containsKey(name))) return; // Do not replace objects
		V object = Utils.ignore(() -> instantiator.newInstance(clazz));
		if((object == null))
			Utils.throwISE("Unable to create an instance of class '%s'", clazz.getCanonicalName());
		objects.put(name, object);
	}
	
	public V get(K name) {
		return objects.get(name);
	}
	
	public Collection<V> all() {
		return Collections.unmodifiableCollection(objects.values());
	}
	
	public Collection<K> allNames() {
		return Collections.unmodifiableCollection(objects.keySet());
	}
	
	@Override
	public Iterator<V> iterator() {
		return objects.values().iterator();
	}
	
	public Stream<V> stream() {
		return objects.values().stream();
	}
}