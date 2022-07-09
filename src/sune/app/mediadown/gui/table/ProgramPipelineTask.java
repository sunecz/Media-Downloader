package sune.app.mediadown.gui.table;

import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import sune.app.mediadown.Episode;
import sune.app.mediadown.Program;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.language.Translation;
import sune.app.mediadown.resource.GlobalCache;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.WorkerProxy;
import sune.app.mediadown.util.WorkerUpdatableTask;
import sune.app.mediadown.util.WorkerUpdatableTaskUtils;

/** @since 00.01.27 */
public final class ProgramPipelineTask extends MediaEnginePipelineTaskBase<Program, Episode, ProgramPipelineResult> {
	
	public ProgramPipelineTask(TableWindow window, MediaEngine engine, List<Program> items) {
		super(window, engine, items);
	}
	
	@Override
	protected final CheckedBiFunction<Program, CheckedBiFunction<WorkerProxy, Episode, Boolean>,
			WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Episode, Boolean>, Void>> getFunction(MediaEngine engine) {
		return ((program, f) -> WorkerUpdatableTaskUtils.cachedListBiTask(GlobalCache.ofEpisodes(), program,
		                                                                  f, engine::getEpisodes, program));
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
		table.getColumns().add(columnTitle);
		table.setPlaceholder(new Label(translation.getSingle("tables.episodes.placeholder")));
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
}