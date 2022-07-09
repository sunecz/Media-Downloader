package sune.app.mediadown.gui.form;

import java.util.Objects;

import javafx.scene.Node;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public abstract class FormField<T> {
	
	/** @since 00.02.07 */
	protected final T property;
	protected final String name;
	protected final String title;
	
	public FormField(T property, String name, String title) {
		this.property = Objects.requireNonNull(property);
		this.name = Objects.requireNonNull(name);
		this.title = Objects.requireNonNull(title);
	}
	
	public abstract Node render(Form form);
	/** @since 00.02.07 */
	public abstract void value(SSDValue value, SSDType type);
	/** @since 00.02.07 */
	public abstract Object value();
	
	/** @since 00.02.07 */
	public T property() {
		return property;
	}
	
	/** @since 00.02.07 */
	public String name() {
		return name;
	}
	
	/** @since 00.02.07 */
	public String title() {
		return title;
	}
}