package sune.app.mediadown.gui.window;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.download.Download;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.gui.Dialog;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.gui.ProgressWindow;
import sune.app.mediadown.gui.ProgressWindow.ProgressAction;
import sune.app.mediadown.gui.ProgressWindow.ProgressContext;
import sune.app.mediadown.gui.control.IconTableCell;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.PluginUpdater;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.MathUtils;

/** @since 00.02.09 */
public class PluginManagerWindow extends DraggableWindow<VBox> {
	
	public static final String NAME = "plugin_manager";
	
	private final TableView<PluginFile> table;
	private final Button btnUpdateAll;
	
	public PluginManagerWindow() {
		super(NAME, new VBox(5.0), 650.0, 500.0);
		initModality(Modality.APPLICATION_MODAL);
		
		table = new TableView<>();
		
		TableColumn<PluginFile, String> columnIcon = new TableColumn<>("");
		TableColumn<PluginFile, String> columnTitle = new TableColumn<>(tr("table.column.title"));
		TableColumn<PluginFile, String> columnVersion = new TableColumn<>(tr("table.column.version"));
		TableColumn<PluginFile, String> columnAuthor = new TableColumn<>(tr("table.column.author"));
		TableColumn<PluginFile, String> columnUrl = new TableColumn<>(tr("table.column.url"));
		
		columnIcon.setCellFactory((v) -> new PluginIconTableCell());
		columnIcon.setCellValueFactory((v) -> new SimpleStringProperty(v.getValue().getPlugin().instance().icon()));
		columnTitle.setCellValueFactory((v) -> new SimpleStringProperty(v.getValue().getInstance().getTitle()));
		columnVersion.setCellValueFactory((v) -> new SimpleStringProperty(v.getValue().getInstance().getVersion()));
		columnAuthor.setCellValueFactory((v) -> new SimpleStringProperty(v.getValue().getInstance().getAuthor()));
		columnUrl.setCellFactory((v) -> new PluginUrlTableCell());
		columnUrl.setCellValueFactory((v) -> new SimpleStringProperty(v.getValue().getInstance().getURL()));
		
		columnIcon.setPrefWidth(24.0);
		columnTitle.setPrefWidth(210.0);
		columnVersion.setPrefWidth(100.0);
		columnAuthor.setPrefWidth(60.0);
		columnUrl.setPrefWidth(160.0);
		
		columnIcon.setReorderable(false);
		columnIcon.setResizable(false);
		columnIcon.setSortable(false);
		columnTitle.setReorderable(false);
		columnVersion.setReorderable(false);
		columnAuthor.setReorderable(false);
		columnUrl.setReorderable(false);
		columnUrl.setSortable(false);
		
		table.getColumns().add(columnIcon);
		table.getColumns().add(columnTitle);
		table.getColumns().add(columnVersion);
		table.getColumns().add(columnAuthor);
		table.getColumns().add(columnUrl);
		
		table.getSortOrder().add(columnTitle);
		table.setEditable(false);
		table.setMaxWidth(Double.MAX_VALUE);
		VBox.setVgrow(table, Priority.ALWAYS);
		
		btnUpdateAll = new Button(tr("button.update_all"));
		btnUpdateAll.setOnAction((e) -> updateAll());
		
		HBox boxBottom = new HBox(5);
		HBox boxFill = new HBox();
		boxBottom.getChildren().addAll(boxFill, btnUpdateAll);
		boxBottom.setId("box-bottom");
		HBox.setHgrow(boxFill, Priority.ALWAYS);
		
		content.getChildren().addAll(table, boxBottom);
		content.setPadding(new Insets(10.0));
		
		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			
			if(parent != null) {
				centerWindow(parent);
			}
			
			loadItems();
		});
	}
	
	private final Collection<PluginFile> allItems() {
		return Plugins.allLoaded();
	}
	
	private final void loadItems() {
		table.getItems().addAll(allItems());
		table.sort();
	}
	
	private final void updateAll() {
		ProgressWindow.submitAction(this, new ActionUpdatePlugins(allItems()));
	}
	
	private static final class PluginIconTableCell extends IconTableCell<PluginFile, String> {
		
		@Override
		protected ImageView iconView(String value) {
			Image image = getTableRow().getItem().getInstance().getIcon();
			
			if(image == null) {
				return null;
			}
			
			ImageView view = new ImageView(image);
			view.setFitWidth(24.0);
			view.setFitHeight(24.0);
			return view;
		}
	}
	
	private static final class PluginUrlTableCell extends TableCell<PluginFile, String> {
		
		private Hyperlink hyperlink;
		
		private final void openUrl(URI uri) {
			try {
				OS.current().browse(uri);
			} catch(IOException ex) {
				// Ignore
			}
		}
		
		@Override
		protected void updateItem(String item, boolean empty) {
			if(Objects.equals(item, getItem())) {
				return;
			}
			
			super.updateItem(item, empty);
			
			if(item == null) {
				setText(null);
				setGraphic(null);
			} else {
				if(hyperlink == null) {
					hyperlink = new Hyperlink(item);
					hyperlink.setPadding(Insets.EMPTY);
					hyperlink.setOnAction((e) -> {
						openUrl(Net.uri(((Hyperlink) e.getTarget()).getText()));
					});
				} else {
					hyperlink.setText(item);
				}
				
				setText(null);
				setGraphic(hyperlink);
			}
		}
	}
	
	private final class ActionUpdatePlugins implements ProgressAction {
		
		private final Collection<PluginFile> plugins;
		private final Translation translation = trtr("progress.update");
		private final AtomicBoolean cancelled = new AtomicBoolean();
		private ProgressContext context;
		private double pluginsCount;
		private Download download;
		
		protected ActionUpdatePlugins(Collection<PluginFile> plugins) {
			this.plugins = plugins;
		}
		
		private final boolean update(PluginFile pluginFile) {
			try {
				String pluginURL = PluginUpdater.check(pluginFile);
				
				// Check whether there is a newer version of the plugin
				if(pluginURL == null) {
					return false;
				}
				
				String pluginTitle = pluginFile.getPlugin().instance().title();
				download = PluginUpdater.update(pluginURL, Path.of(pluginFile.getPath()));
				
				download.addEventListener(DownloadEvent.BEGIN, (ctx) -> {
					context.setText(translation.getSingle("download.begin", "name", pluginTitle));
				});
				download.addEventListener(DownloadEvent.UPDATE, (ctx) -> {
					DownloadTracker tracker = (DownloadTracker) ctx.trackerManager().tracker();
					context.setText(translation.getSingle(
						"download.progress",
						"name",    pluginTitle,
						"current", tracker.current(),
						"total",   tracker.total(),
						"percent", MathUtils.round(tracker.progress() * 100.0, 2)
					));
				});
				download.addEventListener(DownloadEvent.ERROR, (ctx) -> {
					context.setText(translation.getSingle("download.error", "message", ctx.exception()));
				});
				download.addEventListener(DownloadEvent.END, (ctx) -> {
					context.setText(translation.getSingle("download.end", "name", pluginTitle));
				});
				
				download.start();
				return !download.isError();
			} catch(Exception ex) {
				// Ignore
			}
			
			return false;
		}
		
		private final void stopDownload() {
			try {
				if(download != null) {
					download.stop();
				}
			} catch(Exception ex) {
				// Ignore
			}
		}
		
		@Override
		public void action(ProgressContext context) {
			this.context = context;
			context.setText(translation.getSingle("init"));
			
			if(cancelled.get()) {
				return; // Do not continue
			}
			
			context.setProgress(ProgressContext.PROGRESS_NONE);
			pluginsCount = plugins.size();
			
			boolean updated = false;
			int ctr = 0;
			
			for(PluginFile pluginFile : plugins) {
				if(cancelled.get()) {
					stopDownload();
					break; // Do not continue
				}
				
				String pluginTitle = MediaDownloader.translation().getSingle(pluginFile.getPlugin().instance().title());
				context.setText(translation.getSingle("item_init", "name", pluginTitle));
				updated = update(pluginFile) || updated;
				context.setProgress(++ctr / pluginsCount);
				context.setText(translation.getSingle("item_done", "name", pluginTitle));
			}
			
			context.setText(translation.getSingle("done"));
			
			Dialog.showInfo(
				translation.getSingle("title"),
				translation.getSingle(updated ? "done_any" : "done_none")
			);
		}
		
		@Override
		public void cancel() {
			cancelled.set(true);
			context.setText(translation.getSingle("cancelled"));
		}
	}
}