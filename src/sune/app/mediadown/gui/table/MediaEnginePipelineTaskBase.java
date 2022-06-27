package sune.app.mediadown.gui.table;

import java.util.List;

import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.gui.window.TableWindow;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.CheckedFunction;
import sune.app.mediadown.util.WorkerProxy;
import sune.app.mediadown.util.WorkerUpdatableTask;

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
	
	protected abstract CheckedBiFunction<A, CheckedBiFunction<WorkerProxy, B, Boolean>,
		WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, B, Boolean>, Void>> getFunction(MediaEngine engine);
	protected abstract R getResult(TableWindow window, MediaEngine engine, List<B> result);
	
	@Override
	protected CheckedFunction<CheckedBiFunction<WorkerProxy, B, Boolean>,
			WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, B, Boolean>, Void>> getTask() {
		return ((function) -> WorkerUpdatableTask.voidTaskChecked(null, (proxy, value) -> {
			for(A item : items) {
				if(!running.get() || proxy.isCanceled())
					break;
				getFunction(engine).apply(item, function).startAndWaitChecked();
			}
		}));
	}
	
	@Override
	protected R getResult(TableWindow window, List<B> result) {
		return getResult(window, engine, result);
	}
}