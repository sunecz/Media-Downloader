package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.concurrent.ListTask;
import sune.app.mediadown.concurrent.ListTask.ListTaskEvent;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.01.27 */
public abstract class MediaEnginePipelineTaskBase<A, B, R extends PipelineResult<?>>
		extends TableWindowPipelineTaskBase<B, R> {
	
	protected final MediaEngine engine;
	protected final List<A> items;
	
	public MediaEnginePipelineTaskBase(TableWindow window, MediaEngine engine, List<A> items) {
		super(window);
		this.engine = engine;
		this.items = items;
	}
	
	protected abstract ListTask<B> getFunction(A item, MediaEngine engine);
	protected abstract R getResult(TableWindow window, MediaEngine engine, List<B> result);
	
	@Override
	protected ListTask<B> getTask() {
		return ListTask.of((task) -> {
			for(A item : items) {
				ListTask<B> t = getFunction(item, engine);
				t.addEventListener(ListTaskEvent.ADD, (p) -> Ignore.callVoid(() -> task.add(Utils.cast(p.b))));
				t.startAndWait();
			}
		});
	}
	
	@Override
	protected R getResult(TableWindow window, List<B> result) {
		return getResult(window, engine, result);
	}
}