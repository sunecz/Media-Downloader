package sune.app.mediadown.util;

import java.util.Arrays;
import java.util.List;

public final class Tuple {
	
	private final List<Object> objects;
	
	public Tuple(Object... items) {
		objects = Arrays.asList(items);
	}
	
	public <T> T get(int index) {
		@SuppressWarnings("unchecked")
		T casted = (T) objects.get(index);
		return casted;
	}
	
	public int size() {
		return objects.size();
	}
}