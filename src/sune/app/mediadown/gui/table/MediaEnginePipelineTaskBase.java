package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.entity.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.task.ListTask;

/** @since 00.01.27 */
public abstract class MediaEnginePipelineTaskBase<A, B> extends TableWindowPipelineTaskBase<B> {
	
	protected final MediaEngine engine;
	protected final List<A> items;
	
	public MediaEnginePipelineTaskBase(TableWindow window, MediaEngine engine, List<A> items) {
		super(window);
		this.engine = engine;
		this.items = items;
	}
	
	protected abstract ListTask<B> getFunction(A item, MediaEngine engine);
	protected abstract PipelineResult getResult(TableWindow window, MediaEngine engine, List<B> result);
	
	@Override
	protected ListTask<B> getTask() {
		return ListTask.of((task) -> {
			for(A item : items) {
				ListTask<B> t = getFunction(item, engine);
				t.forwardAdd(task);
				t.startAndWait();
			}
		});
	}
	
	@Override
	protected PipelineResult getResult(TableWindow window, List<B> result) {
		return getResult(window, engine, result);
	}
}