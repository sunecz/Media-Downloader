package sune.app.mediadown.gui.util;

import java.util.Map;
import java.util.WeakHashMap;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

/** @since 00.02.09 */
public class SimpleGUIProperty<T> implements GUIProperty<T> {
	
	protected final ObjectProperty<T> delegate;
	
	public SimpleGUIProperty() {
		delegate = new SimpleObjectProperty<>();
	}
	
	public SimpleGUIProperty(T initialValue) {
		delegate = new SimpleObjectProperty<>(initialValue);
	}
	
	@Override public T get() { return delegate.get(); }
	@Override public void set(T newValue) { delegate.set(newValue); }
	@Override public T getValue() { return delegate.getValue(); }
	@Override public void setValue(T value) { delegate.setValue(value); }
	
	@Override public void bind(ObservableValue<? extends T> observable) { delegate.bind(observable); }
	@Override public void unbind() { delegate.unbind(); }
	@Override public boolean isBound() { return delegate.isBound(); }
	@Override public void bindBidirectional(Property<T> other) { delegate.bindBidirectional(other); }
	@Override public void unbindBidirectional(Property<T> other) { delegate.unbindBidirectional(other); }
	
	@Override public Object getBean() { return delegate.getBean(); }
	@Override public String getName() { return delegate.getName(); }
	
	@Override
	public void addListener(javafx.beans.value.ChangeListener<? super T> listener) {
		delegate.addListener(listener);
	}
	
	@Override
	public void removeListener(javafx.beans.value.ChangeListener<? super T> listener) {
		delegate.removeListener(listener);
	}
	
	@Override
	public void addListener(InvalidationListener listener) {
		delegate.addListener(listener);
	}
	
	@Override
	public void removeListener(InvalidationListener listener) {
		delegate.removeListener(listener);
	}
	
	@Override
	public void addListener(sune.app.mediadown.util.Property.ChangeListener<? super T> listener) {
		addListener(GUIListeners.get(this, listener));
	}
	
	@Override
	public void removeListener(sune.app.mediadown.util.Property.ChangeListener<? super T> listener) {
		removeListener(GUIListeners.get(this, listener));
	}
	
	private static final class GUIListeners {
		
		private static final Map<
			sune.app.mediadown.util.Property.ChangeListener<?>,
			javafx.beans.value.ChangeListener<?>
		> listeners = new WeakHashMap<>();
		
		private GUIListeners() {
		}
		
		private static final <T> javafx.beans.value.ChangeListener<? super T> create(
			sune.app.mediadown.util.Property<T> property,
			sune.app.mediadown.util.Property.ChangeListener<? super T> listener
		) {
			javafx.beans.value.ChangeListener<? super T> guiListener = (
				(unused, oldValue, newValue) -> listener.changed(property, oldValue, newValue)
			);
			
			return guiListener;
		}
		
		public static final <T> javafx.beans.value.ChangeListener<? super T> get(
			sune.app.mediadown.util.Property<T> property,
			sune.app.mediadown.util.Property.ChangeListener<? super T> listener
		) {
			@SuppressWarnings("unchecked")
			javafx.beans.value.ChangeListener<? super T> newListener = (
				(javafx.beans.value.ChangeListener<? super T>) listeners.compute(
					listener,
					(k, v) -> v == null ? create(property, listener) : v
				)
			);
			
			return newListener;
		}
	}
}
