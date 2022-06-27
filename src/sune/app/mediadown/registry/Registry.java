package sune.app.mediadown.registry;

import java.util.List;

public interface Registry<T> extends Iterable<T> {
	
	void    register  (T value);
	void    unregister(T value);
	List<T> all();
}