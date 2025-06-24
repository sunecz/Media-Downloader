package sune.app.mediadown.pipeline.state;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** @since 00.02.09 */
public interface Metrics {
	
	String type();
	Object get(String name);
	Set<String> names();
	
	static Immutable freeze(Metrics metrics) {
		Map<String, Object> values = new HashMap<>();
		
		for(String name : metrics.names()) {
			values.put(name, metrics.get(name));
		}
		
		return new Immutable(metrics.type(), values);
	}
	
	static class Mutable implements Metrics {
		
		private final String type;
		private final Map<String, Object> values = new HashMap<>();
		
		public Mutable(String type) {
			this.type = type;
		}
		
		public void set(String name, Object value) {
			values.put(Objects.requireNonNull(name), value);
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public Object get(String name) {
			return values.get(Objects.requireNonNull(name));
		}
		
		@Override
		public Set<String> names() {
			return values.keySet();
		}
		
		public Immutable freeze() {
			return new Immutable(type, Map.copyOf(values));
		}
	}
	
	static class Immutable implements Metrics {
		
		private final String type;
		private final Map<String, Object> values;
		
		private Immutable(String type, Map<String, Object> values) {
			this.type = type;
			this.values = Objects.requireNonNull(values);
		}
		
		@Override
		public String type() {
			return type;
		}
		
		@Override
		public Object get(String name) {
			return values.get(Objects.requireNonNull(name));
		}
		
		@Override
		public Set<String> names() {
			return Collections.unmodifiableSet(values.keySet());
		}
	}
}
