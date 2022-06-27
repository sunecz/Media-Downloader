package sune.app.mediadown.event;

public interface IEventMapper<T extends IEventType> {
	
	Listener<?> map(EventType<T, ?> type);
}