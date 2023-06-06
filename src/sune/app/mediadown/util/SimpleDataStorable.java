package sune.app.mediadown.util;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class SimpleDataStorable implements DataStorable {
	
	/* Implementation note:
	 * Since version 00.02.04 the order of keys in the map is consistent.
	 * This also implies that the hash code of the map is consistent, i.e.
	 * if there are two maps with the same keys with the same values, then
	 * the hash codes will be the same.
	 */
	
	private static final Map<String, Object> EMPTY_DATA = new TreeMap<>(Comparator.naturalOrder());
	
	protected Map<String, Object> data;
	
	public SimpleDataStorable() {
		data = EMPTY_DATA;
	}
	
	public SimpleDataStorable(Map<String, Object> values) {
		// Only initialize the data field if really needed
		if(values != null && !values.isEmpty()) {
			data = new TreeMap<>(Comparator.naturalOrder());
			data.putAll(values);
		} else {
			data = EMPTY_DATA;
		}
	}
	
	protected final boolean isEmptyData() {
		return data == EMPTY_DATA;
	}
	
	protected final Map<String, Object> ensureOwnData() {
		if(isEmptyData()) {
			data = new TreeMap<>(Comparator.naturalOrder());
		}
		
		return data;
	}
	
	@Override
	public boolean has(String name) {
		if(isEmptyData()) {
			return false;
		}
		
		return data.containsKey(name);
	}
	
	@Override
	public void set(String name, Object value) {
		ensureOwnData().put(name, value);
	}
	
	@Override
	public <T> T get(String name) {
		if(isEmptyData()) {
			return null;
		}
		
		@SuppressWarnings("unchecked")
		T value = (T) data.get(name);
		return value;
	}
	
	@Override
	public void remove(String name) {
		if(isEmptyData()) {
			return;
		}
		
		data.remove(name);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(data);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		SimpleDataStorable other = (SimpleDataStorable) obj;
		return Objects.equals(data, other.data);
	}
}