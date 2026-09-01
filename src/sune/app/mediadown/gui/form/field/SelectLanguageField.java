package sune.app.mediadown.gui.form.field;

import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.gui.form.FormFieldType;
import sune.app.mediadown.language.Language;
import sune.app.mediadown.resource.ResourceRegistry;
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;

public class SelectLanguageField<T> extends FormField<T> {
	
	private final ComboBox<Language> control;
	
	public SelectLanguageField(T property, String name, String title) {
		super(property, FormFieldType.SELECT, name, title);
		control = new ComboBox<>();
		// Add all the registered languages in their order, but put the Automatic language to the top
		control.getItems().setAll(ResourceRegistry.languages.values().stream()
		                                          .sorted((a, b) -> a.name().equals("auto") ? -1 : 0)
		                                          .collect(Collectors.toList()));
		control.setMaxWidth(Double.MAX_VALUE);
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void value(SSDNode node) {
		String name = Utils.removeStringQuotes(((SSDObject) node).getFormattedValue().stringValue());
		control.getSelectionModel().select(ResourceRegistry.language(name));
	}
	
	@Override
	public Object value() {
		return control.getSelectionModel().getSelectedItem().name();
	}
}