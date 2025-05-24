package sune.app.mediadown.pipeline;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/** @since 00.02.09 */
public interface Metrics {
	
	<T> T get(String name);
	Iterator<String> namesIterator();
	
	static Mutable create() {
		return new Mutable();
	}
	
	static class Mutable implements Metrics {
		
		private final Map<String, Object> values = new HashMap<>();
		
		public void set(String name, Object value) {
			Objects.requireNonNull(name);
			values.put(name, value);
		}
		
		public <T> T get(String name) {
			Objects.requireNonNull(name);
			@SuppressWarnings("unchecked")
			T value = (T) values.get(name);
			return value;
		}
		
		@Override
		public Iterator<String> namesIterator() {
			return values.keySet().iterator();
		}
		
		public Immutable freeze() {
			return new Immutable(Map.copyOf(values));
		}
	}
	
	static class Immutable implements Metrics {
		
		private final Map<String, Object> values;
		
		private Immutable(Map<String, Object> values) {
			this.values = Objects.requireNonNull(values);
		}
		
		public <T> T get(String name) {
			Objects.requireNonNull(name);
			@SuppressWarnings("unchecked")
			T value = (T) values.get(name);
			return value;
		}
		
		@Override
		public Iterator<String> namesIterator() {
			return values.keySet().iterator();
		}
	}
}
