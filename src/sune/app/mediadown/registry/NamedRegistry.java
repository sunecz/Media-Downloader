package sune.app.mediadown.registry;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public interface NamedRegistry<T> extends Iterable<Entry<String, T>> {
	
	void           register  (String name, T value);
	void           unregister(String name);
	T              get       (String name);
	void           clear();
	int            size();
	boolean        isEmpty();
	Map<String, T> all();
	Collection <T> allValues();
	Set<String>    allNames();
}