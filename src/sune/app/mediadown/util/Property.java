package sune.app.mediadown.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Simpler replacement for the JavaFX Property classes.
 * </p>
 * 
 * <p>
 * This class was made primarily to overcome the need to depend on the JavaFX framework
 * just to have a functionality provided by the Property classes, such as observability.
 * </p>
 * 
 * @author sune
 * @since 00.02.09
 */
public class Property<T> {
	
	protected final ChangeListeners<T> listeners = new ChangeListeners<>();
	protected T value;
	
	public Property() {
	}
	
	public Property(T initialValue) {
		value = initialValue;
	}
	
	public T get() {
		return value;
	}
	
	public void set(T newValue) {
		if(value != newValue) {
			T oldValue = value;
			value = newValue;
			listeners.call(this, oldValue, newValue);
		}
	}
	
	public void addListener(ChangeListener<? super T> listener) {
		listeners.add(listener);
	}
	
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
	
	@FunctionalInterface
	public static interface ChangeListener<T> {
		
		void changed(Property<? extends T> observable, T oldValue, T newValue);
	}
}
