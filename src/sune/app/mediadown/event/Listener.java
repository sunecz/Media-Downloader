package sune.app.mediadown.event;

public interface Listener<T> {
	
	void call(T value);
}