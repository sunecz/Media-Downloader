package sune.app.mediadown.resource.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import sune.app.mediadown.util.CheckedSupplier;

/** @since 00.02.07 */
public class Cache {
	
	private static final boolean DEFAULT_SUPPORT_CONCURRENCY = true;
	
	private final Map<Object, CacheObject<?>> objects;
	
	public Cache() {
		this(DEFAULT_SUPPORT_CONCURRENCY);
	}
	
	public Cache(boolean supportConcurrency) {
		objects = supportConcurrency ? new ConcurrentHashMap<>() : new HashMap<>();
	}
	
	/** @since 00.02.09 */
	private final <T> CacheObject<T> newObject(Object key, T instance, CheckedSupplier<T> creator) {
		return new CacheObject<>(this, key, instance, creator);
	}
	
	/** @since 00.02.09 */
	private final <T> CacheObject<T> setAndGetObjectChecked(Object key, CheckedSupplier<T> creator) throws Exception {
		T instance = creator.get();
		
		if(!canAddValue(instance)) {
			return null;
		}
		
		CacheObject<T> object = newObject(key, instance, creator);
		objects.put(key, object);
		
		return object;
	}
	
	/** @since 00.02.05 */
	private final <T> T setAndGetChecked(Object key, CheckedSupplier<T> creator) throws Exception {
		CacheObject<T> object = setAndGetObjectChecked(key, creator);
		return object != null ? object.value() : null;
	}
	
	protected <T> boolean canAddValue(T instance) { return true; /* Always add all values by default */ }
	
	public <T> void set(Object key, T instance) {
		setChecked(key, instance, null);
	}
	
	public <T> void set(Object key, Supplier<T> creator) {
		setChecked(key, creator.get(), creator::get);
	}
	
	public <T> void set(Object key, T instance, Supplier<T> creator) {
		setChecked(key, instance, creator::get);
	}
	
	/** @since 00.02.05 */
	public <T> void setChecked(Object key, CheckedSupplier<T> creator) throws Exception {
		setChecked(key, creator.get(), creator);
	}
	
	/** @since 00.02.05 */
	public <T> void setChecked(Object key, T instance, CheckedSupplier<T> creator) {
		if(!canAddValue(instance)) {
			return;
		}
		
		objects.put(key, newObject(key, instance, creator));
	}
	
	public <T> T get(Object key) {
		try { return getChecked(key); } catch(Exception ex) { /* Ignore */ } return null;
	}
	
	public <T> T get(Object key, Supplier<T> creator) {
		try { return getChecked(key, creator::get); } catch(Exception ex) { /* Ignore */ } return null;
	}
	
	public <T> T getChecked(Object key) throws Exception {
		CacheObject<?> object = objects.get(key);
		
		if(object == null) {
			return null;
		}
		
		Object instance = object.value();
		
		if(instance == null) {
			CheckedSupplier<?> creator = object.creator();
			
			if(creator != null) {
				instance = creator.get();
				
				if(!canAddValue(instance)) {
					return null;
				}
				
				@SuppressWarnings("unchecked")
				CacheObject<T> newObject = newObject(
					key, (T) instance, (CheckedSupplier<T>) creator
				);
				
				objects.put(key, newObject);
			}
		}
		
		@SuppressWarnings("unchecked")
		T casted = (T) instance;
		return casted;
	}
	
	/** @since 00.02.05 */
	public <T> T getChecked(Object key, CheckedSupplier<T> creator) throws Exception {
		CacheObject<?> object = objects.get(key);
		
		if(object == null) {
			return setAndGetChecked(key, creator);
		}
		
		@SuppressWarnings("unchecked")
		T casted = (T) object.value();
		return casted;
	}
	
	public boolean has(Object key) {
		CacheObject<?> object; return (object = objects.get(key)) != null && object.value() != null;
	}
	
	public void remove(Object key) {
		objects.remove(key);
	}
	
	/** @since 00.02.07 */
	public void clear() {
		objects.clear();
	}
	
	/** @since 00.02.09 */
	private static final class Cleaner {
		
		private static final ReferenceQueue<Object> queue = new ReferenceQueue<>();
		
		private static Thread thread;
		
		static {
			thread = new Thread(Cleaner::run);
			thread.setDaemon(true);
			thread.start();
		}
		
		private static final void run() {
			try {
				while(true) {
					Ref<?> ref = (Ref<?>) queue.remove();
					ref.cache().remove(ref.key());
				}
			} catch(InterruptedException ex) {
				// Ignore
			}
		}
		
		public static class Ref<T> extends SoftReference<T> {
			
			private final Cache cache;
			private final Object key;
			
			public Ref(Cache cache, Object key, T value) {
				super(value, queue);
				this.cache = Objects.requireNonNull(cache);
				this.key = key;
			}
			
			public Cache cache() {
				return cache;
			}
			
			public Object key() {
				return key;
			}
			
			public T value() {
				return get();
			}
		}
	}
	
	/** @since 00.02.09 */
	private static final class CacheObject<T> extends Cleaner.Ref<T> {
		
		private final CheckedSupplier<T> creator;
		
		public CacheObject(Cache cache, Object key, T value, CheckedSupplier<T> creator) {
			super(cache, key, value);
			this.creator = creator;
		}
		
		public CheckedSupplier<T> creator() {
			return creator;
		}
	}
}