package sune.app.mediadown.util;

public final class Property<T> {
	
	private T value;
	
	public Property() {
	}
	
	public Property(T value) {
		this.value = value;
	}
	
	public void setValue(T value) {
		this.value = value;
	}
	
	public T getValue() {
		return value;
	}
}