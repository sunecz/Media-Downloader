package sune.app.mediadown.gui.form.field;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.resource.ResourceRegistry;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public class SelectThemeField extends FormField {
	
	private final ComboBox<Theme> control;
	
	public SelectThemeField(String name, String title) {
		super(name, title);
		control = new ComboBox<>();
		control.getItems().setAll(ResourceRegistry.themes.values());
		control.setMaxWidth(Double.MAX_VALUE);
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void setValue(SSDValue value, SSDType type) {
		String name = Utils.removeStringQuotes(value.stringValue());
		control.getSelectionModel().select(ResourceRegistry.theme(name));
	}
	
	@Override
	public Object getValue() {
		return control.getSelectionModel().getSelectedItem().getName();
	}
}