package sune.app.mediadown.gui.form.field;

import javafx.scene.Node;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.gui.form.FormFieldType;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;

public class TextField<T> extends FormField<T> {
	
	private final javafx.scene.control.TextField control;
	
	public TextField(T property, String name, String title) {
		super(property, FormFieldType.TEXT, name, title);
		control = new javafx.scene.control.TextField();
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void value(SSDNode node) {
		control.setText(((SSDObject) node).getFormattedValue().stringValue());
	}
	
	@Override
	public Object value() {
		return control.getText();
	}
}