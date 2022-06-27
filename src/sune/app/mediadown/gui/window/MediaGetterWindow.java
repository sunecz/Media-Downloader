package sune.app.mediadown.gui.window;

import java.util.function.Function;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.MediaGetter;
import sune.app.mediadown.MediaGetters;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.gui.control.FixedTextField;
import sune.app.mediadown.gui.control.ScrollableComboBox;
import sune.app.mediadown.gui.table.ResolvedMediaPipelineResult;
import sune.app.mediadown.gui.table.TablePipelineResult;
import sune.app.mediadown.server.Server;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Threads;
import sune.app.mediadown.util.Utils;

/** @since 00.01.27 */
public class MediaGetterWindow extends DraggableWindow<HBox> {
	
	public static final String NAME = "media_getter";
	
	private static final class IconListCell<T> extends ListCell<T> {
		
		private static final double ICON_WIDTH = 16.0;
		private final Function<T, Image> iconSupplier;
		
		public IconListCell(Function<T, Image> iconSupplier) {
			this.iconSupplier = iconSupplier;
		}
		
		@Override
		protected void updateItem(T item, boolean empty) {
			super.updateItem(item, empty);
			if((item == null || empty))
				return;
			setText(item.toString());
			Image icon;
			if((iconSupplier != null
					&& (icon = iconSupplier.apply(item)) != null)) {
				setGraphic(FXUtils.toGraphics(icon, ICON_WIDTH));
			}
		}
	}
	
	private ComboBox<MediaGetter> cmbGetters;
	private TextField txtURL;
	private Button btnGet;
	
	public MediaGetterWindow() {
		super(NAME, new HBox(5), 500.0, 160.0);
		initModality(Modality.APPLICATION_MODAL);
		cmbGetters = new ScrollableComboBox<>();
		txtURL = new FixedTextField();
		btnGet = new Button(translation.getSingle("buttons.get"));
		btnGet.setOnAction((e) -> showSelectionWindow());
		txtURL.textProperty().addListener((o, ov, nv) -> {
			MediaGetter getter = MediaGetters.fromURL(nv);
			if((getter != null)) {
				cmbGetters.getSelectionModel().select(getter);
			}
		});
		txtURL.addEventFilter(KeyEvent.KEY_PRESSED, (e) -> {
			if((e.getCode() == KeyCode.ENTER)) btnGet.fire();
		});
		cmbGetters.setCellFactory(this::cmbGettersCellFactory);
		cmbGetters.setButtonCell(new IconListCell<>(this::getIcon));
		content.getChildren().addAll(cmbGetters, txtURL, btnGet);
		content.setPadding(new Insets(10));
		btnGet.setMinWidth(80);
		HBox.setHgrow(txtURL, Priority.ALWAYS);
		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			if((parent != null)) {
				centerWindow(parent);
			}
			// Must be added here since Media Getters are not available yet when creating this window
			cmbGetters.getItems().setAll(MediaGetters.all());
			cmbGetters.getSelectionModel().select(0);
			txtURL.setText("");
			txtURL.requestFocus();
		});
	}
	
	private final Image getIcon(MediaGetter getter) {
		// Unfortunately we must use instanceof here
		if((getter instanceof MediaEngine)) return ((MediaEngine) getter).icon();
		if((getter instanceof Server))      return ((Server)      getter).icon();
		return null;
	}
	
	private final ListCell<MediaGetter> cmbGettersCellFactory(ListView<MediaGetter> view) {
		return new IconListCell<>(this::getIcon);
	}
	
	private final void showSelectionWindow() {
		MediaGetter getter = cmbGetters.getValue();
		String url = txtURL.getText();
		if((url == null || url.isEmpty())) {
			FXUtils.showErrorWindow(this, translation.getSingle("errors.title"), translation.getSingle("errors.url_empty"));
			return;
		}
		if(!Utils.isValidURL(url)) {
			FXUtils.showErrorWindow(this, translation.getSingle("errors.title"), translation.getSingle("errors.url_invalid"));
			return;
		}
		showSelectionWindow(getter, url);
	}
	
	public final void showSelectionWindow(MediaGetter getter, String url) {
		TableWindow window = MediaDownloader.window(TableWindow.NAME);
		Threads.execute(() -> {
			Utils.ignore(() -> {
				TablePipelineResult<?, ?> result = window.show(this, getter, url);
				if((result.isTerminating())) {
					MainWindow mainWindow = MainWindow.getInstance();
					((ResolvedMediaPipelineResult) result).getValue().forEach(mainWindow::addDownload);
				}
			}, MediaDownloader::error);
			FXUtils.thread(this::close);
	    });
	}
}