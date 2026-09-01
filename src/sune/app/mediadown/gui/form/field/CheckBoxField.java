package sune.app.mediadown.gui.form.field;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.gui.form.FormFieldType;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;

public class CheckBoxField<T> extends FormField<T> {
	
	private final CheckBox control;
	
	public CheckBoxField(T property, String name, String title) {
		super(property, FormFieldType.CHECKBOX, name, title);
		control = new CheckBox();
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void value(SSDNode node) {
		control.setSelected(((SSDObject) node).getFormattedValue().booleanValue());
	}
	
	@Override
	public Object value() {
		return control.isSelected();
	}
}