package sune.app.mediadown.concurrent;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.util.Callback;
import sune.app.mediadown.util.CheckedBiConsumer;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.CheckedCallback;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.CheckedSupplier;
import sune.app.mediadown.util.Utils;

@Deprecated
public class WorkerUpdatableTask<P, R> {
	
	private final Worker worker;
	private final Callable<R> callable;
	private WorkerResult<R> result;
	
	private final WorkerProxy proxy;
	
	public WorkerUpdatableTask(P param, Callback<P, R> callback) {
		this(createWorker(), () -> callback.call(param));
	}
	
	public WorkerUpdatableTask(P param, CheckedCallback<P, R> callback) {
		this(createWorker(), () -> callback.call(param));
	}
	
	public WorkerUpdatableTask(Worker worker, P param, Callback<P, R> callback) {
		this(worker, () -> callback.call(param));
	}
	
	public WorkerUpdatableTask(Worker worker, P param, CheckedCallback<P, R> callback) {
		this(worker, () -> callback.call(param));
	}
	
	public WorkerUpdatableTask(P param, CheckedBiFunction<WorkerProxy, P, R> function) {
		this.worker   = createWorker();
		this.proxy    = proxy(this);
		this.callable = (() -> function.apply(proxy, param));
	}
	
	private WorkerUpdatableTask(Worker worker, Callable<R> callable) {
		this.worker   = worker;
		this.proxy    = proxy(this);
		this.callable = callable;
	}
	
	private final WorkerProxy proxy(WorkerUpdatableTask<P, R> task) {
		return new WorkerProxy() {
			
			private final WorkerUpdatableTask<P, R> theTask = task;
			
			@Override public void pause()  { theTask.pause();  }
			@Override public void resume() { theTask.resume(); }
			@Override public void cancel() { theTask.cancel(); }
			
			@Override public boolean isRunning()  { return theTask.isRunning();  }
			@Override public boolean isPaused()   { return theTask.isPaused();   }
			@Override public boolean isCanceled() { return theTask.isCanceled(); }
		};
	}
	
	private static final Worker createWorker() {
		return Worker.createWorker();
	}
	
	public static final <P, R> void runTaskAndThen(WorkerUpdatableTask<P, R> task, Consumer<WorkerResult<R>> then) {
		if(task == null || then == null) {
			throw new IllegalArgumentException("Task and Then action cannot be null");
		}
		
		Threads.execute(() -> {
			WorkerResult<R> result = task.startAndWait();
			then.accept(result);
		});
	}
	
	public static final <P, R> WorkerUpdatableTask<P, R> noActionTask() {
		return new WorkerUpdatableTask<>(null, (Callback<P, R>) (p) -> null);
	}
	
	public static final <P, R> WorkerUpdatableTask<P, R> constantValueTask(R value) {
		return new WorkerUpdatableTask<>(null, (Callback<P, R>) (p) -> value);
	}
	
	public static final <P, R> WorkerUpdatableTask<P, R> constantValueTask(Supplier<R> supplier) {
		return new WorkerUpdatableTask<>(null, (Callback<P, R>) (p) -> supplier.get());
	}
	
	/** @since 00.02.05 */
	public static final <P, R> WorkerUpdatableTask<P, R> constantValueTaskChecked(CheckedSupplier<R> supplier) {
		return new WorkerUpdatableTask<>(null, (CheckedCallback<P, R>) (p) -> supplier.get());
	}
	
	public static final <P> WorkerUpdatableTask<Consumer<P>, P[]> arrayTask(Consumer<P> consumer, Supplier<P[]> supplier) {
		return new WorkerUpdatableTask<>(consumer, (Callback<Consumer<P>, P[]>) (c) -> {
			P[] array = supplier.get();
			
			if(array == null) {
				return null;
			}
			
			Stream.of(array).forEach(c);
			return array;
		});
	}
	
	public static final <P> WorkerUpdatableTask<BiFunction<WorkerProxy, P, Boolean>, P[]> arrayTask
			(BiFunction<WorkerProxy, P, Boolean> function,
			 Supplier<P[]> supplier) {
		return new WorkerUpdatableTask<>(function, (w, c) -> {
			P[] array = supplier.get();
			
			if(array == null) {
				return null;
			}
			
			for(P item : array) {
				if(!w.isRunning() || !c.apply(w, item)) {
					break;
				}
			}
			
			return array;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<CheckedConsumer<P>, P[]> arrayTaskChecked
			(CheckedConsumer<P> consumer, CheckedSupplier<P[]> supplier) {
		return new WorkerUpdatableTask<>(consumer, (CheckedCallback<CheckedConsumer<P>, P[]>) (c) -> {
			P[] array = supplier.get();
			
			if(array == null) {
				return null;
			}
			
			for(P item : array) {
				c.accept(item);
			}
			
			return array;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, P, Boolean>, P[]> arrayTaskChecked
			(CheckedBiFunction<WorkerProxy, P, Boolean> function, CheckedSupplier<P[]> supplier) {
		return new WorkerUpdatableTask<>(function, (w, c) -> {
			P[] array = supplier.get();
			
			if(array == null) {
				return null;
			}
			
			for(P item : array) {
				if(!w.isRunning() || !c.apply(w, item)) {
					break;
				}
			}
			
			return array;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<Consumer<P>, List<P>> listTask(Consumer<P> consumer, Supplier<List<P>> supplier) {
		return new WorkerUpdatableTask<>(consumer, (Callback<Consumer<P>, List<P>>) (c) -> {
			List<P> list = supplier.get();
			
			if(list == null) {
				return null;
			}
			
			list.forEach(c);
			return list;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<BiFunction<WorkerProxy, P, Boolean>, List<P>> listTask
			(BiFunction<WorkerProxy, P, Boolean> function,
			 Supplier<List<P>> supplier) {
		return new WorkerUpdatableTask<>(function, (w, c) -> {
			List<P> list = supplier.get();
			
			if(list == null) {
				return null;
			}
			
			for(P item : list) {
				if(!w.isRunning() || !c.apply(w, item)) {
					break;
				}
			}
			
			return list;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<CheckedConsumer<P>, List<P>> listTaskChecked
			(CheckedConsumer<P> consumer, CheckedSupplier<List<P>> supplier) {
		return new WorkerUpdatableTask<>(consumer, (CheckedCallback<CheckedConsumer<P>, List<P>>) (c) -> {
			List<P> list = supplier.get();
			
			if(list == null) {
				return null;
			}
			
			for(P item : list) {
				c.accept(item);
			}
			
			return list;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, P, Boolean>, List<P>> listTaskChecked
			(CheckedBiFunction<WorkerProxy, P, Boolean> function, CheckedSupplier<List<P>> supplier) {
		return new WorkerUpdatableTask<>(function, (w, c) -> {
			List<P> list = supplier.get();
			
			if(list == null) {
				return null;
			}
			
			for(P item : list) {
				if(!w.isRunning() || !c.apply(w, item)) {
					break;
				}
			}
			
			return list;
		});
	}
	
	public static final <P> WorkerUpdatableTask<Consumer<P>, Void> arrayVoidTask(Consumer<P> consumer, Supplier<P[]> supplier) {
		return new WorkerUpdatableTask<>(consumer, (Callback<Consumer<P>, Void>) (c) -> {
			P[] array = supplier.get();
			
			if(array == null) {
				return null;
			}
			
			Stream.of(array).forEach(c);
			return null;
		});
	}
	
	public static final <P> WorkerUpdatableTask<BiFunction<WorkerProxy, P, Boolean>, Void> arrayVoidTask
			(BiFunction<WorkerProxy, P, Boolean> function,
			 Supplier<P[]> supplier) {
		return new WorkerUpdatableTask<>(function, (w, c) -> {
			P[] array = supplier.get();
			
			if(array == null) {
				return null;
			}
			
			for(P item : array) {
				if(!w.isRunning() || !c.apply(w, item)) {
					break;
				}
			}
			
			return null;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<CheckedConsumer<P>, Void> arrayVoidTaskChecked
			(CheckedConsumer<P> consumer, CheckedSupplier<P[]> supplier) {
		return new WorkerUpdatableTask<>(consumer, (CheckedCallback<CheckedConsumer<P>, Void>) (c) -> {
			P[] array = supplier.get();
			
			if(array == null) {
				return null;
			}
			
			for(P item : array) {
				c.accept(item);
			}
			
			return null;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, P, Boolean>, Void> arrayVoidTaskChecked
			(CheckedBiFunction<WorkerProxy, P, Boolean> function, CheckedSupplier<P[]> supplier) {
		return new WorkerUpdatableTask<>(function, (w, c) -> {
			P[] array = supplier.get();
			
			if(array == null) {
				return null;
			}
			
			for(P item : array) {
				if(!w.isRunning() || !c.apply(w, item)) {
					break;
				}
			}
			
			return null;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<Consumer<P>, Void> listVoidTask(Consumer<P> consumer, Supplier<List<P>> supplier) {
		return new WorkerUpdatableTask<>(consumer, (Callback<Consumer<P>, Void>) (c) -> {
			List<P> list = supplier.get();
			
			if(list == null) {
				return null;
			}
			
			list.forEach(c);
			return null;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<BiFunction<WorkerProxy, P, Boolean>, Void> listVoidTask
			(BiFunction<WorkerProxy, P, Boolean> function,
			 Supplier<List<P>> supplier) {
		return new WorkerUpdatableTask<>(function, (w, c) -> {
			List<P> list = supplier.get();
			
			if(list == null) {
				return null;
			}
			
			for(P item : list) {
				if(!w.isRunning() || !c.apply(w, item)) {
					break;
				}
			}
			
			return null;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<CheckedConsumer<P>, Void> listVoidTaskChecked
			(CheckedConsumer<P> consumer, CheckedSupplier<List<P>> supplier) {
		return new WorkerUpdatableTask<>(consumer, (CheckedCallback<CheckedConsumer<P>, Void>) (c) -> {
			List<P> list = supplier.get();
			
			if(list == null) {
				return null;
			}
			
			for(P item : list) {
				c.accept(item);
			}
			
			return null;
		});
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, P, Boolean>, Void> listVoidTaskChecked
			(CheckedBiFunction<WorkerProxy, P, Boolean> function, CheckedSupplier<List<P>> supplier) {
		return new WorkerUpdatableTask<>(function, (w, c) -> {
			List<P> list = supplier.get();
			
			if(list == null) {
				return null;
			}
			
			for(P item : list) {
				if(!w.isRunning() || !c.apply(w, item)) {
					break;
				}
			}
			
			return null;
		});
	}
	
	public static final <P> WorkerUpdatableTask<P, Void> voidTask(P param, Consumer<P> consumer) {
		return new WorkerUpdatableTask<P, Void>(param, (Callback<P, Void>) (p) -> { consumer.accept(p); return null; });
	}
	
	/** @since 00.01.27 */
	public static final <P> WorkerUpdatableTask<P, Void> voidTask(P param, BiConsumer<WorkerProxy, P> function) {
		return new WorkerUpdatableTask<P, Void>(param, (w, p) -> { function.accept(w, p); return null; });
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<P, Void> voidTaskChecked(P param, CheckedConsumer<P> consumer) {
		return new WorkerUpdatableTask<P, Void>(param, (CheckedCallback<P, Void>) (p) -> { consumer.accept(p); return null; });
	}
	
	/** @since 00.02.05 */
	public static final <P> WorkerUpdatableTask<P, Void> voidTaskChecked(P param, CheckedBiConsumer<WorkerProxy, P> function) {
		return new WorkerUpdatableTask<P, Void>(param, (w, p) -> { function.accept(w, p); return null; });
	}
	
	private final void assignResult() {
		if(!worker.hasNextResult()) {
			return;
		}
		
		result = Utils.cast(worker.nextResult());
	}
	
	private final WorkerResult<R> checkResult(Supplier<WorkerResult<R>> supplier) throws Exception {
		WorkerResult<R> result = supplier.get();
		
		if(result != null) {
			Exception exception = result.getException();
			
			if(exception != null) {
				throw exception;
			}
		}
		
		return result;
	}
	
	public void start() {
		worker.submit(callable);
	}
	
	public WorkerResult<R> startAndWait() {
		start();
		worker.waitTillDone();
		return getResult();
	}
	
	public WorkerResult<R> startAndWaitChecked() throws Exception {
		return checkResult(this::startAndWait);
	}
	
	public void pause() {
		worker.pause();
	}
	
	public void resume() {
		worker.resume();
	}
	
	public void cancel() {
		worker.stop();
	}
	
	public WorkerResult<R> getResult() {
		assignResult();
		return result;
	}
	
	public WorkerResult<R> getResultChecked() throws Exception {
		return checkResult(this::getResult);
	}
	
	public boolean isRunning() {
		return worker.isRunning();
	}
	
	public boolean isPaused() {
		return worker.isPaused();
	}
	
	public boolean isCanceled() {
		return worker.isStopped();
	}
}