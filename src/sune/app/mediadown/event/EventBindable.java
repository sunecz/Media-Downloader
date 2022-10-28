package sune.app.mediadown.event;

/** @since 00.02.08 */
public interface EventBindable<T extends EventType> {
	
	<V> void addEventListener(Event<? extends T, V> event, Listener<V> listener);
	<V> void removeEventListener(Event<? extends T, V> event, Listener<V> listener);
}