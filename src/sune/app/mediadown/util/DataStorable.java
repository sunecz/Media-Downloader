package sune.app.mediadown.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface DataStorable {
	
	boolean has(String name);
	void set(String name, Object value);
	<T> T get(String name);
	/** @since 00.02.09 */
	<T> T get(String name, T defaultValue);
	void remove(String name);
	/** @since 00.02.09 */
	Set<String> keys();
	/** @since 00.02.09 */
	Collection<Object> values();
	/** @since 00.02.09 */
	Map<String, Object> data();
}