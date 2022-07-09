package sune.app.mediadown.gui.form.field;

import java.util.Collection;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public class SelectField<T> extends FormField<T> {
	
	private final ComboBox<Object> control;
	
	public SelectField(T property, String name, String title, Collection<?> items) {
		super(property, name, title);
		control = new ComboBox<>();
		control.getItems().setAll(items);
		control.setMaxWidth(Double.MAX_VALUE);
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void value(SSDValue value, SSDType type) {
		control.getSelectionModel().select(value.value());
	}
	
	@Override
	public Object value() {
		return control.getSelectionModel().getSelectedItem();
	}
}