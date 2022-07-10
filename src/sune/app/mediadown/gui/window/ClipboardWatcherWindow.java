package sune.app.mediadown.gui.window;

import java.net.URI;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
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
	
	private final ChangeListener<ClipboardContents> listener = this::clipboardContentsChanged;
	
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
		
		setOnCloseRequest((e) -> ensureDisabled());
		
		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			if(parent != null) centerWindow(parent);
			// Must be added here since Media Getters are not available yet when creating this window
			txtURLs.setText("");
			txtURLs.requestFocus();
			ensureEnabled();
		});
	}
	
	private static final boolean isSupportedURI(URI uri) {
		return MediaGetters.fromURI(uri) != null;
	}
	
	private static final MediaGetterWindow mediaGetterWindow() {
		return MediaDownloader.window(MediaGetterWindow.NAME);
	}
	
	private static final boolean autoEnableClipboardWatcher() {
		return MediaDownloader.configuration().autoEnableClipboardWatcher();
	}
	
	private final void ensureDisabled() {
		// Don't disable the watcher till the user disables it manually,
		// if the watcher is automatically enabled.
		if(!autoEnableClipboardWatcher()) disable();
	}
	
	private final void ensureEnabled() {
		if(!isActive()) enable();
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
				// Use the new line character rather than System#lineSeparator
				builder.append("\n");
			}
			
			builder.append(uri.toString());
			txtURLs.appendText(builder.toString());
		});
	}
	
	private final void ensureWindowIsShowing() {
		FXUtils.thread(() -> {
			if(!isShowing())
				show(MainWindow.getInstance());
		});
	}
	
	private final void clipboardContentsChanged(Observable o, ClipboardContents ov, ClipboardContents contents) {
		DataFormat format = contents.format();
		Object value = contents.value();
		
		if(format == DataFormat.URL) {
			ensureWindowIsShowing();
			appendURI((URI) value);
			return; // We're done, do not continue
		}
		
		if(format != DataFormat.PLAIN_TEXT
				&& format != DataFormat.HTML)
			return; // Nothing to do
		
		// Should always be string (see ClipboardWatcher#ensureString)
		String text = (String) value;
		
		if(format == DataFormat.HTML) {
			Document document = Utils.parseDocument((String) value);
			StringBuilder builder = new StringBuilder();
			
			// Edge browser copy links from the URL bar as an HTML anchor tag.
			// Probe the content for anchor tags and extract their hrefs.
			for(Element elLink : document.select("a")) {
				// Use the new line character rather than System#lineSeparator
				builder.append("\n");
				builder.append(elLink.attr("href"));
			}
			
			// Always add the textual content of the document,
			// maybe there's an URL.
			builder.append("\n" + document.text());
			
			text = builder.toString();
		}
		
		// Check for multiple lines of URIs
		List<URI> uris = text.lines()
			.map(String::trim)
			.filter(Predicate.not(String::isEmpty))
			.filter(Utils::isValidURL)
			.map(Utils::uri)
			.collect(Collectors.toList());
		
		if(!uris.isEmpty()) {
			ensureWindowIsShowing();
			uris.forEach(this::appendURI);
		}
	}
	
	private final ClipboardWatcher clipboardWatcher() {
		return ClipboardWatcher.instance();
	}
	
	private final void setStatusText(String status) {
		FXUtils.thread(() -> lblStatus.setText(translation.getSingle("label.status", "status", status)));
	}
	
	private final void setStateButtonText(String text) {
		FXUtils.thread(() -> btnSetState.setText(text));
	}
	
	private final boolean isActive() {
		return clipboardWatcher().isActive();
	}
	
	private final void toggleState() {
		if(!isActive()) enable(); else disable();
	}
	
	private final void startClipboardWatcher(ClipboardWatcher watcher) {
		if(isActive()) return; // Already active
		watcher.contentsProperty().addListener(listener);
		watcher.start();
	}
	
	private final void stopClipboardWatcher(ClipboardWatcher watcher) {
		if(!isActive()) return; // Not active
		watcher.stop();
		watcher.contentsProperty().removeListener(listener);
	}
	
	private final List<String> nonEmptyURLs() {
		return Stream.of(txtURLs.getText().split("\\r?\\n"))
					.map(String::trim)
					.filter(Predicate.not(String::isEmpty))
					.distinct()
					.collect(Collectors.toList());
	}
	
	private final void closeIfTrue(boolean shouldClose) {
		if(shouldClose) FXUtils.thread(this::close);
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
			mediaGetterWindow().showSelectionWindow(this, Utils.uri(url), this::closeIfTrue);
		} else {
			List<URI> uris = Utils.deduplicate(
				urls.stream()
					.filter(Utils::isValidURL)
					.map(Utils::uri)
					.collect(Collectors.toList())
			);
			mediaGetterWindow().showSelectionWindow(this, uris, this::closeIfTrue);
		}
	}
	
	public final void enable() {
		startClipboardWatcher(clipboardWatcher());
		setStatusText(translation.getSingle("status.enabled"));
		setStateButtonText(translation.getSingle("button.set_state_disable"));
	}
	
	public final void disable() {
		stopClipboardWatcher(clipboardWatcher());
		setStatusText(translation.getSingle("status.disabled"));
		setStateButtonText(translation.getSingle("button.set_state_enable"));
	}
}