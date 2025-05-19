package sune.app.mediadown.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** @since 00.02.09 */
public class SimpleProperty<T> implements Property<T> {
	
	protected final ChangeListeners<T> listeners = new ChangeListeners<>();
	protected T value;
	
	public SimpleProperty() {
	}
	
	public SimpleProperty(T initialValue) {
		value = initialValue;
	}
	
	@Override
	public T get() {
		return value;
	}
	
	@Override
	public void set(T newValue) {
		if(value != newValue) {
			T oldValue = value;
			value = newValue;
			listeners.call(this, oldValue, newValue);
		}
	}
	
	@Override
	public void addListener(ChangeListener<? super T> listener) {
		listeners.add(listener);
	}
	
	@Override
	public void removeListener(ChangeListener<? super T> listener) {
		listeners.remove(listener);
	}
	
	private static final class ChangeListeners<T> {
		
		private final List<ChangeListener<? super T>> listeners = new ArrayList<>();
		
		public void call(Property<? extends T> observable, T oldValue, T newValue) {
			listeners.forEach((l) -> l.changed(observable, oldValue, newValue));
		}
		
		public void add(ChangeListener<? super T> listener) {
			listeners.add(Objects.requireNonNull(listener));
		}
		
		public void remove(ChangeListener<? super T> listener) {
			listeners.remove(Objects.requireNonNull(listener));
		}
	}
}
