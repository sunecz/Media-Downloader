package sune.app.mediadown.gui.window;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.concurrent.Threads;
import sune.app.mediadown.entity.MediaGetter;
import sune.app.mediadown.entity.MediaGetters;
import sune.app.mediadown.gui.Dialog;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.gui.Window;
import sune.app.mediadown.gui.control.ScrollableComboBox;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.gui.table.ResolvedMediaPipelineResult;
import sune.app.mediadown.gui.table.TablePipelineResult;
import sune.app.mediadown.gui.table.URIListPipelineResult;
import sune.app.mediadown.gui.table.URIListPipelineTask;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.resource.ResourceRegistry;
import sune.app.mediadown.task.ListTask;
import sune.app.mediadown.util.ClipboardUtils;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.01.27 */
public class MediaGetterWindow extends DraggableWindow<VBox> {
	
	public static final String NAME = "media_getter";
	
	private final TextArea txtURLs;
	private final HBox boxBottom;
	private final ComboBox<MediaGetter> cmbGetters;
	private final Button btnGet;
	
	public MediaGetterWindow() {
		super(NAME, new VBox(5.0), 450.0, 300.0);
		initModality(Modality.APPLICATION_MODAL);
		txtURLs = new TextArea();
		
		boxBottom = new HBox(5.0);
		cmbGetters = new ScrollableComboBox<>();
		btnGet = new Button(translation.getSingle("buttons.get"));
		
		btnGet.setOnAction((e) -> showSelectionWindow());
		txtURLs.textProperty().addListener((o, ov, nv) -> {
			List<String> urls = nonEmptyURLs();
			boolean isSingle = urls.size() == 1;
			
			if(isSingle) {
				URI uri = Ignore.call(() -> Net.uri(urls.get(0)));
				
				if(uri != null && uri.isAbsolute()) {
					MediaGetter getter = MediaGetters.fromURI(uri);
					
					if(getter != null) {
						cmbGetters.getSelectionModel().select(getter);
					}
				}
			} else {
				cmbGetters.getSelectionModel().selectFirst();
			}
		});
		cmbGetters.setCellFactory((v) -> new IconListCell<>(MediaGetter::icon));
		cmbGetters.setButtonCell(new IconListCell<>(MediaGetter::icon));
		
		txtURLs.setPromptText(translation.getSingle("placeholder.urls"));
		txtURLs.setFont(Font.font("monospaced", txtURLs.getFont().getSize()));
		cmbGetters.setMaxWidth(Double.MAX_VALUE);
		boxBottom.getChildren().addAll(cmbGetters, btnGet);
		content.getChildren().addAll(txtURLs, boxBottom);
		content.setPadding(new Insets(10));
		btnGet.setMinWidth(80);
		VBox.setVgrow(txtURLs, Priority.ALWAYS);
		HBox.setHgrow(cmbGetters, Priority.ALWAYS);
		
		// Try to paste clipboard content (URLs) to the input field when the window is focused.
		focusedProperty().addListener((o, ov, isFocused) -> {
			if(isFocused) {
				pasteClipboardContent();
			}
		});
		
		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			if(parent != null) centerWindow(parent);
			// Must be added here since Media Getters are not available yet when creating this window
			cmbGetters.getItems().clear();
			cmbGetters.getItems().add(AutomaticMediaGetter.instance());
			cmbGetters.getItems().addAll(
				MediaGetters.all().stream()
					.sorted(Utils.OfString.comparingNormalized(MediaGetter::title))
					.collect(Collectors.toList())
			);
			cmbGetters.getSelectionModel().selectFirst();
			txtURLs.setText("");
			txtURLs.requestFocus();
			pasteClipboardContent();
		});
	}
	
	private final List<String> nonEmptyURLs() {
		return Stream.of(Regex.of("\\r?\\n").split(txtURLs.getText()))
					.map(String::trim)
					.filter(Predicate.not(String::isEmpty))
					.distinct()
					.collect(Collectors.toList());
	}
	
	/** @since 00.02.07 */
	private final void pasteClipboardContent() {
		// Paste the clipboard contents automatically only if the input field is empty
		if(!txtURLs.getText().isBlank())
			return;
		
		StringBuilder builder = new StringBuilder();
		ClipboardUtils.uris().stream()
			.filter((uri) -> MediaGetters.fromURI(uri) != null)
			.map((uri) -> uri.toString() + '\n')
			.forEachOrdered(builder::append);
		txtURLs.setText(builder.toString());
		txtURLs.positionCaret(txtURLs.getLength());
	}
	
	/** @since 00.02.07 */
	private final void doTask(Window<?> parent, MediaGetter getter, URI uri, Consumer<Boolean> onFinish) {
		boolean shouldClose = Ignore.defaultValue(() -> {
			TableWindow tableWindow = MediaDownloader.window(TableWindow.NAME);
			TablePipelineResult<?> result = tableWindow.show(parent, getter, uri);
			boolean isTerminating = result.isTerminating() && result instanceof ResolvedMediaPipelineResult;
			
			if(isTerminating) {
				MainWindow mainWindow = MainWindow.getInstance();
				List<ResolvedMedia> resolvedMedia = ((ResolvedMediaPipelineResult) result).getValue();
				resolvedMedia.forEach(mainWindow::addDownload);
			}
			
			return isTerminating;
		}, false, MediaDownloader::error);
		
		if(onFinish != null) {
			onFinish.accept(shouldClose);
		}
	}
	
	/** @since 00.02.07 */
	private final void doTask(Window<?> parent, List<URI> uris, Consumer<Boolean> onFinish) {
		boolean shouldClose = Ignore.defaultValue(() -> {
			URIListPipelineTask task = new URIListPipelineTask(parent, uris);
			
			Pipeline pipeline = Pipeline.create();
			pipeline.addTask(task);
			pipeline.start();
			pipeline.waitFor();
			PipelineResult result = pipeline.getResult();
			boolean isTerminating = result.isTerminating() && result instanceof URIListPipelineResult;
			
			List<URI> errors = task.errors();
			if(!errors.isEmpty()) {
				showDialogOfUnsupportedUris(errors);
			}
			
			if(isTerminating) {
				MainWindow mainWindow = MainWindow.getInstance();
				List<ResolvedMedia> resolvedMedia = ((URIListPipelineResult) result).getValue();
				resolvedMedia.forEach(mainWindow::addDownload);
			}
			
			return isTerminating;
		}, false, MediaDownloader::error);
		
		if(onFinish != null) {
			onFinish.accept(shouldClose);
		}
	}
	
	/** @since 00.02.09 */
	private final void showDialogOfUnsupportedUris(List<URI> uris) {
		Translation tr = translation.getTranslation("errors.unsupported_urls");
		String content = uris.stream().map((u) -> u.toString() + '\n').reduce("", (a, b) -> a + b);
		Dialog.showContentError(tr.getSingle("title"), tr.getSingle("description"), content);
	}
	
	private final void showSelectionWindow() {
		List<String> urls = nonEmptyURLs();
		
		if(urls.isEmpty()) {
			FXUtils.showErrorWindow(this, translation.getSingle("errors.title"), translation.getSingle("errors.url_empty"));
			return;
		}
		
		if(urls.size() == 1) {
			MediaGetter getter = cmbGetters.getValue();
			URI uri = Ignore.call(() -> Net.uri(urls.get(0)));
			
			if(uri == null || !uri.isAbsolute()) {
				FXUtils.showErrorWindow(this, translation.getSingle("errors.title"), translation.getSingle("errors.url_invalid"));
				return;
			}
			
			showSelectionWindow(getter, uri);
		} else {
			showSelectionWindowURLs(urls);
		}
	}
	
	/** @since 00.02.08 */
	private final void showSelectionWindow(Window<?> parent, Function<URI, MediaGetter> getterSupplier, URI uri,
			Consumer<Boolean> onFinish) {
		if(uri == null || !uri.isAbsolute()) {
			return;
		}
		
		MediaGetter getter = getterSupplier.apply(uri);
		
		if(getter == null || getter instanceof AutomaticMediaGetter) {
			showDialogOfUnsupportedUris(List.of(uri));
			return;
		}
		
		Threads.execute(() -> doTask(parent, getter, uri, onFinish));
	}
	
	private final void showSelectionWindow(MediaGetter getter, URI uri) {
		showSelectionWindow(this, (u) -> getter, uri, (shouldClose) -> {
			if(shouldClose) FXUtils.thread(this::close);
		});
	}
	
	private final void showSelectionWindowURLs(List<String> urls) {
		showSelectionWindow(
			urls.stream()
				.map((u) -> Ignore.call(() -> Net.uri(u)))
				.filter(Objects::nonNull)
				.filter(URI::isAbsolute)
				.collect(Collectors.toList())
		);
	}
	
	/** @since 00.02.07 */
	private final void showSelectionWindow(List<URI> uris) {
		showSelectionWindow(this, uris, (shouldClose) -> {
			if(shouldClose) FXUtils.thread(this::close);
		});
	}
	
	/** @since 00.02.07 */
	public final void showSelectionWindow(Window<?> parent, URI uri, Consumer<Boolean> onFinish) {
		showSelectionWindow(parent, MediaGetters::fromURI, uri, onFinish);
	}
	
	/** @since 00.02.07 */
	public final void showSelectionWindow(Window<?> parent, List<URI> uris, Consumer<Boolean> onFinish) {
		List<URI> uniqueURIs = Utils.deduplicate(uris).stream()
			.filter(URI::isAbsolute)
			.collect(Collectors.toList());
		
		if(uniqueURIs.isEmpty()) {
			return;
		}
		
		Threads.execute(() -> doTask(parent, uniqueURIs, onFinish));
	}
	
	private static final class IconListCell<T> extends ListCell<T> {
		
		private static final double ICON_WIDTH = 16.0;
		private final Function<T, Image> iconSupplier;
		
		public IconListCell(Function<T, Image> iconSupplier) {
			this.iconSupplier = iconSupplier;
		}
		
		@Override
		protected void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);
			if(item == null || empty)
				return;
			setText(item.toString());
			Image icon;
			if(iconSupplier != null
					&& (icon = iconSupplier.apply(item)) != null) {
				setGraphic(FXUtils.toGraphics(icon, ICON_WIDTH));
			} else {
				setGraphic(null);
			}
		}
	}
	
	/** @since 00.02.07 */
	private static final class AutomaticMediaGetter implements MediaGetter {
		
		private static AutomaticMediaGetter INSTANCE;
		
		private final String title;
		private final Image icon;
		
		// Forbid anyone to create an instance of this class
		private AutomaticMediaGetter() {
			title = MediaDownloader.translation().getSingle("windows." + NAME + ".getter.automatic");
			icon = ResourceRegistry.icon("automatic.png");
		}
		
		public static final AutomaticMediaGetter instance() {
			return INSTANCE == null ? (INSTANCE = new AutomaticMediaGetter()) : INSTANCE;
		}
		
		@Override
		public ListTask<Media> getMedia(URI uri, Map<String, Object> data) throws Exception {
			// Do nothing
			return ListTask.empty();
		}
		
		@Override
		public boolean isCompatibleURI(URI uri) {
			return false;
		}
		
		@Override
		public String title() {
			return title;
		}
		
		@Override
		public String url() {
			return null;
		}
		
		@Override
		public String version() {
			return null;
		}
		
		@Override
		public String author() {
			return null;
		}
		
		@Override
		public Image icon() {
			return icon;
		}
		
		@Override
		public String toString() {
			return title;
		}
	}
}