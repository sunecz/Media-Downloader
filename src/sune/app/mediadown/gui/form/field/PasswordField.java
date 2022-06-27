package sune.app.mediadown.gui.form.field;

import javafx.scene.Node;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public class PasswordField extends FormField {
	
	private final javafx.scene.control.PasswordField control;
	
	public PasswordField(String name, String title) {
		super(name, title);
		control = new javafx.scene.control.PasswordField();
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void setValue(SSDValue value, SSDType type) {
		control.setText(type == SSDType.NULL ? "" : value.stringValue());
	}
	
	@Override
	public Object getValue() {
		return control.getText();
	}
}