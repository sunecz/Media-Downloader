package sune.app.mediadown.gui.util;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;

public class FXProperty<T> implements ReadOnlyProperty<T> {
	
	private final ReadOnlyProperty<T> property;
	private final Map<Long, ChangeListener<? super T>> listenersChange;
	private final Map<Long, InvalidationListener> listenersInvalid;
	
	public FXProperty(ReadOnlyProperty<T> property) {
		if((property == null))
			throw new IllegalArgumentException("Property cannot be null!");
		this.property 		  = property;
		this.listenersChange  = new LinkedHashMap<>();
		this.listenersInvalid = new LinkedHashMap<>();
	}
	
	@Override
	public void addListener(ChangeListener<? super T> listener) {
		property.addListener(listener);
	}
	
	@Override
	public void addListener(InvalidationListener listener) {
		property.addListener(listener);
	}
	
	@Override
	public void removeListener(ChangeListener<? super T> listener) {
		property.removeListener(listener);
	}
	
	@Override
	public void removeListener(InvalidationListener listener) {
		property.removeListener(listener);
	}
	
	private final void addListener(long id, ChangeListener<? super T> listener) {
		listenersChange.put(id, listener);
		addListener(listener);
	}
	
	private final void addListener(long id, InvalidationListener listener) {
		listenersInvalid.put(id, listener);
		addListener(listener);
	}
	
	private final void removeListener(long id, boolean invalidListener) {
		if((invalidListener)) {
			removeListener(listenersInvalid.remove(id));
		} else {
			removeListener(listenersChange.remove(id));
		}
	}
	
	public void once(ChangeListener<? super T> listener) {
		final long id = System.nanoTime();
		ChangeListener<T> temp = ((o, ov, nv) -> {
			listener.changed(o, ov, nv);
			removeListener(id, false);
		});
		addListener(id, temp);
	}
	
	public void once(InvalidationListener listener) {
		final long id = System.nanoTime();
		InvalidationListener temp = ((o) -> {
			listener.invalidated(o);
			removeListener(id, true);
		});
		addListener(id, temp);
	}
	
	@Override
	public T getValue() {
		return property.getValue();
	}
	
	@Override
	public Object getBean() {
		return property.getBean();
	}
	
	@Override
	public String getName() {
		return property.getName();
	}
}