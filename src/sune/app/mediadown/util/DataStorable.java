package sune.app.mediadown.util;

public interface DataStorable {
	
	boolean has(String name);
	void set(String name, Object value);
	<T> T get(String name);
	void remove(String name);
}