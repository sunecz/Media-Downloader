package sune.app.mediadown.gui.table;

import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import sune.app.mediadown.entity.MediaEngine;
import sune.app.mediadown.entity.Program;
import sune.app.mediadown.gui.GUI;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.report.Report;
import sune.app.mediadown.report.Report.Reason;
import sune.app.mediadown.report.ReportContext;
import sune.app.mediadown.resource.cache.GlobalCache;
import sune.app.mediadown.task.ListTask;
import sune.app.mediadown.task.Tasks;
import sune.app.mediadown.util.ClipboardUtils;
import sune.app.mediadown.util.Utils;

/** @since 00.01.27 */
public final class MediaEnginePipelineTask extends TableWindowPipelineTaskBase<Program, MediaEnginePipelineResult> {
	
	private final MediaEngine engine;
	
	public MediaEnginePipelineTask(TableWindow window, MediaEngine engine) {
		super(window);
		this.engine = engine;
	}
	
	/** @since 00.02.09 */
	private final ContextMenu newContextMenu(TableWindow window, TableView<Program> table) {
		Translation translation = window.getTranslation();
		ContextMenu menu = new ContextMenu();
		
		MenuItem itemCopyURL = new MenuItem(translation.getSingle("tables.engines.context_menu.copy_url"));
		itemCopyURL.setOnAction((e) -> {
			Program item = table.getSelectionModel().getSelectedItem();
			ClipboardUtils.copy(item.uri().normalize().toString());
		});
		
		MenuItem itemReportBroken = new MenuItem(translation.getSingle("tables.engines.context_menu.report_broken"));
		itemReportBroken.setOnAction((e) -> {
			Program item = table.getSelectionModel().getSelectedItem();
			GUI.showReportWindow(window, Report.Builders.ofProgram(
				item, Reason.BROKEN,
				ReportContext.ofMediaGetter(engine)
			));
		});
		
		menu.getItems().addAll(itemCopyURL, itemReportBroken);
		menu.showingProperty().addListener((o, ov, isShowing) -> {
			if(!isShowing) return;
			boolean noSelectedItems = table.getSelectionModel().getSelectedItems().isEmpty();
			itemCopyURL.setDisable(noSelectedItems);
			itemReportBroken.setDisable(noSelectedItems);
		});
		
		return menu;
	}
	
	@Override
	protected final ListTask<Program> getTask() {
		return Tasks.cachedList(GlobalCache::ofPrograms, engine.getClass(), (k) -> engine.getPrograms());
	}
	
	@Override
	protected final MediaEnginePipelineResult getResult(TableWindow window, List<Program> result) {
		return new MediaEnginePipelineResult(window, engine, result);
	}
	
	@Override
	protected String getProgressText(TableWindow window) {
		return window.getTranslation().getSingle("progress.programs");
	}
	
	@Override
	public final TableView<Program> getTable(TableWindow window) {
		TableView<Program> table = new TableView<>();
		Translation translation = window.getTranslation();
		String titleTitle = translation.getSingle("tables.engines.columns.title");
		TableColumn<Program, String> columnTitle = new TableColumn<>(titleTitle);
		columnTitle.setCellValueFactory((c) -> new SimpleObjectProperty<>(c.getValue().title()));
		columnTitle.setPrefWidth(530);
		columnTitle.setComparator(Utils::compareNatural);
		table.getColumns().add(columnTitle);
		table.setPlaceholder(new Label(translation.getSingle("tables.engines.placeholder")));
		table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		table.setContextMenu(newContextMenu(window, table));
		columnTitle.setSortType(SortType.ASCENDING);
		table.getSortOrder().add(columnTitle);
		return table;
	}
	
	@Override
	public final String getTitle(TableWindow window) {
		return window.getTranslation().getSingle("tables.engines.title");
	}
	
	@Override
	public final boolean filter(Program item, String text) {
		return Utils.normalize(item.title()).toLowerCase().contains(text);
	}
	
	/** @since 00.02.07 */
	public boolean canReload() {
		return true;
	}
	
	/** @since 00.02.07 */
	@Override
	public void beforeReload() {
		GlobalCache.ofPrograms().remove(engine.getClass());
	}
	
	/** @since 00.02.07 */
	@Override
	protected void onCancelled() throws Exception {
		beforeReload();
	}
}