package sune.app.mediadown.gui.window;

import static sune.app.mediadown.MediaDownloader.DATE;
import static sune.app.mediadown.MediaDownloader.VERSION;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sune.app.mediadown.Disposables;
import sune.app.mediadown.Download;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.MediaGetter;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.engine.MediaEngines;
import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.PipelineEvent;
import sune.app.mediadown.event.tracker.ConversionTracker;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.PlainTextTracker;
import sune.app.mediadown.event.tracker.Tracker;
import sune.app.mediadown.event.tracker.TrackerManager;
import sune.app.mediadown.event.tracker.TrackerVisitor;
import sune.app.mediadown.event.tracker.WaitTracker;
import sune.app.mediadown.gui.Dialog;
import sune.app.mediadown.gui.InformationItems.ItemDownloader;
import sune.app.mediadown.gui.InformationItems.ItemMediaEngine;
import sune.app.mediadown.gui.InformationItems.ItemPlugin;
import sune.app.mediadown.gui.InformationItems.ItemSearchEngine;
import sune.app.mediadown.gui.InformationItems.ItemServer;
import sune.app.mediadown.gui.ProgressWindow;
import sune.app.mediadown.gui.ProgressWindow.ProgressAction;
import sune.app.mediadown.gui.ProgressWindow.ProgressContext;
import sune.app.mediadown.gui.Window;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.gui.table.ResolvedMediaPipelineResult;
import sune.app.mediadown.gui.table.TablePipelineResult;
import sune.app.mediadown.gui.window.InformationWindow.InformationTab;
import sune.app.mediadown.gui.window.InformationWindow.TabContent;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.message.Message;
import sune.app.mediadown.message.MessageList;
import sune.app.mediadown.message.MessageManager;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.pipeline.MediaPipelineResult;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineMedia;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.PluginUpdater;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.MathUtils;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Threads;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.SizeUnit;

public final class MainWindow extends Window<BorderPane> {
	
	public static final String NAME = "main";
	
	private static MainWindow INSTANCE;
	
	private final AtomicBoolean closeRequest = new AtomicBoolean();
	private final Actions actions = new Actions();
	
	private TableView<PipelineInfo> table;
	private Button btnDownload;
	private Button btnDownloadSelected;
	private Button btnAdd;
	
	private ContextMenu menuTable;
	private MenuItem menuItemPause;
	private MenuItem menuItemTerminate;
	private MenuItem menuItemShowFile;
	
	private ContextMenu menuAdd;
	
	private MenuBar menuBar;
	private Menu menuApplication;
	private MenuItem menuItemInformation;
	private MenuItem menuItemConfiguration;
	private MenuItem menuItemMessages;
	private Menu menuTools;
	private MenuItem menuItemClipboardWatcher;
	private MenuItem menuItemUpdateResources;
	
	public MainWindow() {
		super(NAME, new BorderPane(), 750.0, 450.0);
		
		pane.setTop(initializeMenuBar());
		pane.setCenter(initializePipelinesTable());
		pane.setBottom(initializeButtons());
		
		setScene(scene);
	    setOnCloseRequest(this::internal_close);
	    setMinWidth(600);
	    setMinHeight(400);
	    setTitle(tr("title", "version", VERSION, "date", DATE));
	    setResizable(true);
	    centerOnScreen();
	    FXUtils.onWindowShowOnce(this, this::init);
	    FXUtils.onWindowShowOnce(this, this::showMessagesAsync);
	    FXUtils.onWindowShowOnce(this, this::maybeAutoEnableClipboardWatcher);
	    INSTANCE = this;
	}
	
	public static final MainWindow getInstance() {
		return INSTANCE;
	}
	
	private final void internal_close(WindowEvent e) {
		actions.terminate();
		maybeAutoDisableClipboardWatcher();
		
		List<Pipeline> pipelines = getPipelines();
		if(!pipelines.isEmpty()) {
			e.consume();
			if(!closeRequest.get()) {
				Threads.execute(() -> {
					internal_stopPipelines();
					FXUtils.thread(MediaDownloader::close);
				});
				closeRequest.set(true);
			}
		} else {
			MediaDownloader.close();
		}
	}
	
	private final void internal_stopPipelines() {
		List<Pipeline> pipelines = getPipelines();
		for(Pipeline pipeline : pipelines) {
			if((pipeline.isStarted() && pipeline.isRunning() || pipeline.isPaused())) {
				Utils.ignore(pipeline::stop, this::showError);
			}
		}
		pipelines.clear();
	}
	
	private final void init() {
		Disposables.add(() -> {
			getPipelines().forEach(Utils.suppressException(Pipeline::stop, this::showError));
		});
		prepareAddMenu();
	}
	
	/** @since 00.02.02 */
	private final boolean showMessages() {
		MessageList list = Utils.ignore(() -> MessageManager.current(), MessageManager.empty());
		String language = MediaDownloader.language().code();
		if(language.equalsIgnoreCase("auto"))
			language = MediaDownloader.Languages.localLanguage().code();
		List<Message> messages = list.difference(language, Utils.ignore(() -> MessageManager.local(), MessageManager.empty()));
		if(!messages.isEmpty()) {
			FXUtils.thread(() -> {
				MessageWindow window = MediaDownloader.window(MessageWindow.NAME);
				window.setArgs("messages", messages);
				window.show();
			});
			return true;
		}
		return false;
	}
	
	/** @since 00.02.04 */
	private final void showMessagesAsync() {
		actions.submit(new ProgressAction() {
			
			@Override
			public void action(ProgressContext context) {
				context.setProgress(ProgressContext.PROGRESS_INDETERMINATE);
				context.setText(tr("actions.messages.checking"));
				Utils.ignore(MainWindow.this::showMessages, MediaDownloader::error);
			}
			
			@Override public void cancel() { /* Do nothing */ }
		});
	}
	
	/** @since 00.02.02 */
	private final boolean resetAndShowMessages() {
		Utils.ignore(() -> MessageManager.deleteLocal(), MediaDownloader::error);
		return showMessages();
	}
	
	/** @since 00.02.04 */
	private final void resetAndShowMessagesAsync() {
		actions.submit(new ProgressAction() {
			
			@Override
			public void action(ProgressContext context) {
				context.setProgress(ProgressContext.PROGRESS_INDETERMINATE);
				context.setText(tr("actions.messages.checking"));
				if(!resetAndShowMessages()) {
					// Show the dialog in the next pulse so that the progress window can be closed
					FXUtils.thread(() -> {
						Translation dialogTranslation = trtr("dialogs.messages_empty");
						Dialog.showInfo(dialogTranslation.getSingle("title"), dialogTranslation.getSingle("text"));
					});
				}
			}
			
			@Override public void cancel() { /* Do nothing */ }
		});
	}
	
	/** @since 00.02.07 */
	private final void maybeAutoEnableClipboardWatcher() {
		if(MediaDownloader.configuration().autoEnableClipboardWatcher()) {
			ClipboardWatcherWindow window = MediaDownloader.window(ClipboardWatcherWindow.NAME);
			window.enable(); // Automatically enable the clipboard watcher
		}
	}
	
	/** @since 00.02.07 */
	private final void maybeAutoDisableClipboardWatcher() {
		if(MediaDownloader.configuration().autoEnableClipboardWatcher()) {
			ClipboardWatcherWindow window = MediaDownloader.window(ClipboardWatcherWindow.NAME);
			window.disable(); // Automatically disable the clipboard watcher
		}
	}
	
	private final void prepareAddMenu() {
		menuAdd = new ContextMenu();
		for(MediaEngine engine : MediaEngines.all()) {
			MenuItem item = new MenuItem(engine.title());
			ImageView icon = new ImageView(engine.icon());
			icon.setFitWidth(16);
			icon.setFitHeight(16);
			item.setGraphic(icon);
			item.getProperties().put("engine", engine);
			item.setOnAction((e) -> showSelectionWindow((MediaEngine) item.getProperties().get("engine")));
			menuAdd.getItems().add(item);
		}
		MenuItem itemMediaGetter = new MenuItem(tr("context_menus.add.items.media_getter"));
		itemMediaGetter.setOnAction((e) -> {
			MediaDownloader.window(MediaGetterWindow.NAME).show(this);
		});
		menuAdd.getItems().addAll(itemMediaGetter);
		prepareContextMenuForShowing(menuAdd);
	}
	
	/** @since 00.02.08 */
	private final <T> TableColumn<PipelineInfo, T> pipelinesTableColumn(String propertyName, String translationPath,
			double preferredWidth) {
		String title = translationPath != null ? tr(translationPath) : null;
		TableColumn<PipelineInfo, T> column = new TableColumn<>(title);
		column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
		column.setPrefWidth(preferredWidth);
		return column;
	}
	
	/** @since 00.02.08 */
	private final TableColumn<PipelineInfo, String> pipelinesTableColumnSource(String propertyName,
			String translationPath, double preferredWidth) {
		String title = translationPath != null ? tr(translationPath) : null;
		TableColumn<PipelineInfo, String> column = new TableColumn<>(title);
		column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
		column.setCellFactory((col) -> new IconTableCell());
		column.setPrefWidth(preferredWidth);
		return column;
	}
	
	/** @since 00.02.08 */
	private final TableColumn<PipelineInfo, Double> pipelinesTableColumnProgressBar(String propertyName,
			String translationPath, double preferredWidth) {
		String title = translationPath != null ? tr(translationPath) : null;
		TableColumn<PipelineInfo, Double> column = new TableColumn<>(title);
		column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
		column.setCellFactory((col) -> new ProgressBarTableCell());
		column.setPrefWidth(preferredWidth);
		return column;
	}
	
	/** @since 00.02.08 */
	private final TableView<PipelineInfo> initializePipelinesTable() {
		table = new TableView<>();
		table.setPlaceholder(new Label(tr("tables.main.placeholder")));
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		table.getColumns().addAll(List.of(
			pipelinesTableColumnSource("source", null, 24),
			pipelinesTableColumn("title", "tables.main.columns.title", 150),
			pipelinesTableColumnProgressBar("progress", "tables.main.columns.progress", 80),
			pipelinesTableColumn("state", "tables.main.columns.state", 100),
			pipelinesTableColumn("current", "tables.main.columns.current", 80),
			pipelinesTableColumn("total", "tables.main.columns.total", 80),
			pipelinesTableColumn("speed", "tables.main.columns.speed", 80),
			pipelinesTableColumn("timeLeft", "tables.main.columns.time_left", 90),
			pipelinesTableColumn("destination", "tables.main.columns.destination", 150),
			pipelinesTableColumn("information", "tables.main.columns.information", 150)
		));
		
		table.getItems().addListener((Change<? extends PipelineInfo> change) -> {
			while(change.next()) {
				boolean isDisabled = btnDownload.isDisable();
				boolean shouldDisable = isDisabled;
				
				if(change.wasAdded()) {
					shouldDisable = false;
				} else if(change.wasRemoved()) {
					shouldDisable = change.getList().stream()
						.map(PipelineInfo::pipeline)
						.allMatch(Pipeline::isStarted);
				}
				
				if(isDisabled != shouldDisable) {
					btnDownload.setDisable(shouldDisable);
				}
			}
		});
		
		table.getSelectionModel().getSelectedItems().addListener((Change<? extends PipelineInfo> change) -> {
			while(change.next()) {
				boolean isDisabled = btnDownloadSelected.isDisable();
				boolean shouldDisable = isDisabled;
				
				if(change.wasAdded() || change.wasRemoved()) {
					shouldDisable = change.getList().stream()
						.map(PipelineInfo::pipeline)
						.allMatch(Pipeline::isStarted);
				}
				
				if(isDisabled != shouldDisable) {
					btnDownloadSelected.setDisable(shouldDisable);
				}
			}
		});
		
		table.addEventHandler(MouseEvent.MOUSE_PRESSED, (e) -> {
			menuTable.hide(); // Fix: graphical glitch when the menu is already showing
			
			List<PipelineInfo> infos = table.getSelectionModel().getSelectedItems();
			
			if(infos.isEmpty()) {
				return; // Nothing to do
			}
			
			switch(e.getButton()) {
				case PRIMARY:
					if(e.getClickCount() > 1) {
						PipelineInfo info = infos.get(0);
						Pipeline pipeline = info.pipeline();
						
						if(pipeline.isStarted() || pipeline.isDone()) {
							showFile(info);
						}
					}
					
					break;
				case SECONDARY:
					int count = infos.size();
					int started = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isStarted).count();
					int done = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isDone).count();
					int stopped = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isStopped).count();
					
					menuItemTerminate.setText(
						anyTerminable(infos)
							? tr("context_menus.table.items.terminate_cancel")
							: tr("context_menus.table.items.terminate_remove")
					);
					
					menuItemPause.setDisable(started == 0 || (done == count || stopped == count));
					menuItemPause.setText(
						anyNonPaused(infos)
							? tr("context_menus.table.items.pause")
							: tr("context_menus.table.items.resume")
					);
					
					menuItemShowFile.setDisable(!(started > 0 || done > 0 || stopped > 0));
					menuTable.show(table, e.getScreenX(), e.getScreenY());
					break;
				default:
					// Do nothing
					break;
			}
		});
		
		initializePipelinesTableContextMenu();
		BorderPane.setMargin(table, new Insets(15, 15, 5, 15));
		
		return table;
	}
	
	/** @since 00.02.08 */
	private final ContextMenu initializePipelinesTableContextMenu() {
		menuTable = new ContextMenu();
		
		menuItemPause = new MenuItem(tr("context_menus.table.items.pause"));
		menuItemPause.setOnAction((e) -> {
			List<PipelineInfo> infos = table.getSelectionModel().getSelectedItems();
			
			if(infos.isEmpty()) {
				return; // Nothing to pause/resume
			}
			
			if(anyNonPaused(infos)) {
				for(PipelineInfo info : infos) {
					Utils.ignore(info.pipeline()::pause, this::showError);
				}
			} else {
				for(PipelineInfo info : infos) {
					Utils.ignore(info.pipeline()::resume, this::showError);
				}
			}
		});
		
		menuItemTerminate = new MenuItem(tr("context_menus.table.items.terminate_cancel"));
		menuItemTerminate.setOnAction((e) -> {
			List<PipelineInfo> infos = table.getSelectionModel().getSelectedItems();
			
			if(infos.isEmpty()) {
				return; // Nothing to terminate/remove
			}
			
			if(anyTerminable(infos)) {
				for(PipelineInfo info : infos) {
					Pipeline pipeline = info.pipeline();
					
					if(pipeline.isStarted() && (pipeline.isRunning() || pipeline.isPaused())) {
						Utils.ignore(pipeline::stop, this::showError);
					}
				}
			} else {
				removePipelines(new ArrayList<>(infos));
			}
		});
		
		menuItemShowFile = new MenuItem(tr("context_menus.table.items.show_file"));
		menuItemShowFile.setOnAction((e) -> {
			List<PipelineInfo> infos = table.getSelectionModel().getSelectedItems();
			
			if(infos.isEmpty()) {
				return; // Nothing to show file for
			}
			
			for(PipelineInfo info : infos) {
				showFile(info);
			}
		});
		
		menuTable.setAutoFix(true);
		menuTable.setAutoHide(true);
		menuTable.getItems().addAll(menuItemPause, menuItemTerminate, menuItemShowFile);
		
		return menuTable;
	}
	
	/** @since 00.02.08 */
	private final MenuBar initializeMenuBar() {
		menuBar = new MenuBar();
		menuApplication = new Menu(tr("menu_bar.application.title"));
		menuTools = new Menu(tr("menu_bar.tools.title"));
		
		menuItemInformation = new MenuItem(tr("menu_bar.application.item.information"));
		menuItemInformation.setOnAction((e) -> {
			showInformationWindow();
		});
		
		menuItemConfiguration = new MenuItem(tr("menu_bar.application.item.configuration"));
		menuItemConfiguration.setOnAction((e) -> {
			MediaDownloader.window(ConfigurationWindow.NAME).show(this);
		});
		
		menuItemMessages = new MenuItem(tr("menu_bar.application.item.messages"));
		menuItemMessages.setOnAction((e) -> resetAndShowMessagesAsync());
		
		menuItemClipboardWatcher = new MenuItem(tr("menu_bar.tools.item.clipboard_watcher"));
		menuItemClipboardWatcher.setOnAction((e) -> {
			MediaDownloader.window(ClipboardWatcherWindow.NAME).show(this);
		});
		
		menuItemUpdateResources = new MenuItem(tr("menu_bar.tools.item.update_resources"));
		menuItemUpdateResources.setOnAction((e) -> {
			Translation tr = trtr("dialogs.update_resources");
			if(Dialog.showPrompt(tr.getSingle("title"), tr.getSingle("text"))) {
				MediaDownloader.updateResources();
				
				// Show dialog with a success message. Currently any thrown error is shown to
				// the user in a dialog but is not taken into consideration here.
				Translation trDone = tr.getTranslation("success");
				Dialog.showInfo(trDone.getSingle("title"), trDone.getSingle("text"));
			}
		});
		
		menuApplication.getItems().addAll(menuItemInformation, menuItemConfiguration, menuItemMessages);
		menuTools.getItems().addAll(menuItemClipboardWatcher, menuItemUpdateResources);
		menuBar.getMenus().addAll(menuApplication, menuTools);
		
		return menuBar;
	}
	
	/** @since 00.02.08 */
	private final Pane initializeButtons() {
		btnDownload = new Button(tr("buttons.download"));
		btnDownload.setOnAction((e) -> {
			startPipelines(table.getItems());
			btnDownload.setDisable(true);
			btnDownloadSelected.setDisable(true);
		});
		
		btnDownloadSelected = new Button(tr("buttons.download_selected"));
		btnDownloadSelected.setOnAction((e) -> {
			startPipelines(table.getSelectionModel().getSelectedItems());
			btnDownloadSelected.setDisable(true);
		});
		
		btnAdd = new Button(tr("buttons.add"));
		btnAdd.setOnAction((e) -> {
			showContextMenuAtNode(menuAdd, btnAdd);
		});
		
		btnDownload.setDisable(true);
		btnDownloadSelected.setDisable(true);
		btnDownload.setMinWidth(80);
		btnDownloadSelected.setMinWidth(80);
		btnAdd.setMinWidth(80);
		
		HBox box = new HBox(5);
		box.setAlignment(Pos.CENTER_RIGHT);
		box.setPadding(new Insets(5, 0, 0, 0));
		HBox fillBox = new HBox();
		HBox.setHgrow(fillBox, Priority.ALWAYS);
		box.getChildren().addAll(btnAdd, fillBox, btnDownloadSelected, btnDownload);
		box.setPadding(new Insets(0, 15, 15, 15));
		
		return box;
	}
	
	private final ProgressAction action_updatePlugins(Stage window, Collection<PluginFile> plugins) {
		InformationWindow informationWindow = MediaDownloader.window(InformationWindow.NAME);
		Translation translation = informationWindow.getTranslation().getTranslation("tabs.plugins");
		return new ProgressAction() {
			
			private final AtomicBoolean cancelled = new AtomicBoolean();
			private ProgressContext context;
			private double pluginsCount;
			private Download downloadUpdate;
			
			private final boolean update(PluginFile pluginFile) {
				try {
					String pluginURL = PluginUpdater.check(pluginFile);
					// Check whether there is a newer version of the plugin
					if((pluginURL != null)) {
						String pluginTitle = pluginFile.getPlugin().instance().title();
						downloadUpdate = PluginUpdater.update(pluginURL, Path.of(pluginFile.getPath()));
						downloadUpdate.addEventListener(DownloadEvent.BEGIN,
							(data) -> context.setText(translation.getSingle("labels.update.download.begin", "name", pluginTitle)));
						downloadUpdate.addEventListener(DownloadEvent.UPDATE, (data) -> {
							DownloadTracker tracker = Utils.cast(data.b.tracker());
							String progress = translation.getSingle("labels.update.download.progress",
								"name",    pluginTitle,
								"current", tracker.current(),
								"total",   tracker.total(),
								"percent", MathUtils.round(tracker.progress() * 100.0, 2));
							context.setText(progress);
						});
						downloadUpdate.addEventListener(DownloadEvent.ERROR,
							(data) -> context.setText(translation.getSingle("labels.update.download.error", "message", data.b)));
						downloadUpdate.addEventListener(DownloadEvent.END,
						    (data) -> context.setText(translation.getSingle("labels.update.download.end", "name", pluginTitle)));
						downloadUpdate.start();
						return true;
					}
				} catch(Exception ex) {
					// Ignore
				}
				return false;
			}
			
			private final void stopDownload() {
				try {
					if((downloadUpdate != null))
						downloadUpdate.stop();
				} catch(Exception ex) {
					// Ignore
				}
			}
			
			@Override
			public void action(ProgressContext context) {
				this.context = context;
				context.setText(translation.getSingle("labels.update_many.init"));
				if((cancelled.get())) return;
				context.setProgress(ProgressContext.PROGRESS_NONE);
				pluginsCount = plugins.size();
				boolean updated = false;
				int ctr = 0;
				for(PluginFile pluginFile : plugins) {
					if((cancelled.get())) {
						stopDownload();
						break;
					}
					String pluginTitle = MediaDownloader.translation().getSingle(pluginFile.getPlugin().instance().title());
					context.setText(translation.getSingle("labels.update_many.item_init", "name", pluginTitle));
					updated = update(pluginFile) || updated;
					context.setProgress(++ctr / pluginsCount);
					context.setText(translation.getSingle("labels.update_many.item_done", "name", pluginTitle));
				}
				String title = translation.getSingle("labels.update_many.title");
				String text = translation.getSingle("labels.update_many." + (updated ? "done_any" : "done_none"));
				Dialog.showInfo(title, text);
				FXUtils.thread(window::close);
			}
			
			@Override
			public void cancel() {
				context.setText(translation.getSingle("labels.update_many.cancel"));
				cancelled.set(true);
			}
		};
	}
	
	private final void showInformationWindow() {
		InformationWindow window = MediaDownloader.window(InformationWindow.NAME);
		Translation translation = window.getTranslation().getTranslation("tabs");
		TabContent<?>[] tabs = {
			new TabContent<>(translation.getTranslation("plugins"), ItemPlugin.items()),
			new TabContent<>(translation.getTranslation("media_engines"), ItemMediaEngine.items()),
			new TabContent<>(translation.getTranslation("downloaders"), ItemDownloader.items()),
			new TabContent<>(translation.getTranslation("servers"), ItemServer.items()),
			new TabContent<>(translation.getTranslation("search_engines"), ItemSearchEngine.items()),
		};
		// Special buttons for the Plugins tab
		tabs[0].setOnInit((win) -> {
			InformationTab<ItemPlugin> tab = win.getSelectedTab();
			tab.getList().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
			Translation tabTranslation = translation.getTranslation("plugins");
			HBox boxBottom = new HBox(5);
			Button btnUpdateAll = new Button(tabTranslation.getSingle("buttons.update_all"));
			Button btnUpdateSelected = new Button(tabTranslation.getSingle("buttons.update_selected"));
			btnUpdateAll.setOnAction((e) -> {
				ProgressWindow.submitAction(window, action_updatePlugins(window, Plugins.allLoaded()));
			});
			btnUpdateSelected.setOnAction((e) -> {
				Collection<ItemPlugin> selectedItems = tab.getList().getSelectionModel().getSelectedItems();
				Collection<PluginFile> plugins = selectedItems.stream().map(ItemPlugin::getPlugin).collect(Collectors.toList());
				ProgressWindow.submitAction(window, action_updatePlugins(window, plugins));
			});
			HBox boxFill = new HBox();
			boxBottom.getChildren().addAll(boxFill, btnUpdateAll, btnUpdateSelected);
			boxBottom.setId("box-bottom");
			HBox.setHgrow(boxFill, Priority.ALWAYS);
			GridPane pane = (GridPane) tab.getContent();
			pane.getChildren().add(boxBottom);
			GridPane.setConstraints(boxBottom, 0, 2, 1, 1);
		});
		// Filter out empty tabs
		tabs = Arrays.asList(tabs).stream().filter((t) -> t.getItems().length > 0).toArray(TabContent[]::new);
		window.setArgs("tabs", tabs);
		window.show(this);
	}
	
	private final void prepareContextMenuForShowing(ContextMenu menu) {
		menu.showingProperty().addListener((o) -> {
			Map<Object, Object> props = menu.getProperties();
			if((menu.isShowing() && props.containsKey("firstShow"))) {
				Node node = (Node) props.get("node");
				double height = menu.getHeight();
				double screenX = (double) props.get("screenX");
				double screenY = (double) props.get("screenY") - height + 15.0;
				props.put("height", height);
				props.remove("firstShow");
				menu.hide();
				menu.show(node, screenX, screenY);
			}
		});
	}
	
	private final void showContextMenuAtNode(ContextMenu menu, Node node) {
		if((menu.isShowing())) return; // Fix: graphical glitch when the menu is already showing
		Bounds boundsLocal = node.getBoundsInLocal();
		Bounds boundsScreen = node.localToScreen(boundsLocal);
		double screenX = boundsScreen.getMinX();
		double screenY = boundsScreen.getMinY();
		Map<Object, Object> props = menu.getProperties();
		if((props.containsKey("height"))) {
			double height = (double) props.get("height");
			menu.show(node, screenX, screenY - height + 15.0);
		} else {
			props.put("firstShow", true);
			props.put("node",      node);
			props.put("screenX",   screenX);
			props.put("screenY",   screenY);
			menu.show(this);
		}
	}
	
	private final void showFile(PipelineInfo info) {
		Utils.ignore(() -> OS.current().highlight(info.media().path()), MediaDownloader::error);
	}
	
	/** @since 00.02.05 */
	private final PipelineInfo getPipelineInfo(ResolvedMedia media) {
		Pipeline pipeline = Pipeline.create();
		PipelineInfo info = new PipelineInfo(pipeline, media);
		TrackerVisitor visitor = new DefaultTrackerVisitor(info);
		
		pipeline.addEventListener(PipelineEvent.BEGIN, (o) -> {
			info.update(new PipelineInfoData.OfText(
				tr("progress.initialization")
			));
		});
		
		pipeline.addEventListener(PipelineEvent.END, (o) -> {
			if(pipeline.isError()) {
				return;
			}
			
			String state = pipeline.isStopped() ? "stopped" : "completed";
			info.update(new PipelineInfoData.OfEndText(
				state,
				tr("progress." + state),
				pipeline.isStopped() ? 0.0 : 1.0
			));
		});
		
		pipeline.addEventListener(PipelineEvent.ERROR, (o) -> {
			Pair<Pipeline, Exception> pair = (Pair<Pipeline, Exception>) o;
			info.update(new PipelineInfoData.OfEndText(
				"error",
				tr("progress.error", "text", pair.b.getMessage()),
				0.0
			));
			showError(pair.b);
		});
		
		pipeline.addEventListener(PipelineEvent.UPDATE, (p) -> {
			if(!pipeline.isRunning()) {
				return;
			}
			
			info.update(new PipelineInfoData.OfText(
				tr("progress.waiting")
			));
		});
		
		pipeline.getEventRegistry().addMany((o) -> {
			if(!pipeline.isRunning()) {
				return;
			}
			
			Tracker tracker = Utils.<Pair<?, TrackerManager>>cast(o).b.tracker();
			tracker.visit(visitor);
		}, DownloadEvent.UPDATE, ConversionEvent.UPDATE);
		
		return info;
	}
	
	private final void showError(Exception ex) {
		MediaDownloader.error(ex);
	}
	
	/** @since 00.02.08 */
	private final boolean anyNonPaused(List<PipelineInfo> infos) {
		return infos.stream().map(PipelineInfo::pipeline)
					.anyMatch((p) -> p.isStarted() && p.isRunning());
	}
	
	/** @since 00.02.08 */
	private final boolean anyTerminable(List<PipelineInfo> infos) {
		return infos.stream().map(PipelineInfo::pipeline)
					.anyMatch((p) -> p.isStarted() && (p.isRunning() || p.isPaused()));
	}
	
	private final List<Pipeline> getPipelines() {
		return table.getItems().stream().map(PipelineInfo::pipeline).filter((d) -> d != null).collect(Collectors.toList());
	}
	
	private final void startPipeline(PipelineInfo info) {
		Pipeline pipeline = info.pipeline();
		
		if(pipeline.isStarted()) {
			return;
		}
		
		ResolvedMedia media = info.media();
		PipelineMedia pipelineMedia = PipelineMedia.of(media.media(), media.path(), media.configuration(),
			DownloadConfiguration.ofDefault());
		PipelineResult<?> input = MediaPipelineResult.of(pipelineMedia);
		
		try {
			pipeline.setInput(input);
			pipeline.start();
			pipelineMedia.awaitSubmitted();
		} catch(Exception ex) {
			showError(ex);
		}
	}
	
	/** @since 00.02.08 */
	private final void startPipelines(List<PipelineInfo> infos) {
		List<PipelineInfo> notEnqueued = infos.stream()
			.filter(Predicate.not(PipelineInfo::isQueued))
			.collect(Collectors.toList());
		
		// Enqueue all the items, so that they can be sequentually added
		notEnqueued.stream().forEachOrdered((i) -> i.isQueued(true));
		
		// Start all items in a thread with sequential ordering
		Threads.executeEnsured(() -> {
			notEnqueued.stream().forEachOrdered(this::startPipeline);
		});
	}
	
	private final void removePipelines(List<PipelineInfo> infos) {
		infos.stream().map(PipelineInfo::pipeline)
			.filter((p) -> p != null)
			.forEach(Utils.suppressException(Pipeline::stop, this::showError));
		FXUtils.thread(() -> table.getItems().removeAll(infos));
	}
	
	public final void showSelectionWindow(MediaEngine engine) {
		TableWindow window = MediaDownloader.window(TableWindow.NAME);
		Threads.execute(() -> {
			Utils.ignore(() -> {
				TablePipelineResult<?, ?> result = window.show(this, engine);
				if(result.isTerminating()) {
					((ResolvedMediaPipelineResult) result).getValue().forEach(this::addDownload);
				}
			}, MediaDownloader::error);
	    });
	}
	
	/** @since 00.02.05 */
	public final void addDownload(ResolvedMedia media) {
		PipelineInfo info = getPipelineInfo(media);
		FXUtils.thread(() -> table.getItems().add(info));
	}
	
	/** @since 00.02.04 */
	private final class Actions {
		
		private ProgressWindow progressWindow;
		
		public final void submit(ProgressAction action) {
			progressWindow = ProgressWindow.submitAction(MainWindow.this, action);
			FXUtils.thread(progressWindow::showAndWait);
		}
		
		public final void terminate() {
			if(progressWindow != null)
				FXUtils.thread(progressWindow::hide);
		}
	}
	
	/** @since 00.02.08 */
	private final class DefaultTrackerVisitor implements TrackerVisitor {
		
		private final PipelineInfo info;
		
		public DefaultTrackerVisitor(PipelineInfo info) {
			this.info = info;
		}
		
		@Override
		public void visit(ConversionTracker tracker) {
			double percent     = tracker.progress() * 100.0;
			double timeCurrent = tracker.currentTime();
			double timeTotal   = tracker.totalTime();
			
			info.update(new PipelineInfoData.OfConversion(
				tracker,
				tr(
					"progress.conversion.update",
					"percent",      Utils.num2string(percent,     2),
					"time_current", Utils.num2string(timeCurrent, 2),
					"time_total",   Utils.num2string(timeTotal,   2)
				)
			));
		}
		
		@Override
		public void visit(DownloadTracker tracker) {
			double percent  = tracker.progress() * 100.0;
			String speedVal = Utils.formatSize(tracker.speed(), 2);
			double timeLeft = tracker.secondsLeft();
			
			info.update(new PipelineInfoData.OfDownload(
				tracker,
				tr(
					"progress.download.update",
					"percent",   Utils.num2string(percent,  2),
					"speed",     speedVal,
					"time_left", Utils.num2string(timeLeft, 0)
				)
			));
		}
		
		@Override
		public void visit(PlainTextTracker tracker) {
			double percent = tracker.progress() * 100.0;
			String text    = tracker.text();
			
			info.update(new PipelineInfoData.OfProgress(
				tracker.progress(),
				tr(
					"progress.plain",
					"percent", Utils.num2string(percent, 2),
					"text",    text
				)
			));
		}
		
		@Override
		public void visit(WaitTracker tracker) {
			info.update(new PipelineInfoData.OfText(tr("progress.waiting")));
		}
		
		@Override
		public void visit(Tracker tracker) {
			String text = tracker.textProgress();
			
			if(text != null) {
				info.update(new PipelineInfoData.OfText(text));
			}
		}
	}
	
	/** @since 00.02.08 */
	private static final class IconTableCell extends TableCell<PipelineInfo, String> {
		
		private ImageView icon;
		
		public IconTableCell() {
			getStyleClass().add("has-icon");
		}
		
		private final Image image() {
			MediaGetter getter = (MediaGetter) getTableRow().getItem().media().media().source().instance();
			return getter != null ? getter.icon() : null;
		}
		
		private final void initialize() {
			if(isInitialized()) {
				return;
			}
			
			if(getTableRow().getItem() == null) {
				return;
			}
			
			Image image = image();
			
			if(image == null) {
				return;
			}
			
			icon = new ImageView(image);
			icon.setFitWidth(24);
			icon.setFitHeight(24);
			
			setGraphic(icon);
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}
		
		private final void dispose() {
			if(!isInitialized()) {
				return;
			}
			
			icon = null;
		}
		
		private final boolean isInitialized() {
			return icon != null;
		}
		
		private final void value(String value) {
			initialize();
		}
		
		@Override
		protected void updateItem(String item, boolean empty) {
			if(item == getItem() && isInitialized()) {
				return;
			}
			
			super.updateItem(item, empty);
			
			if(item == null) {
				setItem(null);
				setGraphic(null);
				dispose();
			} else {
				value(item);
			}
		}
	}

	/** @since 00.02.08 */
	private static final class ProgressBarTableCell extends TableCell<PipelineInfo, Double> {
		
		private StackPane wrapper;
		private ProgressBar progressBar;
		private Text label;
		
		public ProgressBarTableCell() {
			getStyleClass().add("has-progress-bar");
		}
	
		private final void initialize() {
			if(isInitialized()) {
				return;
			}
			
			wrapper = new StackPane();
			progressBar = new ProgressBar(0.0);
			label = new Text("0.0%");
			wrapper.getChildren().addAll(progressBar, label);
			
			setGraphic(wrapper);
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}
		
		private final void dispose() {
			if(!isInitialized()) {
				return;
			}
			
			wrapper.getChildren().clear();
			label = null;
			progressBar = null;
			wrapper = null;
		}
		
		private final boolean isInitialized() {
			return wrapper != null;
		}
		
		private final void value(double value) {
			initialize();
			progressBar.setProgress(value);
			
			boolean textVisible = value >= 0.0 && value <= 1.0;
			
			if(label.isVisible() != textVisible) {
				label.setVisible(textVisible);
			}
			
			if(textVisible) {
				label.setText(String.format(Locale.US, "%.2f%%", value * 100.0));
			}
		}
		
		@Override
		protected void updateItem(Double item, boolean empty) {
			if(item == getItem() && isInitialized()) {
				return;
			}
			
			super.updateItem(item, empty);
			
			if(item == null) {
				setItem(null);
				setGraphic(null);
				dispose();
			} else {
				value(item);
			}
		}
	}

	public static final class PipelineInfo {
		
		/** @since 00.02.08 */
		private static final long MIN_UPDATE_DIFF_TIME = 250L * 1000000L; // 250 ms
		
		/** @since 00.02.08 */
		public static final double PROGRESS_INDETERMINATE = ProgressBar.INDETERMINATE_PROGRESS;
		
		private final Pipeline pipeline;
		/** @since 00.02.05 */
		private final ResolvedMedia media;
		
		/** @since 00.02.08 */
		private StringProperty sourceProperty;
		/** @since 00.02.08 */
		private StringProperty titleProperty;
		/** @since 00.02.08 */
		private StringProperty destinationProperty;
		/** @since 00.02.08 */
		private DoubleProperty progressProperty;
		/** @since 00.02.08 */
		private StringProperty stateProperty;
		/** @since 00.02.08 */
		private StringProperty currentProperty;
		/** @since 00.02.08 */
		private StringProperty totalProperty;
		/** @since 00.02.08 */
		private StringProperty speedProperty;
		/** @since 00.02.08 */
		private StringProperty timeLeftProperty;
		/** @since 00.02.08 */
		private StringProperty informationProperty;
		
		/** @since 00.02.08 */
		private long lastUpdateTime = Long.MIN_VALUE;
		private volatile boolean stateUpdated = false;
		
		/** @since 00.02.08 */
		private boolean isQueued;
		
		public PipelineInfo(Pipeline pipeline, ResolvedMedia media) {
			this.pipeline = Objects.requireNonNull(pipeline);
			this.media = Objects.requireNonNull(media);
			this.pipeline.getEventRegistry().addMany((o) -> stateUpdated = true, PipelineEvent.values());
		}
		
		public void update(PipelineInfoData data) {
			long now = System.nanoTime();
			
			if(lastUpdateTime == Long.MIN_VALUE
					|| stateUpdated
					|| now - lastUpdateTime >= MIN_UPDATE_DIFF_TIME) {
				FXUtils.thread(() -> data.update(this));
				stateUpdated = false;
				lastUpdateTime = now;
			}
		}
		
		/** @since 00.02.08 */
		public void isQueued(boolean isQueued) {
			this.isQueued = isQueued;
		}
		
		/** @since 00.02.08 */
		public StringProperty sourceProperty() {
			return sourceProperty == null
						? sourceProperty = new SimpleStringProperty(source())
						: sourceProperty;
		}
		
		/** @since 00.02.08 */
		public StringProperty titleProperty() {
			return titleProperty == null
						? titleProperty = new SimpleStringProperty(title())
						: titleProperty;
		}
		
		/** @since 00.02.08 */
		public StringProperty destinationProperty() {
			return destinationProperty == null
						? destinationProperty = new SimpleStringProperty(destination())
						: destinationProperty;
		}
		
		/** @since 00.02.08 */
		public DoubleProperty progressProperty() {
			return progressProperty == null
						? progressProperty = new SimpleDoubleProperty()
						: progressProperty;
		}
		
		/** @since 00.02.08 */
		public StringProperty stateProperty() {
			return stateProperty == null
						? stateProperty = new SimpleStringProperty()
						: stateProperty;
		}
		
		/** @since 00.02.08 */
		public StringProperty currentProperty() {
			return currentProperty == null
						? currentProperty = new SimpleStringProperty()
						: currentProperty;
		}
		
		/** @since 00.02.08 */
		public StringProperty totalProperty() {
			return totalProperty == null
						? totalProperty = new SimpleStringProperty()
						: totalProperty;
		}
		
		/** @since 00.02.08 */
		public StringProperty speedProperty() {
			return speedProperty == null
						? speedProperty = new SimpleStringProperty()
						: speedProperty;
		}
		
		/** @since 00.02.08 */
		public StringProperty timeLeftProperty() {
			return timeLeftProperty == null
						? timeLeftProperty = new SimpleStringProperty()
						: timeLeftProperty;
		}
		
		/** @since 00.02.08 */
		public StringProperty informationProperty() {
			return informationProperty == null
						? informationProperty = new SimpleStringProperty()
						: informationProperty;
		}
		
		/** @since 00.02.08 */
		public void progress(double progress) {
			progressProperty().set(progress);
		}
		
		/** @since 00.02.08 */
		public void state(String state) {
			stateProperty().set(state);
		}
		
		/** @since 00.02.08 */
		public void current(String current) {
			currentProperty().set(current);
		}
		
		/** @since 00.02.08 */
		public void total(String total) {
			totalProperty().set(total);
		}
		
		/** @since 00.02.08 */
		public void speed(String speed) {
			speedProperty().set(speed);
		}
		
		/** @since 00.02.08 */
		public void timeLeft(String timeLeft) {
			timeLeftProperty().set(timeLeft);
		}
		
		/** @since 00.02.08 */
		public void information(String information) {
			informationProperty().set(information);
		}
		
		/** @since 00.02.08 */
		public String source() {
			return media.media().source().toString();
		}
		
		/** @since 00.02.08 */
		public String title() {
			return media.media().metadata().title();
		}
		
		/** @since 00.02.08 */
		public String destination() {
			return media.path().toString();
		}
		
		/** @since 00.02.08 */
		public double progress() {
			return progressProperty().get();
		}
		
		/** @since 00.02.08 */
		public String state() {
			return stateProperty().get();
		}
		
		/** @since 00.02.08 */
		public String current() {
			return currentProperty().get();
		}
		
		/** @since 00.02.08 */
		public String total() {
			return totalProperty().get();
		}
		
		/** @since 00.02.08 */
		public String speed() {
			return speedProperty().get();
		}
		
		/** @since 00.02.08 */
		public String timeLeft() {
			return timeLeftProperty().get();
		}
		
		/** @since 00.02.08 */
		public String information() {
			return informationProperty().get();
		}
		
		/** @since 00.02.08 */
		public Pipeline pipeline() {
			return pipeline;
		}
		
		/** @since 00.02.08 */
		public ResolvedMedia media() {
			return media;
		}
		
		/** @since 00.02.08 */
		public boolean isQueued() {
			return isQueued;
		}
	}
	
	/** @since 00.02.08 */
	public static interface PipelineInfoData {
		
		void update(PipelineInfo info);
		
		public static final class OfText implements PipelineInfoData {
			
			private final String text;
			
			public OfText(String text) {
				this.text = text;
			}
			
			@Override
			public void update(PipelineInfo info) {
				info.current(null);
				info.total(null);
				info.speed(null);
				info.timeLeft(null);
				info.progress(PipelineInfo.PROGRESS_INDETERMINATE);
				info.information(text);
			}
		}
		
		public static final class OfEndText implements PipelineInfoData {
			
			private final String state;
			private final String text;
			private final double progress;
			
			public OfEndText(String state, String text, double progress) {
				this.state = state;
				this.text = text;
				this.progress = progress;
			}
			
			@Override
			public void update(PipelineInfo info) {
				info.progress(progress);
				info.state(state);
				info.current(null);
				info.total(null);
				info.speed(null);
				info.timeLeft(null);
				info.information(text);
			}
		}
		
		public static final class OfProgress implements PipelineInfoData {
			
			private final double progress;
			private final String text;
			
			public OfProgress(double progress, String text) {
				this.progress = progress;
				this.text = text;
			}
			
			@Override
			public void update(PipelineInfo info) {
				info.progress(progress);
				info.state(null);
				info.current(null);
				info.total(null);
				info.speed(null);
				info.timeLeft(null);
				info.information(text);
			}
		}
		
		public static final class OfDownload implements PipelineInfoData {
			
			private final DownloadTracker tracker;
			private final String text;
			
			public OfDownload(DownloadTracker tracker, String text) {
				this.tracker = tracker;
				this.text = text;
			}
			
			@Override
			public void update(PipelineInfo info) {
				info.progress(tracker.progress());
				info.state("download");
				info.current(Utils.OfFormat.size(tracker.current(), SizeUnit.BYTES, 2));
				info.total(Utils.OfFormat.size(tracker.total(), SizeUnit.BYTES, 2));
				info.speed(Utils.OfFormat.size(tracker.speed(), SizeUnit.BYTES, 2) + "/s");
				info.timeLeft(Utils.OfFormat.time(tracker.secondsLeft(), TimeUnit.SECONDS, false));
				info.information(text);
			}
		}
		
		public static final class OfConversion implements PipelineInfoData {
			
			private final ConversionTracker tracker;
			private final String text;
			
			public OfConversion(ConversionTracker tracker, String text) {
				this.tracker = tracker;
				this.text = text;
			}
			
			@Override
			public void update(PipelineInfo info) {
				info.progress(tracker.progress());
				info.state("conversion");
				info.current(Utils.OfFormat.time(tracker.currentTime(), TimeUnit.SECONDS, false));
				info.total(Utils.OfFormat.time(tracker.totalTime(), TimeUnit.SECONDS, false));
				info.speed(null);
				info.timeLeft(null);
				info.information(text);
			}
		}
	}
}