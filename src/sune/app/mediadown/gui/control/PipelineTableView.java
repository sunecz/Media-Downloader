package sune.app.mediadown.gui.control;


import java.util.List;
import java.util.Locale;

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
import sune.app.mediadown.entity.MediaGetter;
import sune.app.mediadown.event.ListEvent;
import sune.app.mediadown.event.ListEvent.ListChange;
import sune.app.mediadown.event.tracker.PipelineProgress;
import sune.app.mediadown.gui.util.FXUtils;
import sune.app.mediadown.language.Translator;
import sune.app.mediadown.os.OS;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineInfo;
import sune.app.mediadown.pipeline.PipelineInfos;
import sune.app.mediadown.pipeline.PipelineInfos.Stats;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.08 */
public class PipelineTableView extends TableView<PipelineInfo> {
	
	private final PipelineInfos pipelineInfos;
	
	private ColumnFactory columnFactory;
	private ContextMenuItemFactory contextMenuItemFactory;
	
	private ObjectProperty<PipelineInfo> onItemDoubleClicked;
	
	public PipelineTableView() {
		pipelineInfos = new PipelineInfos();
		
		pipelineInfos.addEventListener(ListEvent.CHANGED, (c) -> {
			@SuppressWarnings("unchecked")
			ListChange<PipelineInfo> change = (ListChange<PipelineInfo>) c;
			
			if(change.hasAdded()) {
				FXUtils.thread(() -> getItems().addAll(change.added()));
			}
			
			if(change.hasRemoved()) {
				FXUtils.thread(() -> getItems().removeAll(change.removed()));
			}
		});
		
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
	
	private static final void showFile(PipelineInfo info) {
		Ignore.callVoid(() -> OS.current().highlight(info.resolvedMedia().path()), MediaDownloader::error);
	}
	
	private final ContextMenu initializeContextMenu() {
		ContextMenu contextMenu = new ContextMenu();
		
		contextMenu.setAutoFix(true);
		contextMenu.setAutoHide(true);
		
		return contextMenu;
	}
	
	public void add(PipelineInfo info) { pipelineInfos.add(info); }
	public void add(List<PipelineInfo> infos) { pipelineInfos.add(infos); }
	public void remove(PipelineInfo info) { pipelineInfos.remove(info); }
	public void remove(List<PipelineInfo> infos) { pipelineInfos.remove(infos); }
	public void start(List<PipelineInfo> infos) { pipelineInfos.start(infos); }
	public void startAll() { pipelineInfos.startAll(); }
	public void stop(List<PipelineInfo> infos) { pipelineInfos.stop(infos); }
	public void stopAll() { pipelineInfos.stopAll(); }
	public void pause(List<PipelineInfo> infos) { pipelineInfos.pause(infos); }
	public void pauseAll() { pipelineInfos.pauseAll(); }
	public void resume(List<PipelineInfo> infos) { pipelineInfos.resume(infos); }
	public void resumeAll() { pipelineInfos.resumeAll(); }
	/** @since 00.02.09 */
	public void retry(List<PipelineInfo> infos) { pipelineInfos.retry(infos); }
	public List<PipelineInfo> pipelines() { return pipelineInfos.pipelines(); }
	/** @since 00.02.09 */
	public List<PipelineInfo> activePipelines() { return pipelineInfos.activePipelines(); }
	/** @since 00.02.09 */
	public boolean hasActivePipelines() { return pipelineInfos.hasActivePipelines(); }
	
	public void startSelected() { pipelineInfos.start(selectedPipelines()); }
	public void stopSelected() { pipelineInfos.stop(selectedPipelines()); }
	public void pauseSelected() { pipelineInfos.pause(selectedPipelines()); }
	public void resumeSelected() { pipelineInfos.resume(selectedPipelines()); }
	
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
				
				if(PipelineInfos.anyNonPaused(infos)) {
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
				
				if(PipelineInfos.anyTerminable(infos)) {
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
				
				if(PipelineInfos.anyRetryable(infos)) {
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
			column.setCellFactory((col) -> new PipelineInfoIconTableCell());
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
		
		private static final class PipelineInfoIconTableCell extends IconTableCell<PipelineInfo, String> {
			
			private final Image image() {
				MediaGetter getter = (MediaGetter) (
					getTableRow().getItem().resolvedMedia().media().source().instance()
				);
				
				return getter != null ? getter.icon() : null;
			}
			
			@Override
			protected ImageView iconView(String value) {
				Image image = image();
				
				if(image == null) {
					return null;
				}
				
				ImageView view = new ImageView(image);
				view.setFitWidth(24.0);
				view.setFitHeight(24.0);
				return view;
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