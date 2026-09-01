package sune.app.mediadown.gui.form;

import java.util.Objects;

import javafx.scene.Node;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.language.Translation;
import sune.util.ssdf2.SSDNode;

public abstract class FormField<T> {
	
	/** @since 00.02.07 */
	protected final T property;
	/** @since 00.02.09 */
	protected final FormFieldType type;
	protected final String name;
	protected final String title;
	
	public FormField(T property, FormFieldType type, String name, String title) {
		this.property = Objects.requireNonNull(property);
		this.type = Objects.requireNonNull(type);
		this.name = Objects.requireNonNull(name);
		this.title = Objects.requireNonNull(title);
	}
	
	/** @since 00.02.09 */
	protected final Translation translation() {
		return MediaDownloader.translation().getTranslation("forms.fields." + type.name().toLowerCase());
	}
	
	public abstract Node render(Form form);
	/** @since 00.02.09 */
	public abstract void value(SSDNode node);
	/** @since 00.02.07 */
	public abstract Object value();
	
	/** @since 00.02.07 */
	public T property() {
		return property;
	}
	
	/** @since 00.02.09 */
	public FormFieldType type() {
		return type;
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