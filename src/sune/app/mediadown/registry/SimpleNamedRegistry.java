package sune.app.mediadown.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SimpleNamedRegistry<T> implements NamedRegistry<T> {
	
	private final Map<String, T> values = new LinkedHashMap<>();
	
	@Override
	public void register(String name, T value) {
		if((values.containsKey(name)))
			return;
		values.put(name, value);
	}
	
	@Override
	public void unregister(String name) {
		values.remove(name);
	}
	
	@Override
	public T get(String name) {
		return values.get(name);
	}
	
	@Override
	public void clear() {
		values.clear();
	}
	
	@Override
	public int size() {
		return values.size();
	}
	
	@Override
	public boolean isEmpty() {
		return values.isEmpty();
	}
	
	@Override
	public Map<String, T> all() {
		return Collections.unmodifiableMap(values);
	}
	
	@Override
	public Collection<T> allValues() {
		return Collections.unmodifiableCollection(values.values());
	}
	
	@Override
	public Set<String> allNames() {
		return Collections.unmodifiableSet(values.keySet());
	}
	
	@Override
	public Iterator<Entry<String, T>> iterator() {
		return new SimpleNamedRegistryItr();
	}
	
	private final class SimpleNamedRegistryItr implements Iterator<Entry<String, T>> {
		
		private final Iterator<Entry<String, T>> itr = values.entrySet().iterator();
		
		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}
		
		@Override
		public Entry<String, T> next() {
			return itr.next();
		}
	}
}