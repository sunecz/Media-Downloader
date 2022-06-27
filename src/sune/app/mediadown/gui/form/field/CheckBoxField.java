package sune.app.mediadown.gui.form.field;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public class CheckBoxField extends FormField {
	
	private final CheckBox control;
	
	public CheckBoxField(String name, String title) {
		super(name, title);
		control = new CheckBox();
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void setValue(SSDValue value, SSDType type) {
		control.setSelected(value.booleanValue());
	}
	
	@Override
	public Object getValue() {
		return control.isSelected();
	}
}