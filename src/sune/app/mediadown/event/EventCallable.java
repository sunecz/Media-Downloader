package sune.app.mediadown.event;

/** @since 00.01.26 */
public interface EventCallable<T extends IEventType> {
	
	<E> void call(EventType<T, E> type);
	<E> void call(EventType<T, E> type, E value);
}