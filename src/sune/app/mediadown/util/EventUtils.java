package sune.app.mediadown.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.IEventMapper;
import sune.app.mediadown.event.IEventType;
import sune.app.mediadown.event.Listener;

public final class EventUtils {
	
	private static final <E extends IEventType, T> void bind0(EventBindable<E> bindable, EventRegistry<E> registry,
	                                                          EventType<E, T> type) {
		bindable.addEventListener(type, (value) -> registry.call(type, value));
	}
	
	@SafeVarargs
	public static final <E extends IEventType> void bind(EventBindable<E> bindable, EventRegistry<E> registry,
	                                                     EventType<E, ?>... types) {
		for(EventType<E, ?> type : types) bind0(bindable, registry, type);
	}
	
	public static final <E extends IEventType> void bind(Class<E> clazz, EventBindable<E> bindable, EventRegistry<E> registry) {
		EventType<E, Object>[] types = values(clazz);
		for(EventType<E, Object> eventType : types) {
			bindable.addEventListener(eventType, (data) -> registry.call(eventType, data));
		}
	}
	
	@SuppressWarnings("unchecked")
	private static final <E extends IEventType> EventType<E, Object>[] values(Class<E> clazz) {
		try {
			Method method = clazz.getDeclaredMethod("values");
			// the method has to be a static method
			if((Modifier.isStatic(method.getModifiers()))) {
				Reflection.setAccessible(method, true);
				return (EventType<E, Object>[]) method.invoke(null);
			}
		} catch(Exception ex) {
			throw new IllegalStateException("Unable to get all event type values form the given class: " + clazz);
		}
		return null;
	}
	
	private static final <E extends IEventType> void registryAction(Class<E> clazz, EventRegistry<E> registry,
			Consumer<Pair<EventRegistry<E>, EventType<E, Object>>> action) {
		EventType<E, Object>[] types = values(clazz);
		if((types != null)) {
			for(EventType<E, Object> eventType : types)
				// apply the given action
				action.accept(new Pair<EventRegistry<E>, EventType<E, Object>>(registry, eventType));
		} else {
			throw new IllegalStateException("Unable to add the given listener to all values from the given class: " + clazz);
		}
	}
	
	public static final <E extends IEventType> void addListener(Class<E> clazz, EventRegistry<E> registry,
	                                                            Listener<Object> listener) {
		registryAction(clazz, registry, (pair) -> pair.a.add(pair.b, listener));
	}
	
	public static final <E extends IEventType> void removeListener(Class<E> clazz, EventRegistry<E> registry,
	                                                               Listener<Object> listener) {
		registryAction(clazz, registry, (pair) -> pair.a.add(pair.b, listener));
	}
	
	public static final <E extends IEventType> void mapListeners(Class<E> clazz, EventBindable<E> bindable,
	                                                             IEventMapper<E> mapper) {
		EventType<E, Object>[] types = values(clazz);
		for(EventType<E, Object> eventType : types) {
			@SuppressWarnings("unchecked")
			Listener<Object> listener = (Listener<Object>) mapper.map(eventType);
			if((listener != null))
				bindable.addEventListener(eventType, listener);
		}
	}
	
	// forbid anyone to create an instance of this class
	private EventUtils() {
	}
}