package sune.app.mediadown.util;

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
 * @author Sune
 * @since 00.02.09
 */
public interface Property<T> {
	
	T get();
	void set(T newValue);
	
	void addListener(ChangeListener<? super T> listener);
	void removeListener(ChangeListener<? super T> listener);
	
	@FunctionalInterface
	public static interface ChangeListener<T> {
		
		void changed(Property<? extends T> observable, T oldValue, T newValue);
	}
}
