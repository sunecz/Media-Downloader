package sune.app.mediadown.event;

public interface EventBindable<E extends IEventType> {
	
	<T> void addEventListener(EventType<E, T> type, Listener<T> listener);
	<T> void removeEventListener(EventType<E, T> type, Listener<T> listener);
}