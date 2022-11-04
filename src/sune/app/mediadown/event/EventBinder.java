package sune.app.mediadown.event;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import sune.app.mediadown.util.Utils;

/** @since 00.02.08 */
public class EventBinder implements EventBindable<EventType> {
	
	private final Map<Class<? extends EventType>, List<EventBindable<?>>> bindables = new HashMap<>();
	
	private final <T extends EventType> List<EventBindable<T>> get(Class<? extends Event<T, ?>> clazz) {
		return Utils.<List<EventBindable<T>>>cast(Optional.ofNullable(bindables.get(clazz)).orElseGet(List::of));
	}
	
	private final <T extends EventType> List<EventBindable<T>> get(Event<? extends EventType, ?> event) {
		return get(Utils.<Class<? extends Event<T, ?>>>cast(event.getClass()));
	}
	
	public <T extends EventType> void register(Class<T> clazz, EventBindable<T> bindable) {
		bindables.computeIfAbsent(clazz, (k) -> new LinkedList<>()).add(bindable);
	}
	
	@Override
	public <V> void addEventListener(Event<? extends EventType, V> event, Listener<V> listener) {
		for(EventBindable<EventType> bindable : get(event)) {
			bindable.addEventListener(event, listener);
		}
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends EventType, V> event, Listener<V> listener) {
		for(EventBindable<EventType> bindable : get(event)) {
			bindable.removeEventListener(event, listener);
		}
	}
}