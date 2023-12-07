package sune.app.mediadown.gui.window;

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.beans.Observable;
import javafx.collections.ListChangeListener.Change;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import sune.app.mediadown.Disposables;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.concurrent.Threads;
import sune.app.mediadown.download.Download;
import sune.app.mediadown.entity.MediaEngine;
import sune.app.mediadown.entity.MediaEngines;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.PipelineEvent;
import sune.app.mediadown.event.QueueEvent;
import sune.app.mediadown.event.tracker.ConversionTracker;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.PipelineProgress;
import sune.app.mediadown.event.tracker.PipelineStates;
import sune.app.mediadown.event.tracker.PlainTextTracker;
import sune.app.mediadown.event.tracker.Tracker;
import sune.app.mediadown.event.tracker.TrackerEvent;
import sune.app.mediadown.event.tracker.TrackerVisitor;
import sune.app.mediadown.event.tracker.WaitTracker;
import sune.app.mediadown.gui.Dialog;
import sune.app.mediadown.gui.GUI;
import sune.app.mediadown.gui.InformationItems.ItemDownloader;
import sune.app.mediadown.gui.InformationItems.ItemMediaEngine;
import sune.app.mediadown.gui.InformationItems.ItemPlugin;
import sune.app.mediadown.gui.InformationItems.ItemServer;
import sune.app.mediadown.gui.ProgressWindow;
import sune.app.mediadown.gui.ProgressWindow.ProgressAction;
import sune.app.mediadown.gui.ProgressWindow.ProgressContext;
import sune.app.mediadown.gui.Window;
import sune.app.mediadown.gui.control.PipelineTableView;
import sune.app.mediadown.gui.control.PipelineTableView.ColumnFactory;
import sune.app.mediadown.gui.control.PipelineTableView.ContextMenuItem;
import sune.app.mediadown.gui.control.PipelineTableView.ContextMenuItemFactory;
import sune.app.mediadown.gui.control.PipelineTableView.PipelineInfo;
import sune.app.mediadown.gui.control.PipelineTableView.PipelineInfoData;
import sune.app.mediadown.gui.control.PipelineTableView.Stats;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.gui.table.ResolvedMediaPipelineResult;
import sune.app.mediadown.gui.table.TablePipelineResult;
import sune.app.mediadown.gui.window.InformationWindow.InformationTab;
import sune.app.mediadown.gui.window.InformationWindow.TabContent;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.language.Translator;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.message.Message;
import sune.app.mediadown.message.MessageList;
import sune.app.mediadown.message.MessageManager;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.pipeline.ConversionPipelineTask;
import sune.app.mediadown.pipeline.DownloadPipelineTask;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.pipeline.PipelineTransformer;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.PluginUpdater;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.report.Report;
import sune.app.mediadown.report.Report.Reason;
import sune.app.mediadown.report.ReportContext;
import sune.app.mediadown.transformer.Transformer;
import sune.app.mediadown.transformer.Transformers;
import sune.app.mediadown.util.ClipboardUtils;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.MathUtils;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

public final class MainWindow extends Window<BorderPane> {
	
	public static final String NAME = "main";
	
	/** @since 00.02.08 */
	private static final double DEFAULT_WIDTH  = 750.0;
	/** @since 00.02.08 */
	private static final double DEFAULT_HEIGHT = 450.0;
	/** @since 00.02.08 */
	private static final double MINIMUM_WIDTH  = 600.0;
	/** @since 00.02.08 */
	private static final double MINIMUM_HEIGHT = 400.0;
	
	/** @since 00.02.09 */
	private static final String URI_DOCUMENTATION = "https://projects.suneweb.net/media-downloader/docs/";
	
	private static MainWindow INSTANCE;
	
	private final AtomicBoolean closeRequest = new AtomicBoolean();
	private final Actions actions = new Actions();
	
	private PipelineTableView table;
	private Button btnDownload;
	private Button btnDownloadSelected;
	private Button btnAdd;
	
	private ContextMenu menuAdd;
	
	private MenuBar menuBar;
	private Menu menuApplication;
	private MenuItem menuItemInformation;
	private MenuItem menuItemConfiguration;
	/** @since 00.02.09 */
	private MenuItem menuItemCredentials;
	private Menu menuTools;
	private MenuItem menuItemClipboardWatcher;
	private MenuItem menuItemUpdateResources;
	/** @since 00.02.09 */
	private Menu menuHelp;
	/** @since 00.02.09 */
	private MenuItem menuItemReportIssue;
	/** @since 00.02.09 */
	private MenuItem menuItemFeedback;
	/** @since 00.02.09 */
	private MenuItem menuItemDocumentation;
	private MenuItem menuItemMessages;
	/** @since 00.02.08 */
	private MenuItem menuItemAbout;
	
	public MainWindow() {
		super(NAME, new BorderPane(), DEFAULT_WIDTH, DEFAULT_HEIGHT);
		
		pane.setTop(initializeMenuBar());
		pane.setCenter(initializePipelinesTable());
		pane.setBottom(initializeButtons());
		
		setScene(scene);
		setOnCloseRequest(this::closeRequest);
		setMinWidth(MINIMUM_WIDTH);
		setMinHeight(MINIMUM_HEIGHT);
		setResizable(true);
		centerOnScreen();
		
		FXUtils.onWindowShowOnce(this, this::initialize);
		FXUtils.onWindowShowOnce(this, this::showMessagesAsync);
		FXUtils.onWindowShowOnce(this, this::maybeAutoEnableClipboardWatcher);
		
		INSTANCE = this;
	}
	
	/** @since 00.02.08 */
	private static final void contextMenuItemEnableIfAnySelected(Observable o, Pair<ContextMenuItem, Stats> oldValue,
			Pair<ContextMenuItem, Stats> pair) {
		ContextMenuItem item = pair.a;
		Stats stats = pair.b;
		item.setDisable(stats.count() == 0);
	}
	
	/** @since 00.02.09 */
	private static final void contextMenuItemEnableIfOneSelected(Observable o, Pair<ContextMenuItem, Stats> oldValue,
			Pair<ContextMenuItem, Stats> pair) {
		ContextMenuItem item = pair.a;
		Stats stats = pair.b;
		item.setDisable(stats.count() != 1);
	}
	
	/** @since 00.02.08 */
	private static final <T> boolean moveItem(List<T> list, int from, int to) {
		final int size = list.size();
		
		if(from < 0 || from >= size || to < 0 || to >= size) {
			return false;
		}
		
		Collections.swap(list, from, to);
		return true;
	}
	
	/** @since 00.02.08 */
	private static final <T> int[] moveItems(List<T> items, List<Integer> selected, int start, int end,
			int limit, int step, int offset) {
		// Must make a copy since we move the items around during the process
		List<Integer> indexes = List.copyOf(selected);
		int[] newIndexes = new int[indexes.size()];
		
		for(int i = start, p = -1, e = end + step; i != e; p = i, i += step) {
			int index = indexes.get(i);
			
			// Check whether the current item can be moved
			if(index != limit
					&& (p < 0 || newIndexes[p] != index + offset)) {
				moveItem(items, index, index + offset);
				index += offset;
			}
			
			newIndexes[i] = index;
		}
		
		return newIndexes;
	}
	
	/** @since 00.02.08 */
	private static final <T> int[] moveItemsUp(List<T> items, List<Integer> selected) {
		return moveItems(items, selected, 0, selected.size() - 1, 0, 1, -1);
	}
	
	/** @since 00.02.08 */
	private static final <T> int[] moveItemsDown(List<T> items, List<Integer> selected) {
		return moveItems(items, selected, selected.size() - 1, 0, items.size() - 1, -1, 1);
	}
	
	/** @since 00.02.08 */
	private static final <T> void addAll(List<T> list, int index, Collection<T> itemsToAdd) {
		if(index == list.size()) list.addAll(itemsToAdd); // Allow adding to the end
		else                     list.addAll(index, itemsToAdd);
	}
	
	/** @since 00.02.08 */
	private static final <T> int[] moveItemsTo(List<T> items, List<Integer> selected, int destination) {
		final int totalSize = items.size();
		
		if(destination < 0 || destination > totalSize) {
			return Utils.intArray(selected.toArray(Integer[]::new));
		}
		
		final int selectSize = selected.size();
		int belowIndexesCount = 0;
		
		// Use LinkedHashSet for better contains method performance that is used
		// in the removeAll method. Linked variant is used to preserve order.
		Set<T> selectedItems = new LinkedHashSet<>(selectSize);
		for(int index : selected) {
			selectedItems.add(items.get(index));
			
			if(index < destination) {
				++belowIndexesCount;
			}
		}
		
		// Must recalculate since the size of the list will change
		int destinationAfterRemove = destination - belowIndexesCount;
		
		// Use batch methods for better performance
		items.removeAll(selectedItems);
		addAll(items, destinationAfterRemove, selectedItems);
		
		// Must recalculate the select range since we can also add to the end of the list
		int selectIndexEnd = Math.min(destination + selectSize, totalSize);
		int selectIndexStart = selectIndexEnd - selectSize;
		
		return IntStream.range(selectIndexStart, selectIndexEnd).toArray();
	}
	
	/** @since 00.02.08 */
	private static final <T> int[] moveItemsBegin(List<T> items, List<Integer> selected) {
		return moveItemsTo(items, selected, 0);
	}
	
	/** @since 00.02.08 */
	private static final <T> int[] moveItemsEnd(List<T> items, List<Integer> selected) {
		return moveItemsTo(items, selected, items.size());
	}
	
	/** @since 00.02.08 */
	private static final <T> void selectIndexes(TableViewSelectionModel<T> selection, int[] indexes) {
		if(indexes == null || indexes.length == 0) {
			return;
		}
		
		selection.clearSelection();
		selection.selectIndices(
			indexes[0],
			indexes.length > 1
				? Arrays.copyOfRange(indexes, 1, indexes.length)
				: null
		);
	}
	
	public static final MainWindow getInstance() {
		return INSTANCE;
	}
	
	/** @since 00.02.09 */
	private final boolean shouldClose() {
		// Note: The only consideration now when closing the window is put to active pipelines.
		//       Saving the list of incompleted pipelines is currently not supported.
		
		if(!table.hasActivePipelines()) {
			// Nothing is active, may be closed
			return true;
		}
		
		Translation tr = trtr("dialogs.close_request.active_processes");
		return Dialog.showPrompt(tr.getSingle("title"), tr.getSingle("text"));
	}
	
	/** @since 00.02.09 */
	private final void closeRequest(WindowEvent e) {
		if(!closeRequest.compareAndSet(false, true)) {
			return;
		}
		
		// Check whether the user confirmed the window to be closed
		// when there are running pipelines.
		if(!shouldClose()) {
			// Prevent window from closing
			e.consume();
			closeRequest.set(false);
			return; // Do not continue
		}
		
		// Since all of the environment will be disposed and it will take some time,
		// run it in a new thread.
		Threads.executeEnsured(MediaDownloader::close);
	}
	
	/** @since 00.02.09 */
	private final void dispose() {
		actions.terminate();
		maybeAutoDisableClipboardWatcher();
		stopPipelines();
	}
	
	private final void stopPipelines() {
		table.stopAll();
	}
	
	/** @since 00.02.08 */
	private final void initialize() {
		Disposables.add(this::dispose);
		
		// Initialize the menu AFTER the window is shown so that plugins are already loaded.
		initializeAddMenu();
	}
	
	/** @since 00.02.02 */
	private final boolean showMessages() {
		MessageList list = Ignore.supplier(MessageManager::current, MessageManager::empty);
		String language = MediaDownloader.language().code();
		
		if(language.equalsIgnoreCase("auto")) {
			language = MediaDownloader.Languages.localLanguage().code();
		}
		
		List<Message> messages = list.difference(
			language, Ignore.supplier(MessageManager::local, MessageManager::empty)
		);
		
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
		actions.submit((context) -> {
			context.setProgress(ProgressContext.PROGRESS_INDETERMINATE);
			context.setText(tr("actions.messages.checking"));
			Ignore.callVoid(MainWindow.this::showMessages, MediaDownloader::error);
		});
	}
	
	/** @since 00.02.02 */
	private final boolean resetAndShowMessages() {
		Ignore.callVoid(MessageManager::deleteLocal, MediaDownloader::error);
		return showMessages();
	}
	
	/** @since 00.02.04 */
	private final void resetAndShowMessagesAsync() {
		actions.submit((context) -> {
			context.setProgress(ProgressContext.PROGRESS_INDETERMINATE);
			context.setText(tr("actions.messages.checking"));
			
			if(!resetAndShowMessages()) {
				// Show the dialog in the next pulse so that the progress window can be closed
				FXUtils.thread(() -> {
					Translation tr = trtr("dialogs.messages_empty");
					Dialog.showInfo(tr.getSingle("title"), tr.getSingle("text"));
				});
			}
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
	
	/** @since 00.02.08 */
	private final MenuItem createMediaEngineMenuItem(MediaEngine engine) {
		MenuItem item = new MenuItem(engine.title());
		ImageView icon = new ImageView(engine.icon());
		
		icon.setFitWidth(16);
		icon.setFitHeight(16);
		item.setGraphic(icon);
		item.getProperties().put("engine", engine);
		item.setOnAction((e) -> showSelectionWindow((MediaEngine) item.getProperties().get("engine")));
		
		return item;
	}
	
	/** @since 00.02.08 */
	private final void initializeAddMenu() {
		menuAdd = new ContextMenu();
		
		List<MenuItem> menuItems = MediaEngines.all().stream()
			.sorted(Utils.OfString.comparingNormalized(MediaEngine::title))
			.map(this::createMediaEngineMenuItem)
			.collect(Collectors.toList());
		
		MenuItem itemMediaGetter = new MenuItem(tr("context_menus.add.items.media_getter"));
		itemMediaGetter.setOnAction((e) -> {
			MediaDownloader.window(MediaGetterWindow.NAME).show(this);
		});
		menuItems.add(new SeparatorMenuItem());
		menuItems.add(itemMediaGetter);
		
		menuAdd.getItems().addAll(menuItems);
		prepareContextMenuForShowing(menuAdd);
	}
	
	/** @since 00.02.08 */
	private final TableView<PipelineInfo> initializePipelinesTable() {
		table = new PipelineTableView();
		table.setPlaceholder(new Label(tr("tables.main.placeholder")));
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		ColumnFactory columnFactory = table.columnFactory();
		table.getColumns().addAll(List.of(
			columnFactory.createSource(null, 24),
			columnFactory.createTitle(tr("tables.main.columns.title"), 150),
			columnFactory.createProgressBar(tr("tables.main.columns.progress"), 80),
			columnFactory.createState(tr("tables.main.columns.state"), 100),
			columnFactory.createCurrent(tr("tables.main.columns.current"), 80),
			columnFactory.createTotal(tr("tables.main.columns.total"), 80),
			columnFactory.createSpeed(tr("tables.main.columns.speed"), 80),
			columnFactory.createTimeLeft(tr("tables.main.columns.time_left"), 90),
			columnFactory.createDestination(tr("tables.main.columns.destination"), 150),
			columnFactory.createInformation(tr("tables.main.columns.information"), 150)
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
		
		ContextMenuItemFactory contextMenuItemFactory = table.contextMenuItemFactory();
		table.getContextMenu().getItems().addAll(List.of(
			contextMenuItemFactory.create(tr("context_menus.table.items.add"))
				.setOnActivated((e) -> {
					ContextMenu contextMenu = table.getContextMenu();
					double anchorX = contextMenu.getAnchorX();
					double anchorY = contextMenu.getAnchorY();
					
					contextMenu.hide();
					menuAdd.show(this, anchorX, anchorY);
				}),
			contextMenuItemFactory.createSeparator(),
			contextMenuItemFactory.createStart(tr("context_menus.table.items.start")),
			contextMenuItemFactory.createPause(tr("context_menus.table.items.pause"))
				.addOnContextMenuShowing((o, ov, pair) -> {
					ContextMenuItem item = pair.a;
					Stats stats = pair.b;
					
					item.setText(
						stats.anyNonPaused()
							? tr("context_menus.table.items.pause")
							: tr("context_menus.table.items.resume")
					);
				}),
			contextMenuItemFactory.createTerminate(tr("context_menus.table.items.terminate_cancel"))
				.addOnContextMenuShowing((o, ov, pair) -> {
					ContextMenuItem item = pair.a;
					Stats stats = pair.b;
					
					item.setText(
						stats.anyTerminable()
							? tr("context_menus.table.items.terminate_cancel")
							: tr("context_menus.table.items.terminate_remove")
					);
				}),
			contextMenuItemFactory.createRetry(tr("context_menus.table.items.retry")),
			contextMenuItemFactory.createSeparator(),
			contextMenuItemFactory.create(tr("context_menus.table.items.move_up"))
				.setOnActivated((e) -> {
					List<Integer> indexes = table.selectedIndexes();
					
					if(indexes.isEmpty()) {
						return;
					}
					
					selectIndexes(table.getSelectionModel(), moveItemsUp(table.pipelines(), indexes));
				})
				.addOnContextMenuShowing(MainWindow::contextMenuItemEnableIfAnySelected),
			contextMenuItemFactory.create(tr("context_menus.table.items.move_down"))
				.setOnActivated((e) -> {
					List<Integer> indexes = table.selectedIndexes();
					
					if(indexes.isEmpty()) {
						return;
					}
					
					selectIndexes(table.getSelectionModel(), moveItemsDown(table.pipelines(), indexes));
				})
				.addOnContextMenuShowing(MainWindow::contextMenuItemEnableIfAnySelected),
			contextMenuItemFactory.create(tr("context_menus.table.items.move_begin"))
				.setOnActivated((e) -> {
					List<Integer> indexes = table.selectedIndexes();
					
					if(indexes.isEmpty()) {
						return;
					}
					
					selectIndexes(table.getSelectionModel(), moveItemsBegin(table.pipelines(), indexes));
				})
				.addOnContextMenuShowing(MainWindow::contextMenuItemEnableIfAnySelected),
			contextMenuItemFactory.create(tr("context_menus.table.items.move_end"))
				.setOnActivated((e) -> {
					List<Integer> indexes = table.selectedIndexes();
					
					if(indexes.isEmpty()) {
						return;
					}
					
					selectIndexes(table.getSelectionModel(), moveItemsEnd(table.pipelines(), indexes));
				})
				.addOnContextMenuShowing(MainWindow::contextMenuItemEnableIfAnySelected),
			contextMenuItemFactory.createSeparator(),
			contextMenuItemFactory.create(tr("context_menus.table.items.copy_url"))
				.setOnActivated((e) -> {
					List<PipelineInfo> infos = table.selectedPipelines();
					
					if(infos.isEmpty()) {
						return;
					}
					
					List<Media> media = infos.stream()
						.map((info) -> info.resolvedMedia().media())
						.collect(Collectors.toList());
					
					ClipboardUtils.copy(
						Utils.toString(media, (m) -> m.uri().normalize().toString())
					);
				})
				.addOnContextMenuShowing(MainWindow::contextMenuItemEnableIfAnySelected),
			contextMenuItemFactory.create(tr("context_menus.table.items.copy_source_url"))
				.setOnActivated((e) -> {
					List<PipelineInfo> infos = table.selectedPipelines();
					
					if(infos.isEmpty()) {
						return;
					}
					
					List<Media> media = infos.stream()
						.map((info) -> info.resolvedMedia().media())
						.collect(Collectors.toList());
					
					ClipboardUtils.copy(
						Utils.toString(media, (m) -> Objects.toString(Optional.ofNullable(m.metadata().sourceURI())
						                                                      .map(URI::normalize).orElse(null)))
					);
				})
				.addOnContextMenuShowing(MainWindow::contextMenuItemEnableIfAnySelected),
			contextMenuItemFactory.create(tr("context_menus.table.items.media_info"))
				.setOnActivated((e) -> {
					PipelineInfo info = table.selectedPipeline();
					
					if(info == null) {
						return;
					}
					
					Media media = info.resolvedMedia().media();
					MediaDownloader.window(MediaInfoWindow.NAME)
						.setArgs("parent", this, "media", media)
						.show();
				})
				.addOnContextMenuShowing(MainWindow::contextMenuItemEnableIfOneSelected),
			contextMenuItemFactory.create(tr("context_menus.table.items.report_broken"))
				.setOnActivated((e) -> {
					PipelineInfo info = table.selectedPipeline();
					
					if(info == null) {
						return;
					}
					
					PipelineTask task = info.pipeline().getTask();
					ReportContext reportContext = null;
					
					if(task instanceof DownloadPipelineTask) {
						DownloadPipelineTask casted = (DownloadPipelineTask) task;
						reportContext = ReportContext.ofDownload(casted);
					} else if(task instanceof ConversionPipelineTask) {
						ConversionPipelineTask casted = (ConversionPipelineTask) task;
						reportContext = ReportContext.ofConversion(casted);
					} else {
						reportContext = ReportContext.none();
					}
					
					Media media = info.resolvedMedia().media();
					GUI.showReportWindow(this, Report.Builders.ofMedia(
						media, Reason.BROKEN,
						reportContext
					), ReportWindow.Feature.onlyReasons(Reason.BROKEN));
				})
				.addOnContextMenuShowing(MainWindow::contextMenuItemEnableIfOneSelected),
			contextMenuItemFactory.createSeparator(),
			contextMenuItemFactory.createShowFile(tr("context_menus.table.items.show_file"))
		));
		
		return table;
	}
	
	/** @since 00.02.08 */
	private final MenuBar initializeMenuBar() {
		menuBar = new MenuBar();
		menuApplication = new Menu(tr("menu_bar.application.title"));
		menuTools = new Menu(tr("menu_bar.tools.title"));
		menuHelp = new Menu(tr("menu_bar.help.title"));
		
		menuItemInformation = new MenuItem(tr("menu_bar.application.item.information"));
		menuItemInformation.setOnAction((e) -> {
			showInformationWindow();
		});
		
		menuItemConfiguration = new MenuItem(tr("menu_bar.application.item.configuration"));
		menuItemConfiguration.setOnAction((e) -> {
			MediaDownloader.window(ConfigurationWindow.NAME).show(this);
		});
		
		menuItemCredentials = new MenuItem(tr("menu_bar.application.item.credentials"));
		menuItemCredentials.setOnAction((e) -> {
			showCredentialsWindow();
		});
		
		menuItemMessages = new MenuItem(tr("menu_bar.help.item.messages"));
		menuItemMessages.setOnAction((e) -> resetAndShowMessagesAsync());
		
		menuItemAbout = new MenuItem(tr("menu_bar.help.item.about"));
		menuItemAbout.setOnAction((e) -> {
			showAboutWindow();
		});
		
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
		
		menuItemReportIssue = new MenuItem(tr("menu_bar.help.item.report_issue"));
		menuItemReportIssue.setOnAction((e) -> {
			GUI.showReportWindow(
				this, Report.Builders.ofIssue(ReportContext.none()),
				ReportWindow.Feature.onlyReasons(Reason.ISSUE),
				ReportWindow.Feature.noteRequired()
			);
		});
		
		menuItemFeedback = new MenuItem(tr("menu_bar.help.item.feedback"));
		menuItemFeedback.setOnAction((e) -> {
			GUI.showReportWindow(
				this, Report.Builders.ofFeedback(ReportContext.none()),
				ReportWindow.Feature.onlyReasons(Reason.FEEDBACK),
				ReportWindow.Feature.noteRequired()
			);
		});
		
		menuItemDocumentation = new MenuItem(tr("menu_bar.help.item.documentation"));
		menuItemDocumentation.setOnAction((e) -> {
			Ignore.callVoid(() -> OS.current().browse(Net.uri(URI_DOCUMENTATION)), MediaDownloader::error);
		});
		
		menuApplication.getItems().addAll(
			menuItemInformation, menuItemConfiguration, menuItemCredentials
		);
		menuTools.getItems().addAll(menuItemClipboardWatcher, menuItemUpdateResources);
		menuHelp.getItems().addAll(
			menuItemReportIssue, menuItemFeedback, menuItemDocumentation, menuItemMessages, menuItemAbout
		);
		menuBar.getMenus().addAll(menuApplication, menuTools, menuHelp);
		
		return menuBar;
	}
	
	/** @since 00.02.08 */
	private final Pane initializeButtons() {
		btnDownload = new Button(tr("buttons.download"));
		btnDownload.setOnAction((e) -> {
			table.startAll();
			btnDownload.setDisable(true);
			btnDownloadSelected.setDisable(true);
		});
		
		btnDownloadSelected = new Button(tr("buttons.download_selected"));
		btnDownloadSelected.setOnAction((e) -> {
			table.startSelected();
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
							(ctx) -> context.setText(translation.getSingle("labels.update.download.begin", "name", pluginTitle)));
						downloadUpdate.addEventListener(DownloadEvent.UPDATE, (ctx) -> {
							DownloadTracker tracker = Utils.cast(ctx.trackerManager().tracker());
							String progress = translation.getSingle("labels.update.download.progress",
								"name",    pluginTitle,
								"current", tracker.current(),
								"total",   tracker.total(),
								"percent", MathUtils.round(tracker.progress() * 100.0, 2));
							context.setText(progress);
						});
						downloadUpdate.addEventListener(DownloadEvent.ERROR,
							(ctx) -> context.setText(translation.getSingle("labels.update.download.error", "message", ctx.exception())));
						downloadUpdate.addEventListener(DownloadEvent.END,
							(ctx) -> context.setText(translation.getSingle("labels.update.download.end", "name", pluginTitle)));
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
	
	/** @since 00.02.08 */
	private final void initializeInformationWindowPluginsTab(InformationWindow window) {
		InformationWindow informationWindow = MediaDownloader.window(InformationWindow.NAME);
		InformationTab<ItemPlugin> tab = window.selectedTab();
		Translation tabTranslation = informationWindow.getTranslation().getTranslation("tabs.plugins");
		
		tab.list().getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		
		// Special buttons for the Plugins tab
		Button btnUpdateAll = new Button(tabTranslation.getSingle("buttons.update_all"));
		Button btnUpdateSelected = new Button(tabTranslation.getSingle("buttons.update_selected"));
		
		btnUpdateAll.setOnAction((e) -> {
			ProgressWindow.submitAction(window, action_updatePlugins(window, Plugins.allLoaded()));
		});
		
		btnUpdateSelected.setOnAction((e) -> {
			Collection<ItemPlugin> selectedItems = tab.list().getSelectionModel().getSelectedItems();
			Collection<PluginFile> plugins = selectedItems.stream()
				.map(ItemPlugin::getPlugin)
				.collect(Collectors.toList());
			
			ProgressWindow.submitAction(window, action_updatePlugins(window, plugins));
		});
		
		HBox boxBottom = new HBox(5);
		HBox boxFill = new HBox();
		boxBottom.getChildren().addAll(boxFill, btnUpdateAll, btnUpdateSelected);
		boxBottom.setId("box-bottom");
		HBox.setHgrow(boxFill, Priority.ALWAYS);
		
		GridPane pane = Utils.cast(tab.getContent());
		pane.getChildren().add(boxBottom);
		GridPane.setConstraints(boxBottom, 0, 2, 1, 1);
	}
	
	private final void showInformationWindow() {
		InformationWindow window = MediaDownloader.window(InformationWindow.NAME);
		Translation tr = window.getTranslation().getTranslation("tabs");
		
		List<TabContent<?>> tabs = List.of(
			new TabContent<>(tr.getTranslation("plugins"), ItemPlugin.items()),
			new TabContent<>(tr.getTranslation("media_engines"), ItemMediaEngine.items()),
			new TabContent<>(tr.getTranslation("downloaders"), ItemDownloader.items()),
			new TabContent<>(tr.getTranslation("servers"), ItemServer.items())
		);
		
		// Filter out empty tabs
		tabs = tabs.stream()
			.filter((tab) -> !tab.items().isEmpty())
			.collect(Collectors.toList());
		
		tabs.get(0).setOnInit(this::initializeInformationWindowPluginsTab);
		
		window.setArgs("tabs", tabs);
		window.show(this);
	}
	
	/** @since 00.02.08 */
	private final void showAboutWindow() {
		MediaDownloader.window(AboutWindow.NAME).show(this);
	}
	
	/** @since 00.02.09 */
	private final void showCredentialsWindow() {
		MediaDownloader.window(CredentialsWindow.NAME).show(this);
	}
	
	private final void prepareContextMenuForShowing(ContextMenu contextMenu) {
		contextMenu.showingProperty().addListener((o) -> {
			Map<Object, Object> props = contextMenu.getProperties();
			
			if(!contextMenu.isShowing() || !props.containsKey("firstShow")) {
				return;
			}
			
			Node node = (Node) props.get("node");
			double height = contextMenu.getHeight();
			double screenX = (double) props.get("screenX");
			double screenY = (double) props.get("screenY") - height + 15.0;
			
			props.put("height", height);
			props.remove("firstShow");
			
			contextMenu.hide();
			contextMenu.show(node, screenX, screenY);
		});
	}
	
	private final void showContextMenuAtNode(ContextMenu contextMenu, Node node) {
		// Fix: graphical glitch when the menu is already showing
		if(contextMenu.isShowing()) {
			return;
		}
		
		Bounds boundsLocal = node.getBoundsInLocal();
		Bounds boundsScreen = node.localToScreen(boundsLocal);
		double screenX = boundsScreen.getMinX();
		double screenY = boundsScreen.getMinY();
		Map<Object, Object> props = contextMenu.getProperties();
		
		if(props.containsKey("height")) {
			double height = (double) props.get("height");
			contextMenu.show(node, screenX, screenY - height + 15.0);
		} else {
			props.put("firstShow", true);
			props.put("node", node);
			props.put("screenX", screenX);
			props.put("screenY", screenY);
			contextMenu.show(this);
		}
	}
	
	/** @since 00.02.08 */
	private final PipelineInfo createPipelineInfo(ResolvedMedia media) {
		List<Transformer> transformers = Transformers.allFrom(media);
		PipelineTransformer pipelineTransformer = Transformer.of(transformers).pipelineTransformer();
		
		Pipeline pipeline = Pipeline.create(pipelineTransformer);
		PipelineInfo info = new PipelineInfo(pipeline, media);
		TrackerVisitor visitor = new DefaultTrackerVisitor(info);
		
		pipeline.addEventListener(PipelineEvent.BEGIN, (o) -> {
			info.update(new PipelineInfoData.OfText(PipelineStates.INITIALIZATION, PipelineInfo.TEXT_NONE));
		});
		
		pipeline.addEventListener(PipelineEvent.END, (o) -> {
			if(pipeline.isError()) {
				return;
			}
			
			boolean isStopped = pipeline.isStopped();
			
			info.update(new PipelineInfoData.OfEndText(
				isStopped ? PipelineStates.STOPPED : PipelineStates.DONE,
				isStopped ? PipelineProgress.NONE : PipelineProgress.MAX,
				PipelineInfo.TEXT_NONE
			));
		});
		
		pipeline.getEventRegistry().addMany((o) -> {
			Exception exception = Utils.<Pair<?, Exception>>cast(o).b;
			
			info.update(new PipelineInfoData.OfEndText(
				PipelineStates.ERROR,
				PipelineProgress.NONE,
				exception.getMessage()
			));
			
			MediaDownloader.error(exception);
		}, PipelineEvent.ERROR, TrackerEvent.ERROR);
		
		pipeline.addEventListener(PipelineEvent.UPDATE, (p) -> {
			if(!pipeline.isRunning()) {
				return;
			}
			
			info.update(new PipelineInfoData.OfText(PipelineStates.WAIT, PipelineInfo.TEXT_NONE));
		});
		
		pipeline.addEventListener(PipelineEvent.PAUSE, (p) -> {
			info.update(new PipelineInfoData.OfState(PipelineStates.PAUSED, PipelineInfo.TEXT_NONE));
		});
		
		pipeline.addEventListener(TrackerEvent.UPDATE, (tracker) -> {
			if(!pipeline.isRunning()) {
				return;
			}
			
			tracker.visit(visitor);
		});
		
		pipeline.addEventListener(QueueEvent.POSITION_UPDATE, (pair) -> {
			if(!pipeline.isRunning()) {
				return;
			}
			
			info.update(new PipelineInfoData.OfText(
				Utils.format(
					Translator.maybeTranslate(PipelineStates.QUEUED),
					"context", Translator.maybeTranslate(pair.a.contextState()),
					"position", pair.b + 1
				),
				PipelineInfo.TEXT_NONE
			));
		});
		
		return info;
	}
	
	public final void showSelectionWindow(MediaEngine engine) {
		TableWindow window = MediaDownloader.window(TableWindow.NAME);
		
		Threads.execute(() -> {
			Ignore.callVoid(() -> {
				TablePipelineResult<?> result = window.show(this, engine);
				
				if(!result.isTerminating()) {
					return;
				}
				
				ResolvedMediaPipelineResult castedResult = Utils.cast(result);
				castedResult.getValue().forEach(this::addDownload);
			}, MediaDownloader::error);
		});
	}
	
	/** @since 00.02.05 */
	public final void addDownload(ResolvedMedia media) {
		table.add(createPipelineInfo(media));
	}
	
	/** @since 00.02.04 */
	private final class Actions {
		
		private ProgressWindow progressWindow;
		
		public final void submit(ProgressAction action) {
			progressWindow = ProgressWindow.submitAction(MainWindow.this, action);
			FXUtils.thread(progressWindow::showAndWait);
		}
		
		public final void terminate() {
			if(progressWindow != null) {
				FXUtils.thread(progressWindow::hide);
			}
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
			info.update(new PipelineInfoData.OfConversion(tracker, PipelineInfo.TEXT_NONE));
		}
		
		@Override
		public void visit(DownloadTracker tracker) {
			info.update(new PipelineInfoData.OfDownload(tracker, PipelineInfo.TEXT_NONE));
		}
		
		@Override
		public void visit(PlainTextTracker tracker) {
			info.update(new PipelineInfoData.OfTracker(tracker));
		}
		
		@Override
		public void visit(WaitTracker tracker) {
			info.update(new PipelineInfoData.OfText(tracker.state(), PipelineInfo.TEXT_NONE));
		}
		
		@Override
		public void visit(Tracker tracker) {
			info.update(new PipelineInfoData.OfTracker(tracker));
		}
	}
}