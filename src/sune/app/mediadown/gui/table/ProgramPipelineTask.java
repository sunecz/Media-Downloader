package sune.app.mediadown.gui.table;

import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import sune.app.mediadown.entity.Episode;
import sune.app.mediadown.entity.MediaEngine;
import sune.app.mediadown.entity.Program;
import sune.app.mediadown.gui.GUI;
import sune.app.mediadown.gui.window.ReportWindow;
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
public final class ProgramPipelineTask extends MediaEnginePipelineTaskBase<Program, Episode, ProgramPipelineResult> {
	
	public ProgramPipelineTask(TableWindow window, MediaEngine engine, List<Program> items) {
		super(window, engine, items);
	}
	
	/** @since 00.02.09 */
	private final ContextMenu newContextMenu(TableWindow window, TableView<Episode> table) {
		Translation translation = window.getTranslation();
		ContextMenu menu = new ContextMenu();
		
		MenuItem itemCopyURL = new MenuItem(translation.getSingle("tables.episodes.context_menu.copy_url"));
		itemCopyURL.setOnAction((e) -> {
			ClipboardUtils.copy(
				Utils.toString(table.getSelectionModel().getSelectedItems(), (m) -> m.uri().normalize().toString())
			);
		});
		
		MenuItem itemReportBroken = new MenuItem(translation.getSingle("tables.episodes.context_menu.report_broken"));
		itemReportBroken.setOnAction((e) -> {
			Episode item = table.getSelectionModel().getSelectedItem();
			GUI.showReportWindow(window, Report.Builders.ofEpisode(
				item, Reason.BROKEN,
				ReportContext.ofProgram(items.get(0)) // Only one program is always selected
			), ReportWindow.Feature.onlyReasons(Reason.BROKEN));
		});
		
		menu.getItems().addAll(itemCopyURL, itemReportBroken);
		menu.showingProperty().addListener((o, ov, isShowing) -> {
			if(!isShowing) return;
			int countItems = table.getSelectionModel().getSelectedItems().size();
			boolean noSelectedItems = countItems == 0;
			boolean notOneSelectedItem = countItems != 1;
			itemCopyURL.setDisable(noSelectedItems);
			itemReportBroken.setDisable(notOneSelectedItem);
		});
		
		return menu;
	}
	
	@Override
	protected final ListTask<Episode> getFunction(Program item, MediaEngine engine) {
		return Tasks.cachedList(GlobalCache::ofEpisodes, item, engine::getEpisodes);
	}
	
	@Override
	protected final ProgramPipelineResult getResult(TableWindow window, MediaEngine engine, List<Episode> result) {
		return new ProgramPipelineResult(window, engine, result);
	}
	
	@Override
	protected String getProgressText(TableWindow window) {
		return window.getTranslation().getSingle("progress.episodes");
	}
	
	@Override
	public final TableView<Episode> getTable(TableWindow window) {
		TableView<Episode> table = new TableView<>();
		Translation translation = window.getTranslation();
		String titleTitle = translation.getSingle("tables.episodes.columns.title");
		TableColumn<Episode, String> columnTitle = new TableColumn<>(titleTitle);
		columnTitle.setCellValueFactory((c) -> new SimpleObjectProperty<>(c.getValue().title()));
		columnTitle.setPrefWidth(530);
		columnTitle.setComparator(Utils::compareNatural);
		table.getColumns().add(columnTitle);
		table.setPlaceholder(new Label(translation.getSingle("tables.episodes.placeholder")));
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.setContextMenu(newContextMenu(window, table));
		return table;
	}
	
	@Override
	public final String getTitle(TableWindow window) {
		return window.getTranslation().getSingle("tables.episodes.title");
	}
	
	@Override
	public final boolean filter(Episode item, String text) {
		return Utils.normalize(item.title()).toLowerCase().contains(text);
	}
	
	/** @since 00.02.07 */
	public boolean canReload() {
		return true;
	}
	
	/** @since 00.02.07 */
	@Override
	public void beforeReload() {
		items.forEach(GlobalCache.ofEpisodes()::remove);
	}
	
	/** @since 00.02.07 */
	@Override
	protected void onCancelled() throws Exception {
		beforeReload();
	}
}