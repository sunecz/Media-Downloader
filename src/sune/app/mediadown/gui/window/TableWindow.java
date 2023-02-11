package sune.app.mediadown.gui.window;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.MediaGetter;
import sune.app.mediadown.concurrent.SyncObject;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.event.PipelineEvent;
import sune.app.mediadown.gui.DraggableWindow;
import sune.app.mediadown.gui.ProgressWindow;
import sune.app.mediadown.gui.ProgressWindow.ProgressAction;
import sune.app.mediadown.gui.control.FixedTextField;
import sune.app.mediadown.gui.table.MediaEnginePipelineTask;
import sune.app.mediadown.gui.table.MediaGetterPipelineTask;
import sune.app.mediadown.gui.table.TableWindowPipelineTaskBase;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.util.FXUtils;
import sune.app.mediadown.util.History;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.01.27 */
public final class TableWindow extends DraggableWindow<BorderPane> {
	
	public static final String NAME = "table";
	
	private TableView<Object> table;
	private HBox boxBottom;
	private TextField txtSearch;
	private Button btnSelect;
	private Button btnGoBack;
	private Button btnReload;
	
	private final SyncObject lockSelect = new SyncObject();
	private final History<PipelineTask<?>> history = new History<>();
	private final ObservableList<Object> items = FXCollections.observableArrayList();
	private final List<Object> selection = new ArrayList<>();
	private Pipeline pipeline;
	private ProgressWindow progressWindow;
	private TableWindowPipelineTaskBase<?, ?> prevTask;
	
	public TableWindow() {
		super(NAME, new BorderPane(), 650.0, 470.0);
		initModality(Modality.APPLICATION_MODAL);
		setOnCloseRequest((e) -> terminateAndClose());
		boxBottom = new HBox(5);
		txtSearch = new FixedTextField();
		btnSelect = new Button(translation.getSingle("buttons.select"));
		btnGoBack = new Button(translation.getSingle("buttons.goback"));
		btnReload = new Button(translation.getSingle("buttons.reload"));
		btnSelect.setOnAction((e) -> select());
		btnGoBack.setOnAction((e) -> goBack());
		btnReload.setOnAction((e) -> reload());
		txtSearch.setMinWidth(100.0);
		btnSelect.setMinWidth(80.0);
		btnGoBack.setMinWidth(80.0);
		btnReload.setMinWidth(80.0);
		txtSearch.setPromptText(translation.getSingle("etc.prompt_text_search"));
		txtSearch.textProperty().addListener((o, ov, nv) -> updateSearchResults(nv));
		boxBottom.setAlignment(Pos.CENTER_RIGHT);
		boxBottom.setPadding(new Insets(5, 0, 0, 0));
		HBox boxFill = new HBox();
		HBox.setHgrow(boxFill, Priority.ALWAYS);
		btnSelect.setDisable(true);
		btnGoBack.setDisable(true);
		btnReload.setDisable(true);
		txtSearch.setDisable(true);
		boxBottom.getChildren().addAll(btnGoBack, txtSearch, btnReload, btnSelect);
		HBox.setHgrow(txtSearch, Priority.ALWAYS);
		content.setPadding(new Insets(10));
		content.setBottom(boxBottom);
		FXUtils.onWindowShow(this, () -> {
			Stage parent = (Stage) args.get("parent");
			if(parent != null) centerWindow(parent);
		});
	}
	
	/** @since 00.02.07 */
	private final TableWindowPipelineTaskBase<Object, PipelineResult<?>> currentTask() {
		return Utils.cast(pipeline.getTask());
	}
	
	private final void pipelineOnUpdate(Pair<Pipeline, PipelineTask<?>> pair) {
		if(prevTask != null)
			FXUtils.unreflectChanges(prevTask.getResultList());
		TableWindowPipelineTaskBase<?, ?> task
			= (TableWindowPipelineTaskBase<?, ?>) pair.b;
		history.add(task);
		clearSearchResults();
		setCanGoBack();
		@SuppressWarnings("unchecked")
		TableView<Object> table = (TableView<Object>) task.getTable(this);
		if(table == null) return;
		setTable(table); // Only set the table if it is non-null
		FXUtils.thread(() -> setTitle(task.getTitle(this)));
		items.clear(); // Clear all the previous items
		FXUtils.reflectChanges(task.getResultList(), items);
		FXUtils.reflectChanges(task.getResultList(), table.getItems());
		prevTask = task;
	}
	
	private final void pipelineOnError(Pair<Pipeline, Exception> pair) {
		MediaDownloader.error(pair.b);
	}
	
	private final void hideProgressWindow() {
		if(progressWindow != null)
			FXUtils.thread(progressWindow::hide);
	}
	
	private final void showProgressWindow() {
		if(progressWindow != null && pipeline.isRunning())
			FXUtils.thread(progressWindow::show);
	}
	
	public final <B> List<B> waitAndGetSelection(List<B> items) {
		waitForSelection();
		return getSelection();
	}
	
	private final void waitForSelection() {
		hideProgressWindow();
		FXUtils.thread(table::refresh); // Fix desync
		FXUtils.thread(() -> {
			// Sort the table rows, if needed
			if(!table.getSortOrder().isEmpty())
				table.sort();
		});
		setSearchable(true);
		setCanReload();
		hideEmptyColumns();
		FXUtils.thread(() -> txtSearch.requestFocus());
		lockSelect.await();
		showProgressWindow();
	}
	
	private final void notifySelection() {
		lockSelect.unlock();
		setSearchable(false);
		setCanReload();
	}
	
	@SuppressWarnings("unchecked")
	private final <T> List<T> getSelection() {
		return selection.stream().map((o) -> (T) o).collect(Collectors.toList());
	}
	
	public final void submit(ProgressAction action) {
		progressWindow = ProgressWindow.submitAction(this, action);
	}
	
	public final void goBack() {
		PipelineTask<?> result = history.backwardAndGet();
		history.backward(); // Add the result to the correct position
		setCanGoBack();
		pipeline.reset(result);
		notifySelection();
	}
	
	/** @since 00.02.07 */
	public final void reload() {
		TableWindowPipelineTaskBase<Object, PipelineResult<?>> task = currentTask();
		// Allow tasks to do some stuff before the reload (such as clear cache)
		task.beforeReload();
		
		// Remove history entry
		history.backward();
		setCanGoBack();
		
		// Reset the task
		pipeline.reset(task);
		notifySelection();
		
		// Allow tasks to do some stuff after the reload
		task.afterReload();
	}
	
	private final <T> TableRow<T> createTableRow(TableView<T> table) {
		TableRow<T> row = new TableRow<>();
		row.addEventHandler(MouseEvent.MOUSE_CLICKED, (e) -> {
			if(e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
				select();
		});
		return row;
	}
	
	private final void hideEmptyColumns() {
		FXUtils.thread(() -> {
			for(TableColumn<Object, ?> column : table.getColumns()) {
				boolean hideIfEmpty = (boolean) column.getProperties().getOrDefault("hideIfEmpty", false);
				if(!hideIfEmpty) continue; // Ignore column
				boolean isEmpty = true;
				for(Object item : table.getItems()) {
					Object value = column.getCellObservableValue(item).getValue();
					if(value instanceof String && !((String) value).isEmpty()) {
						isEmpty = false; break; // Cell not empty
					}
				}
				if(isEmpty) column.setVisible(false); // Hide column
			}
		});
	}
	
	private final void setSelectable(boolean flag) {
		FXUtils.thread(() -> btnSelect.setDisable(!flag));
	}
	
	private final void setCanGoBack() {
		boolean flag = history.canGoBackward();
		FXUtils.thread(() -> btnGoBack.setDisable(!flag));
	}
	
	private final void setSearchable(boolean flag) {
		FXUtils.thread(() -> txtSearch.setDisable(!flag));
	}
	
	/** @since 00.02.07 */
	private final void setCanReload() {
		boolean flag = currentTask().canReload();
		FXUtils.thread(() -> btnReload.setDisable(!flag));
	}
	
	private final void setTable(TableView<Object> newTable) {
		table = newTable;
		table.setRowFactory(this::createTableRow);
		table.getSelectionModel().selectedIndexProperty().addListener((o, ov, nv) -> setSelectable(true));
		FXUtils.thread(() -> content.setCenter(table));
		setSelectable(false);
	}
	
	private final void select() {
		selection.clear();
		selection.addAll(table.getSelectionModel().getSelectedItems());
		if(!selection.isEmpty()) notifySelection();
	}
	
	private final void clearSearchResults() {
		FXUtils.thread(() -> txtSearch.setText(""));
	}
	
	private final void updateSearchResults(String text) {
		TableWindowPipelineTaskBase<Object, PipelineResult<?>> task = currentTask();
		String normalizedText = Utils.normalize(text).toLowerCase();
		ObservableList<Object> tableItems = table.getItems();
		if(text == null || text.isEmpty()) {
			FXUtils.thread(() -> tableItems.setAll(items));
		} else {
			List<Object> filtered = items.stream()
					.filter((item) -> task.filter(item, normalizedText))
					.collect(Collectors.toList());
			FXUtils.thread(() -> tableItems.setAll(filtered));
		}
		FXUtils.thread(() -> {
			// Fix: "index exceeds maxCellCount" JavaFX error
			if(!tableItems.isEmpty())
				table.scrollTo(0);
		});
	}
	
	private final void terminateAndClose() {
		Ignore.callVoid(pipeline::stop, MediaDownloader::error);
		notifySelection();
		if(progressWindow != null) {
			FXUtils.thread(progressWindow::close);
			progressWindow = null;
		}
		FXUtils.thread(this::close);
	}
	
	private final void resetAndInit() {
		if(prevTask != null)
			FXUtils.unreflectChanges(prevTask.getResultList());
		lockSelect.unlock();
		history.clear();
		items.clear();
		selection.clear();
		pipeline = Pipeline.create();
		pipeline.addEventListener(PipelineEvent.UPDATE, this::pipelineOnUpdate);
		pipeline.addEventListener(PipelineEvent.ERROR, this::pipelineOnError);
		if(progressWindow != null) {
			FXUtils.thread(progressWindow::close);
			progressWindow = null;
		}
		prevTask = null;
	}
	
	public final <R extends PipelineResult<?>> R show(Stage parent, PipelineTask<?> task) throws Exception {
		resetAndInit();
		pipeline.addTask(task);
		pipeline.start();
		FXUtils.thread(() -> show(parent));
		pipeline.waitFor();
		if(prevTask != null)
			FXUtils.unreflectChanges(prevTask.getResultList());
		FXUtils.thread(this::close);
		@SuppressWarnings("unchecked")
		R result = (R) pipeline.getResult();
		return result;
	}
	
	public final <R extends PipelineResult<?>> R show(Stage parent, MediaEngine engine) throws Exception {
		return show(parent, new MediaEnginePipelineTask(this, engine));
	}
	
	/** @since 00.02.07 */
	public final <R extends PipelineResult<?>> R show(Stage parent, MediaGetter getter, URI uri) throws Exception {
		return show(parent, new MediaGetterPipelineTask(this, getter, uri));
	}
}