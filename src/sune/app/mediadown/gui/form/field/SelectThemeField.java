package sune.app.mediadown.gui.form.field;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.resource.ResourceRegistry;
import sune.app.mediadown.theme.Theme;
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public class SelectThemeField<T> extends FormField<T> {
	
	private final ComboBox<Theme> control;
	
	public SelectThemeField(T property, String name, String title) {
		super(property, name, title);
		control = new ComboBox<>();
		control.getItems().setAll(ResourceRegistry.themes.values());
		control.setCellFactory((p) -> new ThemeCell());
		control.setButtonCell(new ThemeCell());
		control.setMaxWidth(Double.MAX_VALUE);
	}
	
	@Override
	public Node render(Form form) {
		return control;
	}
	
	@Override
	public void value(SSDValue value, SSDType type) {
		String name = Utils.removeStringQuotes(value.stringValue());
		control.getSelectionModel().select(ResourceRegistry.theme(name));
	}
	
	@Override
	public Object value() {
		return control.getSelectionModel().getSelectedItem().name();
	}
	
	/** @since 00.02.07 */
	private static final class ThemeCell extends ListCell<Theme> {
		
		private static final String themeToString(Theme theme) {
			return theme.title(MediaDownloader.Languages.currentLanguage().code());
		}
		
		@Override
		protected void updateItem(Theme item, boolean empty) {
			super.updateItem(item, empty);
			
			if(!empty) {
				setText(themeToString(item));
			} else {
				setText(null);
				setGraphic(null);
			}
		}
	}
}