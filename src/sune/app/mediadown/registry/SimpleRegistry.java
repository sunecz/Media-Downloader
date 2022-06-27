package sune.app.mediadown.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SimpleRegistry<T> implements Registry<T> {
	
	private final List<T> values = new ArrayList<>();
	
	@Override
	public void register(T value) {
		values.add(value);
	}
	
	@Override
	public void unregister(T value) {
		values.remove(value);
	}
	
	@Override
	public List<T> all() {
		return Collections.unmodifiableList(values);
	}
	
	@Override
	public Iterator<T> iterator() {
		return new SimpleRegistryItr();
	}
	
	private final class SimpleRegistryItr implements Iterator<T> {
		
		private final Iterator<T> itr = values.iterator();
		
		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}
		
		@Override
		public T next() {
			return itr.next();
		}
	}
}