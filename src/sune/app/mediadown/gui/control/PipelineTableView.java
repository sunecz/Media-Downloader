package sune.app.mediadown.gui.control;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.MediaGetter;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.event.PipelineEvent;
import sune.app.mediadown.event.tracker.ConversionTracker;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.PipelineProgress;
import sune.app.mediadown.event.tracker.Tracker;
import sune.app.mediadown.event.tracker.TrackerView;
import sune.app.mediadown.gui.control.PipelineTableView.PipelineInfo;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.pipeline.MediaPipelineResult;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineMedia;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.util.Cancellable;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Threads;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;
import sune.app.mediadown.util.Utils.SizeUnit;

/** @since 00.02.08 */
public class PipelineTableView extends TableView<PipelineInfo> {
	
	private ColumnFactory columnFactory;
	private ContextMenuItemFactory contextMenuItemFactory;
	
	private ObjectProperty<PipelineInfo> onItemDoubleClicked;
	
	public PipelineTableView() {
		setContextMenu(initializeContextMenu());
		
		addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, (e) -> {
			Stats stats = Stats.from(selectedPipelines());
			
			getContextMenu().getItems().stream()
				.filter((item) -> item instanceof ContextMenuItem)
				.map(Utils::<ContextMenuItem>cast)
				.forEach((item) -> item.onContextMenuShowing().set(new Pair<>(item, stats)));
		});
		
		addEventHandler(MouseEvent.MOUSE_PRESSED, (e) -> {
			List<PipelineInfo> infos = selectedPipelines();
			
			if(infos.isEmpty()) {
				return; // Nothing to do
			}
			
			switch(e.getButton()) {
				case PRIMARY:
					if(e.getClickCount() > 1) {
						onItemDoubleClicked().set(infos.get(0));
					}
					
					break;
				default:
					// Do nothing
					break;
			}
		});
		
		onItemDoubleClicked().addListener((o, ov, info) -> {
			Pipeline pipeline = info.pipeline();
			
			if(pipeline.isStarted() || pipeline.isDone()) {
				showFile(info);
			}
		});
		
		BorderPane.setMargin(this, new Insets(15, 15, 5, 15));
	}
	
	private static final boolean anyNonPaused(List<PipelineInfo> infos) {
		return infos.stream().map(PipelineInfo::pipeline)
					.anyMatch((p) -> p.isStarted() && p.isRunning());
	}
	
	private static final boolean anyTerminable(List<PipelineInfo> infos) {
		return infos.stream().map(PipelineInfo::pipeline)
					.anyMatch((p) -> p.isStarted() && (p.isRunning() || p.isPaused()));
	}
	
	private static final void showFile(PipelineInfo info) {
		Ignore.callVoid(() -> OS.current().highlight(info.resolvedMedia().path()), MediaDownloader::error);
	}
	
	private final ContextMenu initializeContextMenu() {
		ContextMenu contextMenu = new ContextMenu();
		
		contextMenu.setAutoFix(true);
		contextMenu.setAutoHide(true);
		
		return contextMenu;
	}
	
	private final void addMissing(List<PipelineInfo> infos) {
		Set<PipelineInfo> existing = new HashSet<>(pipelines());
		List<PipelineInfo> toAdd = infos.stream()
			.filter(Predicate.not(existing::contains))
			.collect(Collectors.toList());
		FXUtils.thread(() -> getItems().addAll(toAdd));
	}
	
	private final List<PipelineInfo> existingOnly(List<PipelineInfo> infos) {
		Set<PipelineInfo> existing = new HashSet<>(pipelines());
		return infos.stream().filter(existing::contains).collect(Collectors.toList());
	}
	
	private final void start(List<PipelineInfo> infos, boolean checkMissing) {
		if(checkMissing) {
			addMissing(infos);
		}
		
		List<PipelineInfo> notEnqueued = infos.stream()
			.filter(Predicate.not(PipelineInfo::isQueued))
			.collect(Collectors.toList());
		
		// Enqueue all the items, so that they can be sequentually added
		notEnqueued.stream().forEachOrdered((i) -> i.isQueued(true));
		
		// Start all items in a thread with sequential ordering
		Threads.executeEnsured(() -> {
			notEnqueued.stream().forEachOrdered(PipelineInfo::start);
		});
	}
	
	private final void stop(List<PipelineInfo> infos, boolean filterNonExisting) {
		if(filterNonExisting) {
			infos = existingOnly(infos);
		}
		
		infos.stream().forEachOrdered(PipelineInfo::stop);
	}
	
	public void add(PipelineInfo info) {
		FXUtils.thread(() -> getItems().add(info));
	}
	
	public void add(List<PipelineInfo> infos) {
		FXUtils.thread(() -> getItems().addAll(infos));
	}
	
	public void remove(PipelineInfo info) {
		FXUtils.thread(() -> getItems().remove(info));
	}
	
	public void remove(List<PipelineInfo> infos) {
		FXUtils.thread(() -> getItems().removeAll(infos));
	}
	
	public void start(List<PipelineInfo> infos) {
		start(infos, true);
	}
	
	public void startAll() {
		start(pipelines(), false);
	}
	
	public void startSelected() {
		start(selectedPipelines(), false);
	}
	
	public void stop(List<PipelineInfo> infos) {
		stop(infos, true);
	}
	
	public void stopAll() {
		stop(pipelines(), false);
	}
	
	public void stopSelected() {
		stop(selectedPipelines(), false);
	}
	
	public ObjectProperty<PipelineInfo> onItemDoubleClicked() {
		if(onItemDoubleClicked == null) {
			onItemDoubleClicked = new SimpleObjectProperty<>();
		}
		
		return onItemDoubleClicked;
	}
	
	public ContextMenuItemFactory contextMenuItemFactory() {
		if(contextMenuItemFactory == null) {
			contextMenuItemFactory = new DefaultContextMenuItemFactory(this);
		}
		
		return contextMenuItemFactory;
	}
	
	public ColumnFactory columnFactory() {
		if(columnFactory == null) {
			columnFactory = new DefaultColumnFactory();
		}
		
		return columnFactory;
	}
	
	public List<PipelineInfo> pipelines() {
		return getItems();
	}
	
	public List<PipelineInfo> selectedPipelines() {
		return getSelectionModel().getSelectedItems();
	}
	
	public PipelineInfo selectedPipeline() {
		return getSelectionModel().getSelectedItem();
	}
	
	public List<Integer> selectedIndexes() {
		return getSelectionModel().getSelectedIndices();
	}
	
	public int selectedIndex() {
		return getSelectionModel().getSelectedIndex();
	}
	
	private static final class DefaultContextMenuItemFactory implements ContextMenuItemFactory {
		
		private final PipelineTableView table;
		
		public DefaultContextMenuItemFactory(PipelineTableView table) {
			this.table = table;
		}
		
		@Override
		public ContextMenuItem createStart(String title) {
			ContextMenuItem menuItem = new ContextMenuItem(title);
			
			menuItem.setOnAction((e) -> {
				List<PipelineInfo> infos = table.selectedPipelines();
				
				if(infos.isEmpty()) {
					return; // Nothing to start
				}
				
				for(PipelineInfo info : infos) {
					Ignore.callVoid(info::start, MediaDownloader::error);
				}
			});
			
			menuItem.addOnContextMenuShowing((o, ov, pair) -> {
				ContextMenuItem item = pair.a;
				Stats stats = pair.b;
				
				int count = stats.count();
				int started = stats.started();
				
				item.setDisable(started == count);
			});
			
			return menuItem;
		}
		
		@Override
		public ContextMenuItem createPause(String title) {
			ContextMenuItem menuItem = new ContextMenuItem(title);
			
			menuItem.setOnAction((e) -> {
				List<PipelineInfo> infos = table.selectedPipelines();
				
				if(infos.isEmpty()) {
					return; // Nothing to pause/resume
				}
				
				if(anyNonPaused(infos)) {
					for(PipelineInfo info : infos) {
						Ignore.callVoid(info::pause, MediaDownloader::error);
					}
				} else {
					for(PipelineInfo info : infos) {
						Ignore.callVoid(info::resume, MediaDownloader::error);
					}
				}
			});
			
			menuItem.addOnContextMenuShowing((o, ov, pair) -> {
				ContextMenuItem item = pair.a;
				Stats stats = pair.b;
				
				int count = stats.count();
				int started = stats.started();
				int done = stats.done();
				int stopped = stats.stopped();
				
				item.setDisable(started == 0 || (done == count || stopped == count));
			});
			
			return menuItem;
		}
		
		@Override
		public ContextMenuItem createTerminate(String title) {
			ContextMenuItem menuItem = new ContextMenuItem(title);
			
			menuItem.setOnAction((e) -> {
				List<PipelineInfo> infos = table.selectedPipelines();
				
				if(infos.isEmpty()) {
					return; // Nothing to terminate/remove
				}
				
				if(anyTerminable(infos)) {
					infos.forEach(PipelineInfo::stop);
				} else {
					table.remove(infos);
				}
			});
			
			menuItem.addOnContextMenuShowing((o, ov, pair) -> {
				ContextMenuItem item = pair.a;
				Stats stats = pair.b;
				
				int count = stats.count();
				
				item.setDisable(count == 0);
			});
			
			return menuItem;
		}
		
		@Override
		public ContextMenuItem createShowFile(String title) {
			ContextMenuItem menuItem = new ContextMenuItem(title);
			
			menuItem.setOnAction((e) -> {
				List<PipelineInfo> infos = table.selectedPipelines();
				
				if(infos.isEmpty()) {
					return; // Nothing to show file for
				}
				
				for(PipelineInfo info : infos) {
					showFile(info);
				}
			});
			
			menuItem.addOnContextMenuShowing((o, ov, pair) -> {
				ContextMenuItem item = pair.a;
				Stats stats = pair.b;
				
				int started = stats.started();
				int done = stats.done();
				int stopped = stats.stopped();
				
				item.setDisable(!(started > 0 || done > 0 || stopped > 0));
			});
			
			return menuItem;
		}
		
		@Override
		public ContextMenuItem create(String title) {
			return new ContextMenuItem(title);
		}
		
		@Override
		public SeparatorContextMenuItem createSeparator() {
			return new SeparatorContextMenuItem();
		}
	}
	
	private static final class DefaultColumnFactory implements ColumnFactory {
		
		public DefaultColumnFactory() {
		}
		
		private final TableColumn<PipelineInfo, String> createText(String propertyName, String title,
				double preferredWidth) {
			TableColumn<PipelineInfo, String> column = new TableColumn<>(title);
			column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
			column.setPrefWidth(preferredWidth);
			return column;
		}
		
		@Override
		public TableColumn<PipelineInfo, String> createSource(String title, double preferredWidth) {
			String propertyName = "source";
			TableColumn<PipelineInfo, String> column = new TableColumn<>(title);
			column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
			column.setCellFactory((col) -> new IconTableCell());
			column.setPrefWidth(preferredWidth);
			return column;
		}
		
		@Override
		public TableColumn<PipelineInfo, String> createTitle(String title, double preferredWidth) {
			return createText("title", title, preferredWidth);
		}
		
		@Override
		public TableColumn<PipelineInfo, Double> createProgressBar(String title, double preferredWidth) {
			String propertyName = "progress";
			TableColumn<PipelineInfo, Double> column = new TableColumn<>(title);
			column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
			column.setCellFactory((col) -> new ProgressBarTableCell());
			column.setPrefWidth(preferredWidth);
			return column;
		}
		
		@Override
		public TableColumn<PipelineInfo, String> createState(String title, double preferredWidth) {
			String propertyName = "state";
			TableColumn<PipelineInfo, String> column = new TableColumn<>(title);
			column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
			column.setCellFactory((col) -> new StateTableCell());
			column.setPrefWidth(preferredWidth);
			return column;
		}
		
		@Override
		public TableColumn<PipelineInfo, String> createCurrent(String title, double preferredWidth) {
			return createText("current", title, preferredWidth);
		}
		
		@Override
		public TableColumn<PipelineInfo, String> createTotal(String title, double preferredWidth) {
			return createText("total", title, preferredWidth);
		}
		
		@Override
		public TableColumn<PipelineInfo, String> createSpeed(String title, double preferredWidth) {
			return createText("speed", title, preferredWidth);
		}
		
		@Override
		public TableColumn<PipelineInfo, String> createTimeLeft(String title, double preferredWidth) {
			return createText("timeLeft", title, preferredWidth);
		}
		
		@Override
		public TableColumn<PipelineInfo, String> createDestination(String title, double preferredWidth) {
			return createText("destination", title, preferredWidth);
		}
		
		@Override
		public TableColumn<PipelineInfo, String> createInformation(String title, double preferredWidth) {
			return createText("information", title, preferredWidth);
		}
		
		private static final class IconTableCell extends TableCell<PipelineInfo, String> {
			
			private ImageView icon;
			
			public IconTableCell() {
				getStyleClass().add("has-icon");
			}
			
			private final Image image() {
				MediaGetter getter = (MediaGetter) getTableRow().getItem().resolvedMedia().media().source().instance();
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
					setText(null);
					setGraphic(null);
					dispose();
				} else {
					value(item);
				}
			}
		}
		
		private static final class ProgressBarTableCell extends TableCell<PipelineInfo, Double> {
			
			private StackPane wrapper;
			private ProgressBar progressBar;
			private Text text;
			private double lastRegularProgress = PipelineProgress.NONE;
			
			public ProgressBarTableCell() {
				getStyleClass().add("has-progress-bar");
			}
		
			private final void initialize() {
				if(isInitialized()) {
					return;
				}
				
				wrapper = new StackPane();
				progressBar = new ProgressBar(0.0);
				text = new Text("0.0%");
				text.getStyleClass().add("text");
				wrapper.getChildren().addAll(progressBar, text);
				
				setGraphic(wrapper);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			}
			
			private final void dispose() {
				if(!isInitialized()) {
					return;
				}
				
				wrapper.getChildren().clear();
				text = null;
				progressBar = null;
				wrapper = null;
			}
			
			private final boolean isInitialized() {
				return wrapper != null;
			}
			
			private final void value(double value) {
				initialize();
				
				double progress = value;
				boolean textVisible = value >= 0.0 && value <= 1.0;
				
				if(progress == PipelineProgress.NONE) {
					progress = 0.0;
				} else if(progress == PipelineProgress.PROCESSING) {
					progress = PipelineProgress.INDETERMINATE;
					textVisible = true;
				} else if(progress == PipelineProgress.RESET) {
					progress = lastRegularProgress;
					textVisible = progress >= 0.0 && progress <= 1.0;
				} else if(progress != PipelineProgress.INDETERMINATE) {
					lastRegularProgress = progress;
				}
				
				progressBar.setProgress(progress);
				
				if(text.isVisible() != textVisible) {
					text.setVisible(textVisible);
				}
				
				if(textVisible) {
					text.setText(String.format(Locale.US, "%.2f%%", lastRegularProgress * 100.0));
				}
			}
			
			@Override
			protected void updateItem(Double item, boolean empty) {
				if(item == getItem() && isInitialized()) {
					return;
				}
				
				super.updateItem(item, empty);
				
				if(item == null) {
					setText(null);
					setGraphic(null);
					dispose();
				} else {
					value(item);
				}
			}
		}
		
		private static final class StateTableCell extends TableCell<PipelineInfo, String> {
			
			private static final String stateText(String state) {
				return Translator.maybeTranslate(state);
			}
			
			@Override
			protected void updateItem(String item, boolean empty) {
				if(item == getItem()) {
					return;
				}
				
				super.updateItem(item, empty);
				
				if(item == null) {
					setText(null);
					setGraphic(null);
				} else {
					setText(stateText(item));
				}
			}
			
			private static final class Translator {
				
				private static final Pattern REGEX_TRANSLATE = Pattern.compile("^tr\\(([^,]+),\\s*([^\\)]+)\\)$");
				
				private Translator() {
				}
				
				private static final boolean quickCanTranslate(String state) {
					return state != null && state.indexOf("tr(") == 0;
				}
				
				private static final Translation translation(String context) {
					int index = context.indexOf(':');
					
					if(index < 0) {
						return MediaDownloader.translation();
					}
					
					String schema = context.substring(0, index);
					switch(schema) {
						case "plugin": {
							String name = context.substring(index + 1);
							PluginFile plugin = Plugins.getLoaded(name);
							
							if(plugin != null) {
								return plugin.getInstance().translation();
							}
							
							// Fall-through
						}
						default: {
							return MediaDownloader.translation();
						}
					}
				}
				
				private static final String translate(String state) {
					Matcher matcher = REGEX_TRANSLATE.matcher(state);
					
					if(!matcher.matches()) {
						return state;
					}
					
					String context = matcher.group(1);
					String path = matcher.group(2);
					return translation(context).getSingle(path);
				}
				
				public static final String maybeTranslate(String state) {
					return quickCanTranslate(state) ? translate(state) : state;
				}
			}
		}
	}
	
	public static interface ContextMenuItemFactory {
		
		ContextMenuItem createStart(String title);
		ContextMenuItem createPause(String title);
		ContextMenuItem createTerminate(String title);
		ContextMenuItem createShowFile(String title);
		ContextMenuItem create(String title);
		SeparatorContextMenuItem createSeparator();
	}
	
	public static interface ColumnFactory {
		
		TableColumn<PipelineInfo, String> createSource(String title, double preferredWidth);
		TableColumn<PipelineInfo, String> createTitle(String title, double preferredWidth);
		TableColumn<PipelineInfo, Double> createProgressBar(String title, double preferredWidth);
		TableColumn<PipelineInfo, String> createState(String title, double preferredWidth);
		TableColumn<PipelineInfo, String> createCurrent(String title, double preferredWidth);
		TableColumn<PipelineInfo, String> createTotal(String title, double preferredWidth);
		TableColumn<PipelineInfo, String> createSpeed(String title, double preferredWidth);
		TableColumn<PipelineInfo, String> createTimeLeft(String title, double preferredWidth);
		TableColumn<PipelineInfo, String> createDestination(String title, double preferredWidth);
		TableColumn<PipelineInfo, String> createInformation(String title, double preferredWidth);
	}
	
	public static interface PipelineInfoData {
		
		void update(PipelineInfo info);
		
		public static final class OfText implements PipelineInfoData {
			
			private final String state;
			private final String text;
			
			public OfText(String state, String text) {
				this.state = state;
				this.text = text;
			}
			
			@Override
			public void update(PipelineInfo info) {
				info.progress(PipelineProgress.INDETERMINATE);
				info.state(state);
				info.current(null);
				info.total(null);
				info.speed(null);
				info.timeLeft(null);
				info.information(text);
			}
		}
		
		public static final class OfState implements PipelineInfoData {
			
			private final String state;
			private final String text;
			
			public OfState(String state, String text) {
				this.state = state;
				this.text = text;
			}
			
			@Override
			public void update(PipelineInfo info) {
				info.progress(PipelineProgress.RESET);
				info.state(state);
				info.information(text);
			}
		}
		
		public static final class OfEndText implements PipelineInfoData {
			
			private final String state;
			private final double progress;
			private final String text;
			
			public OfEndText(String state, double progress, String text) {
				this.state = state;
				this.progress = progress;
				this.text = text;
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
		
		public static final class OfTracker implements PipelineInfoData {
			
			private final Tracker tracker;
			
			public OfTracker(Tracker tracker) {
				this.tracker = tracker;
			}
			
			@Override
			public void update(PipelineInfo info) {
				info.progress(tracker.progress());
				info.state(tracker.state());
				info.current(null);
				info.total(null);
				info.speed(null);
				info.timeLeft(null);
				info.information(tracker.textProgress());
				tracker.view(info);
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
				info.state(tracker.state());
				info.current(Utils.OfFormat.size(tracker.current(), SizeUnit.BYTES, 2));
				info.total(Utils.OfFormat.size(tracker.total(), SizeUnit.BYTES, 2));
				info.speed(Utils.OfFormat.size(tracker.speed(), SizeUnit.BYTES, 2) + "/s");
				info.timeLeft(Utils.OfFormat.time(tracker.secondsLeft(), TimeUnit.SECONDS, false));
				info.information(text);
				tracker.view(info);
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
				info.state(tracker.state());
				info.current(Utils.OfFormat.time(tracker.currentTime(), TimeUnit.SECONDS, false));
				info.total(Utils.OfFormat.time(tracker.totalTime(), TimeUnit.SECONDS, false));
				info.speed(null);
				info.timeLeft(null);
				info.information(text);
				tracker.view(info);
			}
		}
	}
	
	public static class Stats {
		
		private final List<PipelineInfo> infos;
		
		private final int count;
		private final int started;
		private final int done;
		private final int stopped;
		
		protected Stats(List<PipelineInfo> infos, int count, int started, int done, int stopped) {
			this.infos = infos;
			this.count = count;
			this.started = started;
			this.done = done;
			this.stopped = stopped;
		}
		
		public static final Stats from(List<PipelineInfo> infos) {
			int count = infos.size();
			int started = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isStarted).count();
			int done = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isDone).count();
			int stopped = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isStopped).count();
			
			return new Stats(infos, count, started, done, stopped);
		}
		
		public boolean anyNonPaused() {
			return PipelineTableView.anyNonPaused(infos);
		}
		
		public boolean anyTerminable() {
			return PipelineTableView.anyTerminable(infos);
		}
		
		public int count() {
			return count;
		}
		
		public int started() {
			return started;
		}
		
		public int done() {
			return done;
		}
		
		public int stopped() {
			return stopped;
		}
	}
	
	public static class ContextMenuItem extends MenuItem {
		
		protected ObjectProperty<Pair<ContextMenuItem, Stats>> onContextMenuShowing;
		
		protected ContextMenuItem(String title) {
			super(title);
		}
		
		public ContextMenuItem setOnActivated(EventHandler<ActionEvent> listener) {
			setOnAction(listener);
			return this; // Allow chaining
		}
		
		public ContextMenuItem addOnContextMenuShowing(ChangeListener<? super Pair<ContextMenuItem, Stats>> listener) {
			onContextMenuShowing().addListener(listener);
			return this; // Allow chaining
		}
		
		public ObjectProperty<Pair<ContextMenuItem, Stats>> onContextMenuShowing() {
			if(onContextMenuShowing == null) {
				onContextMenuShowing = new SimpleObjectProperty<>();
			}
			
			return onContextMenuShowing;
		}
	}
	
	public static class SeparatorContextMenuItem extends SeparatorMenuItem {
		
		protected SeparatorContextMenuItem() {
		}
	}
	
	public static final class PipelineInfo implements TrackerView {
		
		private static final long MIN_UPDATE_DIFF_TIME = 250L * 1000000L; // 250 ms
		
		public static final String TEXT_NONE = null;
		
		private final Pipeline pipeline;
		private final ResolvedMedia resolvedMedia;
		private PipelineMedia media;
		
		private StringProperty sourceProperty;
		private StringProperty titleProperty;
		private StringProperty destinationProperty;
		private DoubleProperty progressProperty;
		private StringProperty stateProperty;
		private StringProperty currentProperty;
		private StringProperty totalProperty;
		private StringProperty speedProperty;
		private StringProperty timeLeftProperty;
		private StringProperty informationProperty;
		
		private long lastUpdateTime = Long.MIN_VALUE;
		private volatile boolean stateUpdated = false;
		private boolean isQueued;
		
		public PipelineInfo(Pipeline pipeline, ResolvedMedia resolvedMedia) {
			this.pipeline = Objects.requireNonNull(pipeline);
			this.resolvedMedia = Objects.requireNonNull(resolvedMedia);
			this.pipeline.getEventRegistry().addMany((o) -> stateUpdated = true, PipelineEvent.values());
		}
		
		private final StringProperty newStateProperty() {
			StringProperty property = new SimpleStringProperty();
			stateUpdated = true;
			property.addListener((o, ov, nv) -> stateUpdated = true);
			return property;
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
		
		public void start() {
			Pipeline pipeline = pipeline();
			
			if(pipeline.isStarted()) {
				return;
			}
			
			ResolvedMedia media = resolvedMedia();
			PipelineMedia pipelineMedia = PipelineMedia.of(media.media(), media.path(), media.configuration(),
				DownloadConfiguration.ofDefault());
			PipelineResult<?> input = MediaPipelineResult.of(pipelineMedia);
			
			try {
				media(pipelineMedia);
				pipeline.setInput(input);
				pipeline.start();
				pipelineMedia.awaitSubmitted();
			} catch(Exception ex) {
				MediaDownloader.error(ex);
			}
		}
		
		public void stop() {
			Pipeline pipeline = pipeline();
			
			if(!pipeline.isStarted() || (!pipeline.isRunning() && !pipeline.isPaused())) {
				return;
			}
			
			try {
				pipeline.stop();
				pipeline.waitFor();
				
				Cancellable cancellable;
				if((cancellable = media().submitValue()) != null) {
					cancellable.cancel();
				}
			} catch(Exception ex) {
				MediaDownloader.error(ex);
			}
		}
		
		public void pause() {
			Pipeline pipeline = pipeline();
			
			if(pipeline.isPaused() || !pipeline.isStarted() || pipeline.isDone() || pipeline.isStopped()) {
				return;
			}
			
			try {
				pipeline.pause();
			} catch(Exception ex) {
				MediaDownloader.error(ex);
			}
		}
		
		public void resume() {
			Pipeline pipeline = pipeline();
			
			if(!pipeline.isPaused() || !pipeline.isStarted() || pipeline.isDone() || pipeline.isStopped()) {
				return;
			}
			
			try {
				pipeline.resume();
			} catch(Exception ex) {
				MediaDownloader.error(ex);
			}
		}
		
		public void isQueued(boolean isQueued) {
			this.isQueued = isQueued;
		}
		
		public StringProperty sourceProperty() {
			return sourceProperty == null
						? sourceProperty = new SimpleStringProperty(source())
						: sourceProperty;
		}
		
		public StringProperty titleProperty() {
			return titleProperty == null
						? titleProperty = new SimpleStringProperty(title())
						: titleProperty;
		}
		
		public StringProperty destinationProperty() {
			return destinationProperty == null
						? destinationProperty = new SimpleStringProperty(destination())
						: destinationProperty;
		}
		
		public DoubleProperty progressProperty() {
			return progressProperty == null
						? progressProperty = new SimpleDoubleProperty()
						: progressProperty;
		}
		
		public StringProperty stateProperty() {
			return stateProperty == null
						? stateProperty = newStateProperty()
						: stateProperty;
		}
		
		public StringProperty currentProperty() {
			return currentProperty == null
						? currentProperty = new SimpleStringProperty()
						: currentProperty;
		}
		
		public StringProperty totalProperty() {
			return totalProperty == null
						? totalProperty = new SimpleStringProperty()
						: totalProperty;
		}
		
		public StringProperty speedProperty() {
			return speedProperty == null
						? speedProperty = new SimpleStringProperty()
						: speedProperty;
		}
		
		public StringProperty timeLeftProperty() {
			return timeLeftProperty == null
						? timeLeftProperty = new SimpleStringProperty()
						: timeLeftProperty;
		}
		
		public StringProperty informationProperty() {
			return informationProperty == null
						? informationProperty = new SimpleStringProperty()
						: informationProperty;
		}
		
		@Override
		public void progress(double progress) {
			progressProperty().set(progress);
		}
		
		@Override
		public void state(String state) {
			stateProperty().set(state);
		}
		
		@Override
		public void current(String current) {
			currentProperty().set(current);
		}
		
		@Override
		public void total(String total) {
			totalProperty().set(total);
		}
		
		@Override
		public void speed(String speed) {
			speedProperty().set(speed);
		}
		
		@Override
		public void timeLeft(String timeLeft) {
			timeLeftProperty().set(timeLeft);
		}
		
		@Override
		public void information(String information) {
			informationProperty().set(information);
		}
		
		public void media(PipelineMedia media) {
			this.media = media;
		}
		
		public String source() {
			return resolvedMedia.media().source().toString();
		}
		
		public String title() {
			return resolvedMedia.media().metadata().title();
		}
		
		public String destination() {
			return resolvedMedia.path().toString();
		}
		
		@Override
		public double progress() {
			return progressProperty().get();
		}
		
		@Override
		public String state() {
			return stateProperty().get();
		}
		
		@Override
		public String current() {
			return currentProperty().get();
		}
		
		@Override
		public String total() {
			return totalProperty().get();
		}
		
		@Override
		public String speed() {
			return speedProperty().get();
		}
		
		@Override
		public String timeLeft() {
			return timeLeftProperty().get();
		}
		
		@Override
		public String information() {
			return informationProperty().get();
		}
		
		public Pipeline pipeline() {
			return pipeline;
		}
		
		public ResolvedMedia resolvedMedia() {
			return resolvedMedia;
		}
		
		public boolean isQueued() {
			return isQueued;
		}
		
		public PipelineMedia media() {
			return media;
		}
	}
}