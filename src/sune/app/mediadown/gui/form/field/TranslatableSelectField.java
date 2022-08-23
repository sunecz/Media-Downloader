package sune.app.mediadown.gui.form.field;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

/** @since 00.02.07 */
public class TranslatableSelectField<T, V> extends FormField<T> {
	
	private final ComboBox<V> control;
	private final ValueTransformer<V> transformer;
	
	public TranslatableSelectField(T property, String name, String title, Collection<V> items,
			ValueTransformer<V> transformer) {
		super(property, name, title);
		control = new ComboBox<>();
		control.getItems().setAll(items);
		control.setCellFactory((p) -> new TranslatableCell<>(this));
		control.setButtonCell(new TranslatableCell<>(this));
		control.setMaxWidth(Double.MAX_VALUE);
		this.transformer = Objects.requireNonNull(transformer);
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void value(SSDValue value, SSDType type) {
		String string = Utils.removeStringQuotes(value.stringValue());
		control.getSelectionModel().select(transformer.value(string));
	}
	
	@Override
	public Object value() {
		return transformer.string(Utils.cast(control.getSelectionModel().getSelectedItem()));
	}
	
	private static final class TranslatableCell<V> extends ListCell<V> {
		
		private final TranslatableSelectField<?, V> field;
		
		public TranslatableCell(TranslatableSelectField<?, V> field) {
			this.field = Objects.requireNonNull(field);
		}
		
		@Override
		protected void updateItem(V item, boolean empty) {
			super.updateItem(item, empty);
			
			if(!empty) {
				setText(field.transformer.title(item));
			} else {
				setText(null);
				setGraphic(null);
			}
		}
	}
	
	public static interface ValueTransformer<T> {
		
		T value(String string);
		String string(T value);
		String title(T value);
		
		static <T> ValueTransformer<T> of(Function<String, T> value, Function<T, String> string,
				Function<T, String> title) {
			Objects.requireNonNull(value);
			Objects.requireNonNull(string);
			Objects.requireNonNull(title);
			
			return new ValueTransformer<T>() {
				
				@Override public T value(String s)  { return value .apply(s); }
				@Override public String string(T v) { return string.apply(v); }
				@Override public String title(T v)  { return title .apply(v); }
			};
		}
	}
}