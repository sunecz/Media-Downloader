package sune.app.mediadown.gui.form.field;

import java.util.Collection;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.gui.form.FormFieldType;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;

public class SelectField<T> extends FormField<T> {
	
	private final ComboBox<Object> control;
	
	public SelectField(T property, String name, String title, Collection<?> items) {
		super(property, FormFieldType.SELECT, name, title);
		control = new ComboBox<>();
		control.getItems().setAll(items);
		control.setMaxWidth(Double.MAX_VALUE);
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void value(SSDNode node) {
		control.getSelectionModel().select(((SSDObject) node).getFormattedValue().value());
	}
	
	@Override
	public Object value() {
		return control.getSelectionModel().getSelectedItem();
	}
}