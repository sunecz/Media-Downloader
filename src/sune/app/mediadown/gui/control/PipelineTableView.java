package sune.app.mediadown.gui.control;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
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
import sune.app.mediadown.concurrent.Threads;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.entity.MediaGetter;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.PipelineEvent;
import sune.app.mediadown.event.QueueEvent;
import sune.app.mediadown.event.tracker.ConversionTracker;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.PipelineProgress;
import sune.app.mediadown.event.tracker.PipelineStates;
import sune.app.mediadown.event.tracker.Tracker;
import sune.app.mediadown.event.tracker.TrackerView;
import sune.app.mediadown.gui.control.PipelineTableView.PipelineInfo;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.language.Translator;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.pipeline.MediaPipelineResult;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineMedia;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.Pair;
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
		return infos.stream().anyMatch((i) -> {
			Pipeline p = i.pipeline();
			return i.isPausing() || (p.isStarted() && p.isRunning());
		});
	}
	
	private static final boolean anyTerminable(List<PipelineInfo> infos) {
		return infos.stream().anyMatch((i) -> {
			Pipeline p = i.pipeline();
			return i.isStopping() || (p.isStarted() && (p.isRunning() || p.isPaused()));
		});
	}
	
	/** @since 00.02.09 */
	private static final boolean anyRetryable(List<PipelineInfo> infos) {
		return infos.stream().anyMatch((i) -> {
			Pipeline p = i.pipeline();
			return i.isRetrying() || (p.isDone() || p.isStopped() || p.isError());
		});
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
		
		final List<PipelineInfo> finalInfos = infos;
		Threads.executeEnsured(() -> {
			finalInfos.stream().forEachOrdered(PipelineInfo::stop);
		});
	}
	
	/** @since 00.02.09 */
	private final boolean isActivePipeline(PipelineInfo info) {
		Pipeline pipeline = info.pipeline();
		return pipeline.isRunning() || pipeline.isPaused();
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
	
	public void pause(List<PipelineInfo> infos) {
		Threads.executeEnsured(() -> {
			infos.stream().forEachOrdered(PipelineInfo::pause);
		});
	}
	
	public void pauseAll() {
		pause(pipelines());
	}
	
	public void pauseSelected() {
		pause(selectedPipelines());
	}
	
	public void resume(List<PipelineInfo> infos) {
		Threads.executeEnsured(() -> {
			infos.stream().forEachOrdered(PipelineInfo::resume);
		});
	}
	
	public void resumeAll() {
		resume(pipelines());
	}
	
	public void resumeSelected() {
		resume(selectedPipelines());
	}
	
	/** @since 00.02.09 */
	public void retry(List<PipelineInfo> infos) {
		Threads.executeEnsured(() -> {
			infos.stream().forEachOrdered(PipelineInfo::retry);
		});
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
	
	/** @since 00.02.09 */
	public List<PipelineInfo> activePipelines() {
		return pipelines().stream().filter(this::isActivePipeline).collect(Collectors.toList());
	}
	
	/** @since 00.02.09 */
	public boolean hasActivePipelines() {
		return pipelines().stream().anyMatch(this::isActivePipeline);
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
				
				table.start(infos);
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
					table.pause(infos);
				} else {
					table.resume(infos);
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
					table.stop(infos);
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
		
		/** @since 00.02.09 */
		@Override
		public ContextMenuItem createRetry(String title) {
			ContextMenuItem menuItem = new ContextMenuItem(title);
			
			menuItem.setOnAction((e) -> {
				List<PipelineInfo> infos = table.selectedPipelines();
				
				if(infos.isEmpty()) {
					return; // Nothing to retry
				}
				
				if(anyRetryable(infos)) {
					table.retry(infos);
				}
			});
			
			menuItem.addOnContextMenuShowing((o, ov, pair) -> {
				ContextMenuItem item = pair.a;
				Stats stats = pair.b;
				
				boolean anyRetryable = stats.done() > 0 || stats.stopped() > 0 || stats.error() > 0;
				item.setDisable(!anyRetryable);
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
		}
	}
	
	public static interface ContextMenuItemFactory {
		
		ContextMenuItem createStart(String title);
		ContextMenuItem createPause(String title);
		ContextMenuItem createTerminate(String title);
		/** @since 00.02.09 */
		ContextMenuItem createRetry(String title);
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
		
		/** @since 00.02.09 */
		String state();
		
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
			
			@Override
			public String state() {
				return state;
			}
		}
		
		public static final class OfState implements PipelineInfoData {
			
			private final String state;
			private final String text;
			/** @since 00.02.09 */
			private final double progress;
			
			public OfState(String state, String text) {
				this(state, text, PipelineProgress.RESET);
			}
			
			/** @since 00.02.09 */
			public OfState(String state, String text, double progress) {
				this.state = state;
				this.text = text;
				this.progress = progress;
			}
			
			@Override
			public void update(PipelineInfo info) {
				info.progress(progress);
				info.state(state);
				info.information(text);
			}
			
			@Override
			public String state() {
				return state;
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
			
			@Override
			public String state() {
				return state;
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
			
			@Override
			public String state() {
				return tracker.state();
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
			
			@Override
			public String state() {
				return tracker.state();
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
			
			@Override
			public String state() {
				return tracker.state();
			}
		}
	}
	
	public static class Stats {
		
		private final List<PipelineInfo> infos;
		
		private final int count;
		private final int started;
		private final int done;
		private final int stopped;
		/** @since 00.02.09 */
		private final int error;
		
		protected Stats(List<PipelineInfo> infos, int count, int started, int done, int stopped, int error) {
			this.infos = infos;
			this.count = count;
			this.started = started;
			this.done = done;
			this.stopped = stopped;
			this.error = error;
		}
		
		public static final Stats from(List<PipelineInfo> infos) {
			int count = infos.size();
			int started = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isStarted).count();
			int done = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isDone).count();
			int stopped = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isStopped).count();
			int error = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isError).count();
			
			return new Stats(infos, count, started, done, stopped, error);
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
		
		/** @since 00.02.09 */
		public int error() {
			return error;
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
		private static final Event<? extends EventType, ?>[] STATE_UPDATE_EVENTS = Utils.merge(
			PipelineEvent.values(), QueueEvent.values()
		);
		
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
		/** @since 00.02.09 */
		private volatile String lastState = null;
		private boolean isQueued;
		
		/** @since 00.02.09 */
		private volatile boolean isPausing;
		/** @since 00.02.09 */
		private volatile boolean isResuming;
		/** @since 00.02.09 */
		private volatile boolean isStopping;
		/** @since 00.02.09 */
		private volatile boolean isRetrying;
		
		public PipelineInfo(Pipeline pipeline, ResolvedMedia resolvedMedia) {
			this.pipeline = Objects.requireNonNull(pipeline);
			this.resolvedMedia = Objects.requireNonNull(resolvedMedia);
			this.pipeline.getEventRegistry().addMany((o) -> lastState = null, STATE_UPDATE_EVENTS);
		}
		
		public void update(PipelineInfoData data) {
			boolean needsUpdate = true;
			long now = System.nanoTime();
			String prevState = lastState;
			String newState = data.state();
			
			if(prevState != null && prevState.equals(newState)) {
				needsUpdate = now - lastUpdateTime >= MIN_UPDATE_DIFF_TIME || lastUpdateTime == Long.MIN_VALUE;
			}
			
			if(needsUpdate) {
				FXUtils.thread(() -> data.update(this));
				lastState = newState;
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
			PipelineResult input = MediaPipelineResult.of(pipelineMedia);
			
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
			
			if(pipeline.isStopped() || !pipeline.isStarted() || pipeline.isDone()) {
				return;
			}
			
			isStopping = true;
			update(new PipelineInfoData.OfState(
				PipelineStates.STOPPING, PipelineInfo.TEXT_NONE, PipelineProgress.INDETERMINATE
			));
			
			try {
				pipeline.stop();
				pipeline.waitFor();
			} catch(Exception ex) {
				MediaDownloader.error(ex);
			} finally {
				isStopping = false;
			}
		}
		
		public void pause() {
			Pipeline pipeline = pipeline();
			
			if(pipeline.isPaused() || !pipeline.isStarted() || pipeline.isDone() || pipeline.isStopped()) {
				return;
			}
			
			isPausing = true;
			update(new PipelineInfoData.OfState(
				PipelineStates.PAUSING, PipelineInfo.TEXT_NONE, PipelineProgress.INDETERMINATE
			));
			
			try {
				pipeline.pause();
			} catch(Exception ex) {
				MediaDownloader.error(ex);
			} finally {
				isPausing = false;
			}
		}
		
		public void resume() {
			Pipeline pipeline = pipeline();
			
			if(!pipeline.isPaused() || !pipeline.isStarted() || pipeline.isDone() || pipeline.isStopped()) {
				return;
			}
			
			isResuming = true;
			update(new PipelineInfoData.OfState(
				PipelineStates.RESUMING, PipelineInfo.TEXT_NONE, PipelineProgress.INDETERMINATE
			));
			
			try {
				pipeline.resume();
			} catch(Exception ex) {
				MediaDownloader.error(ex);
			} finally {
				isResuming = false;
			}
		}
		
		/** @since 00.02.09 */
		public void retry() {
			Pipeline pipeline = pipeline();
			
			if(!pipeline.isDone() && !pipeline.isStopped() && !pipeline.isError()) {
				return;
			}
			
			isRetrying = true;
			update(new PipelineInfoData.OfState(
				PipelineStates.RETRYING, PipelineInfo.TEXT_NONE, PipelineProgress.INDETERMINATE
			));
			
			try {
				pipeline.waitFor();
				pipeline.reset();
				isQueued(false);
				start();
			} catch(Exception ex) {
				MediaDownloader.error(ex);
			} finally {
				isRetrying = false;
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
						? stateProperty = new SimpleStringProperty()
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
		
		/** @since 00.02.09 */
		public boolean isPausing() {
			return isPausing;
		}
		
		/** @since 00.02.09 */
		public boolean isResuming() {
			return isResuming;
		}
		
		/** @since 00.02.09 */
		public boolean isStopping() {
			return isStopping;
		}
		
		/** @since 00.02.09 */
		public boolean isRetrying() {
			return isRetrying;
		}
	}
}