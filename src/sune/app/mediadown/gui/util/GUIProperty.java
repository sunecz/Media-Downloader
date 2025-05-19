package sune.app.mediadown.gui.util;

/**
 * <p>
 * Bridge class between the simpler Property class and the JavaFX Property classes.
 * </p>
 * 
 * @author Sune
 * @since 00.02.09
 */
public interface GUIProperty<T>
extends
	sune.app.mediadown.util.Property<T>,
	javafx.beans.property.Property<T> {
}
