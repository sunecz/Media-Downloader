package sune.app.mediadown.gui.form.field;

import javafx.scene.Node;
import sune.app.mediadown.gui.control.IntegerTextField;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public class IntegerField<T> extends FormField<T> {
	
	private final IntegerTextField control;
	
	public IntegerField(T property, String name, String title) {
		super(property, name, title);
		control = new IntegerTextField();
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void value(SSDValue value, SSDType type) {
		control.setValue(value.intValue());
	}
	
	@Override
	public Object value() {
		return control.getValue();
	}
}