package sune.app.mediadown.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class Cache {
	
	private final Map<Object, SoftReference<?>> objects = new HashMap<>();
	private final Map<Object, CheckedSupplier<?>> creators = new HashMap<>();
	
	protected <T> boolean canAddValue(T instance) {
		// Always add the value by default
		return true;
	}
	
	public <T> void set(Object key, T instance) {
		setChecked(key, instance, (CheckedSupplier<T>) null);
	}
	
	public <T> void set(Object key, Supplier<T> creator) {
		set(key, creator.get(), creator);
	}
	
	public <T> void set(Object key, T instance, Supplier<T> creator) {
		setChecked(key, instance, (CheckedSupplier<T>) () -> creator.get());
	}
	
	/** @since 00.02.05 */
	public <T> void setChecked(Object key, CheckedSupplier<T> creator) throws Exception {
		setChecked(key, creator.get(), creator);
	}
	
	/** @since 00.02.05 */
	public <T> void setChecked(Object key, T instance, CheckedSupplier<T> creator) {
		// Check if the value can be added
		if(!canAddValue(instance))
			// Do not add the value
			return;
		objects.put(key, new SoftReference<>(instance));
		if((creator != null))
			creators.put(key, creator);
	}
	
	public <T> T get(Object key) {
		return Utils.ignore(() -> getChecked(key));
	}
	
	public <T> T getChecked(Object key) throws Exception {
		SoftReference<?> reference = objects.get(key);
		if((reference == null))
			throw new NoSuchElementException();
		Object instance = reference.get();
		if((instance == null)) {
			// Try to create a new instance
			CheckedSupplier<?> creator = creators.get(key);
			if((creator != null)) {
				// Creator found, create a new instance
				instance = creator.get();
				// Check if the value can be added
				if(!canAddValue(instance))
					// do not add the value
					return null;
				// Update the instance in the objects
				objects.put(key, new SoftReference<>(instance));
			}
		}
		@SuppressWarnings("unchecked")
		T casted = (T) instance;
		return casted;
	}
	
	private final <T> T setAndGet(Object key, Supplier<T> creator) {
		set(key, creator); // Set the value and the creator so it exists
		return get(key);   // Return the newly created the value
	}
	
	/** @since 00.02.05 */
	private final <T> T setAndGetChecked(Object key, CheckedSupplier<T> creator) throws Exception {
		setChecked(key, creator); // Set the value and the creator so it exists
		return getChecked(key);   // Return the newly created the value
	}
	
	public <T> T get(Object key, Supplier<T> creator) {
		return objects.containsKey(key) && creators.containsKey(key)
					? get(key)                 // The object can be recreated using the creator, if garbage-collected
					: setAndGet(key, creator); // No creator, set one, and return newly created value
	}
	
	/** @since 00.02.05 */
	public <T> T getChecked(Object key, CheckedSupplier<T> creator) throws Exception {
		return objects.containsKey(key) && creators.containsKey(key)
					? getChecked(key)                 // The object can be recreated using the creator, if garbage-collected
					: setAndGetChecked(key, creator); // No creator, set one, and return newly created value
	}
	
	public boolean has(Object key) {
		SoftReference<?> ref; return (ref = objects.get(key)) != null && ref.get() != null;
	}
	
	public void remove(Object key) {
		objects .remove(key);
		creators.remove(key);
	}
	
	/** @since 00.02.07 */
	public void clear() {
		objects .clear();
		creators.clear();
	}
}