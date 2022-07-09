package sune.app.mediadown.gui.form.field;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.gui.form.Form;
import sune.app.mediadown.gui.form.FormField;
import sune.app.mediadown.gui.window.PreviewWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.MediaTitleFormat;
import sune.app.mediadown.media.MediaTitleFormat.ParseException;
import sune.app.mediadown.media.MediaTitleFormats;
import sune.app.mediadown.media.MediaTitleFormats.NamedMediaTitleFormat;
import sune.app.mediadown.util.FXUtils;
import sune.util.ssdf2.SSDType;
import sune.util.ssdf2.SSDValue;

public class TextFieldMediaTitleFormat<T> extends FormField<T> {
	
	/** @since 00.02.06 */
	private static final String URI_HELP = "https://projects.suneweb.net/media-downloader/docs/media-title-format/";
	
	private final VBox wrapper;
	/** @since 00.02.06 */
	private final HBox inline;
	private final javafx.scene.control.TextField control;
	private final Button btnOK;
	private final Button btnPreview;
	/** @since 00.02.06 */
	private final Hyperlink linkHelp;
	
	public TextFieldMediaTitleFormat(T property, String name, String title) {
		super(property, name, title);
		wrapper = new VBox(0.0);
		inline = new HBox(5.0);
		control = new javafx.scene.control.TextField();
		btnOK = new Button(MediaDownloader.translation().getSingle("generic.ok"));
		btnPreview = new Button(MediaDownloader.translation().getSingle("generic.preview"));
		Translation translation = MediaDownloader.translation().getTranslation("windows.configuration.fields.naming");
		linkHelp = new Hyperlink(translation.getSingle("customMediaTitleFormat_help"));
		control.textProperty().addListener((o, ov, nv) -> btnPreview.setDisable(nv == null || nv.isEmpty()));
		control.setFont(Font.font("monospaced", control.getFont().getSize()));
		btnOK.setOnAction((e) -> updateFormat());
		btnOK.setMinWidth(40.0);
		btnOK.heightProperty().addListener((o, ov, nv) -> control.setPrefHeight(nv.doubleValue())); // Fix control height
		btnPreview.setOnAction((e) -> openPreview());
		btnPreview.setMinWidth(60.0);
		btnPreview.setDisable(true);
		linkHelp.setOnAction((e) -> openHelpURI());
		HBox.setHgrow(control, Priority.ALWAYS);
		inline.getChildren().addAll(control, btnOK, btnPreview);
		wrapper.getChildren().addAll(inline, linkHelp);
	}
	
	/** @since 00.02.06 */
	private final void openHelpURI() {
		FXUtils.openURI(URI_HELP);
	}
	
	/** @since 00.02.06 */
	private final void ensureMediaFormatIsRegistered(String text) {
		Parent parent;
		if((parent = wrapper.getParent()) == null)
			return;
		
		MediaTitleFormat format = null;
		if(text != null && !text.isEmpty()) {
			format = createFormat(text);
		}
		
		@SuppressWarnings("unchecked")
		ComboBox<NamedMediaTitleFormat> cmb
			= (ComboBox<NamedMediaTitleFormat>) parent.lookup("#select-media-title-format");
		
		if(cmb == null) return; // Should always be non-null
		
		NamedMediaTitleFormat selected = cmb.getSelectionModel().getSelectedItem();
		List<NamedMediaTitleFormat> items = cmb.getItems();
		
		NamedMediaTitleFormat custom = items.stream()
				.filter((n) -> n.name().equals("custom"))
				.findFirst().orElse(null);
		if(custom != null) items.remove(custom);
		
		if(format != null) {
			MediaTitleFormats.register("custom", format);
			NamedMediaTitleFormat item = new NamedMediaTitleFormat("custom", format);
			items.add(item);
			
			if(selected == custom) {
				cmb.getSelectionModel().select(item);
			}
		} else {
			MediaTitleFormats.unregister("custom");
			
			if(selected == custom) {
				cmb.getSelectionModel().select(0);
			}
		}
	}
	
	private final MediaTitleFormat createFormat(String text) {
		try {
			return MediaTitleFormat.of(text);
		} catch(ParseException ex) {
			MediaDownloader.error(ex);
			return null;
		}
	}
	
	/** @since 00.02.06 */
	private final void updateFormat() {
		ensureMediaFormatIsRegistered(control.getText());
	}
	
	private final void openPreview() {
		String text = control.getText();
		if(text == null || text.isEmpty())
			return;
		
		MediaTitleFormat format = createFormat(text);
		if(format != null) {
			PreviewWindow window = MediaDownloader.window(PreviewWindow.NAME);
			Stage parent = (Stage) control.getScene().getWindow();
			window.setArgs("parent", parent);
			window.showPreview(format);
		}
	}
	
	@Override
	public Node render(Form form) {
		return wrapper;
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