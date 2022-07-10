package sune.app.mediadown.gui.window;

import java.net.URI;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.MediaGetters;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.util.ClipboardWatcher;
import sune.app.mediadown.util.ClipboardWatcher.ClipboardContents;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Utils;

/** @since 00.02.07 */
public class ClipboardWatcherWindow extends DraggableWindow<VBox> {
	
	public static final String NAME = "clipboard_watcher";
	
	private static final Pattern REGEX_TRAILING_END_SEPARATORS = Pattern.compile("[\\n\\r]+$");
	
	private final Label lblStatus;
	private final TextArea txtURLs;
	private final HBox boxBottom;
	private final Button btnSetState;
	private final Button btnGet;
	
	private ClipboardWatcher watcher;
	
	public ClipboardWatcherWindow() {
		super(NAME, new VBox(5.0), 450.0, 300.0);
		initModality(Modality.APPLICATION_MODAL);
		lblStatus = new Label();
		txtURLs = new TextArea();
		boxBottom = new HBox(5.0);
		btnSetState = new Button(translation.getSingle("button.set_state_enable"));
		btnGet = new Button(translation.getSingle("button.get"));
		
		btnSetState.setOnAction((e) -> toggleState());
		btnGet.setOnAction((e) -> sendData());
		
		txtURLs.setPromptText(translation.getSingle("placeholder.urls"));
		HBox boxFill = new HBox();
		HBox.setHgrow(boxFill, Priority.ALWAYS);
		boxBottom.getChildren().addAll(btnSetState, lblStatus, boxFill, btnGet);
		content.getChildren().addAll(txtURLs, boxBottom);
		content.setPadding(new Insets(10));
		btnSetState.setMinWidth(80);
		btnGet.setMinWidth(80);
		boxBottom.setAlignment(Pos.CENTER_LEFT);
		VBox.setVgrow(txtURLs, Priority.ALWAYS);
		
		setOnCloseRequest((e) -> setStateDisabled());
		
		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			if(parent != null) centerWindow(parent);
			// Must be added here since Media Getters are not available yet when creating this window
			txtURLs.setText("");
			txtURLs.requestFocus();
			setStateDisabled();
		});
	}
	
	private static final boolean isSupportedURI(URI uri) {
		return MediaGetters.fromURI(uri) != null;
	}
	
	private final void appendURI(URI uri) {
		if(!isSupportedURI(uri))
			return; // Add only supported URIs
		
		FXUtils.thread(() -> {
			StringBuilder builder = new StringBuilder();
			String text = txtURLs.getText();
			
			// Remove trailing end separators, if they exist
			Matcher matcher = REGEX_TRAILING_END_SEPARATORS.matcher(text);
			if(matcher.find()) txtURLs.deleteText(matcher.start(), matcher.end());
			
			// Put the new text on a new line, if there is already some text
			if(!txtURLs.getText().isEmpty()) {
				builder.append(System.lineSeparator());
			}
			
			builder.append(uri.toString());
			txtURLs.appendText(builder.toString());
		});
	}
	
	private final void clipboardContentsChanged(Observable o, ClipboardContents ov, ClipboardContents contents) {
		DataFormat format = contents.format();
		Object value = contents.value();
		
		if(format == DataFormat.URL) {
			appendURI((URI) value);
		} else if(format == DataFormat.PLAIN_TEXT) {
			// Check for multiple lines of URIs
			String text = (String) value;
			text.lines()
				.filter(Utils::isValidURL)
				.map(Utils::uri)
				.forEach(this::appendURI);
		}
	}
	
	private final ClipboardWatcher createClipboardWatcher() {
		ClipboardWatcher watcher = ClipboardWatcher.instance();
		watcher.contentsProperty().addListener(this::clipboardContentsChanged);
		return watcher;
	}
	
	private final void clearClipboardWatcher(ClipboardWatcher watcher) {
		watcher.contentsProperty().removeListener(this::clipboardContentsChanged);
	}
	
	private final void setStatusText(String status) {
		FXUtils.thread(() -> lblStatus.setText(translation.getSingle("label.status", "status", status)));
	}
	
	private final void setStateButtonText(String text) {
		FXUtils.thread(() -> btnSetState.setText(text));
	}
	
	private final void toggleState() {
		if(watcher == null || !watcher.isActive()) setStateEnabled();
		else                                       setStateDisabled();
	}
	
	private final void setStateEnabled() {
		if(watcher == null) {
			watcher = createClipboardWatcher();
		}
		watcher.start();
		setStatusText(translation.getSingle("status.enabled"));
		setStateButtonText(translation.getSingle("button.set_state_disable"));
	}
	
	private final void setStateDisabled() {
		if(watcher != null) {
			watcher.stop();
			clearClipboardWatcher(watcher);
		}
		setStatusText(translation.getSingle("status.disabled"));
		setStateButtonText(translation.getSingle("button.set_state_enable"));
	}
	
	private final List<String> nonEmptyURLs() {
		return Stream.of(txtURLs.getText().split("\\r?\\n"))
					.map(String::trim)
					.filter(Predicate.not(String::isEmpty))
					.distinct()
					.collect(Collectors.toList());
	}
	
	private static final MediaGetterWindow mediaGetterWindow() {
		return MediaDownloader.window(MediaGetterWindow.NAME);
	}
	
	private final void sendData() {
		List<String> urls = nonEmptyURLs();
		
		if(urls.isEmpty()) {
			FXUtils.showErrorWindow(this, translation.getSingle("error.title"),
			                              translation.getSingle("error.url_empty"));
			return;
		}
		
		if(urls.size() == 1) {
			String url = urls.get(0);
			if(!Utils.isValidURL(url)) {
				FXUtils.showErrorWindow(this, translation.getSingle("error.title"),
				                              translation.getSingle("error.url_invalid"));
				return;
			}
			mediaGetterWindow().showSelectionWindow(Utils.uri(url));
		} else {
			List<URI> uris = urls.stream().filter(Utils::isValidURL).map(Utils::uri).collect(Collectors.toList());
			mediaGetterWindow().showSelectionWindow(uris);
		}
	}
}