package sune.app.mediadown.gui.form.field;

import javafx.scene.Node;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public class PasswordField<T> extends FormField<T> {
	
	private final javafx.scene.control.PasswordField control;
	
	public PasswordField(T property, String name, String title) {
		super(property, name, title);
		control = new javafx.scene.control.PasswordField();
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void value(SSDValue value, SSDType type) {
		control.setText(type == SSDType.NULL ? "" : value.stringValue());
	}
	
	@Override
	public Object value() {
		return control.getText();
	}
}