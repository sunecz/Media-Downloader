package sune.app.mediadown.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import sune.app.mediadown.event.Listener;

/** @since 00.02.09 */
public final class RefManager {
	
	private static final RefManager instance = new RefManager();
	
	private final ReferenceQueue<Object> queue;
	private final Map<Object, Ref<?>> mappings;
	private final Map<Ref<?>, List<Listener<?>>> listeners;
	private final Thread thread;
	
	private RefManager() {
		queue = new ReferenceQueue<>();
		mappings = new WeakHashMap<>();
		listeners = new HashMap<>();
		thread = new Thread(this::run);
		thread.setDaemon(true);
		thread.start();
	}
	
	private static final <T> List<Listener<?>> addListener(List<Listener<?>> list, Listener<T> value) {
		if(list == null) {
			list = new ArrayList<>();
		}
		
		list.add(value);
		return list;
	}
	
	private static final <T> List<Listener<?>> removeListener(List<Listener<?>> list, Listener<T> value) {
		if(list == null) {
			return list;
		}
		
		list.remove(value);
		return list.isEmpty() ? null : list;
	}
	
	public static final <T> Ref<T> register(T object, Listener<Ref<T>> listener) {
		return instance.doRegister(object, listener);
	}
	
	public static final <T> Ref<T> unregister(T object, Listener<Ref<T>> listener) {
		return instance.doUnregister(object, listener);
	}
	
	private final void run() {
		try {
			while(true) {
				Ref<?> ref = (Ref<?>) queue.remove();
				callListeners(ref);
				listeners.remove(ref);
			}
		} catch(InterruptedException ex) {
			// Ignore
		}
	}
	
	@SuppressWarnings("unchecked")
	private final void callListeners(Ref<?> ref) {
		List<Listener<?>> list = listeners.get(ref);
		
		if(list == null) {
			return;
		}
		
		for(Listener<?> listener : list) {
			try {
				((Listener<Object>) listener).call(ref);
			} catch(Exception ex) {
				// Ignore
			}
		}
	}
	
	private final <T> Ref<T> doRegister(T object, Listener<Ref<T>> listener) {
		if(object == null || listener == null) {
			throw new IllegalArgumentException();
		}
		
		@SuppressWarnings("unchecked")
		Ref<T> ref = (Ref<T>) mappings.computeIfAbsent(object, (k) -> new Ref<>(k, queue));
		listeners.compute(ref, (k, v) -> addListener(v, listener));
		return ref;
	}
	
	private final <T> Ref<T> doUnregister(T object, Listener<Ref<T>> listener) {
		if(object == null || listener == null) {
			throw new IllegalArgumentException();
		}
		
		@SuppressWarnings("unchecked")
		Ref<T> ref = (Ref<T>) mappings.get(object);
		
		if(ref != null
				&& listeners.compute(ref, (k, v) -> removeListener(v, listener)) == null) {
			mappings.remove(object);
		}
		
		return ref;
	}
	
	public static final class Ref<T> extends WeakReference<T> {
		
		private Ref(T referent, ReferenceQueue<? super T> queue) {
			super(referent, queue);
		}
	}
}