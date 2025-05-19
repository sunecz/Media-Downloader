package sune.app.mediadown.gui.control;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import sune.app.mediadown.entity.MediaGetter;
import sune.app.mediadown.event.tracker.PipelineProgress;
import sune.app.mediadown.gui.util.FXUtils;
import sune.app.mediadown.language.Translator;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineInfo;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

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
}