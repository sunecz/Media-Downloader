package sune.app.mediadown.event;

/** @since 00.02.08 */
public interface EventCallable<T extends EventType> {
	
	<V> void call(Event<? extends T, V> event);
	<V> void call(Event<? extends T, V> event, V value);
}