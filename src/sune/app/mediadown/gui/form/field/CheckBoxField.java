package sune.app.mediadown.gui.form.field;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public class CheckBoxField<T> extends FormField<T> {
	
	private final CheckBox control;
	
	public CheckBoxField(T property, String name, String title) {
		super(property, name, title);
		control = new CheckBox();
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void value(SSDValue value, SSDType type) {
		control.setSelected(value.booleanValue());
	}
	
	@Override
	public Object value() {
		return control.isSelected();
	}
}