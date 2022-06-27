package sune.app.mediadown.gui.form.field;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.gui.window.PreviewWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.MediaTitleFormats;
import sune.app.mediadown.media.MediaTitleFormats.NamedMediaTitleFormat;
import sune.app.mediadown.util.Previews;
import sune.app.mediadown.util.Previews.MediaTitleFormatPreviewMask;
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

/** @since 00.02.05 */
public class SelectMediaTitleFormatField extends FormField {
	
	private static Translation mediaNamingTranslation;
	private static MediaTitleFormatPreviewMask titleMask;
	
	private final HBox wrapper;
	private final ComboBox<NamedMediaTitleFormat> control;
	private final Button btnPreview;
	
	public SelectMediaTitleFormatField(String name, String title) {
		super(name, title);
		wrapper = new HBox(5.0);
		control = new ComboBox<>();
		btnPreview = new Button(MediaDownloader.translation().getSingle("generic.preview"));
		// Add all the registered languages in their order, but put the Automatic language to the top
		control.getItems().setAll(MediaTitleFormats.allNamed().values());
		control.setMaxWidth(Double.MAX_VALUE);
		control.setConverter(new NamedMediaTitleFormatStringConverter());
		// Add ID to the control, so we can find it elsewhere
		control.setId("select-media-title-format");
		btnPreview.setOnAction((e) -> openPreview());
		btnPreview.setMinWidth(60.0);
		HBox.setHgrow(control, Priority.ALWAYS);
		wrapper.getChildren().addAll(control, btnPreview);
	}
	
	private static final String formatToString(NamedMediaTitleFormat format) {
		if(mediaNamingTranslation == null) {
			mediaNamingTranslation = MediaDownloader.translation().getTranslation("media.naming");
		}
		
		if(titleMask == null) {
			titleMask = MediaTitleFormatPreviewMask.of(0b01111);
		}
		
		return Previews.preview(format.format(), titleMask, mediaNamingTranslation);
	}
	
	private final void openPreview() {
		PreviewWindow window = MediaDownloader.window(PreviewWindow.NAME);
		Stage parent = (Stage) control.getScene().getWindow();
		window.setArgs("parent", parent);
		window.showPreview(control.getSelectionModel().getSelectedItem().format());
	}
	
	@Override
	public Node render(Form form) {
		return wrapper;
	}
	
	@Override
	public void setValue(SSDValue value, SSDType type) {
		String name = Utils.removeStringQuotes(value.stringValue());
		control.getSelectionModel().select(MediaTitleFormats.namedOfName(name));
	}
	
	@Override
	public Object getValue() {
		return control.getSelectionModel().getSelectedItem().name();
	}
	
	private static final class NamedMediaTitleFormatStringConverter extends StringConverter<NamedMediaTitleFormat> {
		
		private final Map<String, NamedMediaTitleFormat> mapping;
		
		private NamedMediaTitleFormatStringConverter() {
			this.mapping = MediaTitleFormats.allNamed().values().stream()
					.collect(Collectors.toMap((q) -> toString(q), Function.identity()));
		}
		
		@Override
		public String toString(NamedMediaTitleFormat value) {
			return formatToString(value);
		}
		
		@Override
		public NamedMediaTitleFormat fromString(String string) {
			return mapping.get(string);
		}
	}
}