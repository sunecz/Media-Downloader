package sune.app.mediadown.gui.table;

import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import sune.app.mediadown.Program;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.resource.cache.GlobalCache;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.CheckedFunction;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.WorkerProxy;
import sune.app.mediadown.util.WorkerUpdatableTask;
import sune.app.mediadown.util.WorkerUpdatableTaskUtils;

/** @since 00.01.27 */
public final class MediaEnginePipelineTask extends TableWindowPipelineTaskBase<Program, MediaEnginePipelineResult> {
	
	private final MediaEngine engine;
	
	public MediaEnginePipelineTask(TableWindow window, MediaEngine engine) {
		super(window);
		this.engine = engine;
	}
	
	@Override
	protected final CheckedFunction<CheckedBiFunction<WorkerProxy, Program, Boolean>,
			WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Program, Boolean>, Void>> getTask() {
		return ((f) -> WorkerUpdatableTaskUtils.cachedListTask(GlobalCache.ofPrograms(), engine.getClass(),
		                                                       f, engine::getPrograms));
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
		table.getColumns().add(columnTitle);
		table.setPlaceholder(new Label(translation.getSingle("tables.engines.placeholder")));
		table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
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