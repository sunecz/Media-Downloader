package sune.app.mediadown.event;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import sune.app.mediadown.util.Utils;

public class EventRegistry<T extends EventType> implements EventCallable<T> {
	
	protected final Map<Event<?, ?>, Queue<Listener<?>>> listeners = new ConcurrentHashMap<>();
	
	@SuppressWarnings("unchecked")
	protected final <V> void invokeListeners(Event<? extends T, V> event, V value) {
		Optional.ofNullable(listeners.get(event)).ifPresent((q) -> q.forEach((l) -> ((Listener<V>) l).call(value)));
	}
	
	public final <V> void add(Event<? extends T, V> event, Listener<V> listener) {
		listeners.computeIfAbsent(event, (k) -> new ConcurrentLinkedQueue<>()).add(listener);
	}
	
	@SafeVarargs
	public final void addMany(Listener<?> listener, Event<? extends T, ?>... events) {
		for(Event<? extends T, ?> event : events) {
			add(Utils.<Event<? extends T, Object>>cast(event), Utils.<Listener<Object>>cast(listener));
		}
	}
	
	public final <V> void remove(Event<? extends T, V> event) {
		listeners.remove(event);
	}
	
	public final <V> void remove(Event<? extends T, V> event, Listener<V> listener) {
		Optional.ofNullable(listeners.get(event)).ifPresent((q) -> q.remove(listener));
	}
	
	@Override
	public final <V> void call(Event<? extends T, V> event) {
		invokeListeners(event, null);
	}
	
	@Override
	public final <V> void call(Event<? extends T, V> event, V value) {
		invokeListeners(event, value);
	}
	
	public final <V> List<Listener<?>> listenersOfEvent(Event<? extends T, V> event) {
		return List.copyOf(Optional.ofNullable(listeners.get(event)).orElseGet(() -> new ConcurrentLinkedQueue<>()));
	}
	
	public final void clear() {
		listeners.clear();
	}
	
	@SuppressWarnings("unchecked")
	public final <E extends T, V> void bind(EventBindable<E> bindable, Event<? extends E, V> event) {
		listenersOfEvent(event).forEach((listener) -> bindable.addEventListener(event, (Listener<V>) listener));
	}
	
	@SafeVarargs
	public final <E extends T> void bindAll(EventBindable<E> bindable, Event<? extends E, ?>... events) {
		for(Event<? extends E, ?> event : events) bind(bindable, event);
	}
}