package sune.app.mediadown.concurrent;

import java.util.ArrayList;
import java.util.List;

import sune.app.mediadown.resource.cache.Cache;
import sune.app.mediadown.util.CheckedBiFunction;
import sune.app.mediadown.util.CheckedFunction;

/** @since 00.02.07 */
@Deprecated
public final class WorkerUpdatableTaskUtils {
	
	// Forbid anyone to create an instance of this class
	private WorkerUpdatableTaskUtils() {
	}
	
	public static final <T> List<T> collect(CheckedBiFunction<WorkerProxy, T, Boolean> function,
			CheckedFunction<CheckedBiFunction<WorkerProxy, T, Boolean>,
			                WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, T, Boolean>, Void>> supplier)
			throws Exception {
		List<T> collector = new ArrayList<>();
		CheckedBiFunction<WorkerProxy, T, Boolean> f = ((p, v) -> collector.add(v) && function.apply(p, v));
		supplier.apply(f).startAndWaitChecked();
		return collector;
	}
	
	public static final <A, T> List<T> collectBi(CheckedBiFunction<WorkerProxy, T, Boolean> function,
			CheckedBiFunction<A, CheckedBiFunction<WorkerProxy, T, Boolean>,
			                  WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, T, Boolean>, Void>> supplier,
			A arg) throws Exception {
		List<T> collector = new ArrayList<>();
		CheckedBiFunction<WorkerProxy, T, Boolean> f = ((p, v) -> collector.add(v) && function.apply(p, v));
		supplier.apply(arg, f).startAndWaitChecked();
		return collector;
	}
	
	public static final <K, R> WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, R, Boolean>, Void>
		cachedListTask(Cache cache, K key, CheckedBiFunction<WorkerProxy, R, Boolean> function,
			CheckedFunction<CheckedBiFunction<WorkerProxy, R, Boolean>,
			                WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, R, Boolean>, Void>> supplier) {
		return cache.has(key)
					? WorkerUpdatableTask.listVoidTaskChecked(function, () -> cache.getChecked(key))
					: WorkerUpdatableTask.    voidTaskChecked(function,
					  	(p, c) -> cache.setChecked(key, () -> collect(function, supplier)));
	}
	
	public static final <K, A, R> WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, R, Boolean>, Void>
		cachedListBiTask(Cache cache, K key, CheckedBiFunction<WorkerProxy, R, Boolean> function,
			CheckedBiFunction<A, CheckedBiFunction<WorkerProxy, R, Boolean>,
			                  WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, R, Boolean>, Void>> supplier,
			A arg) {
		return cache.has(key)
					? WorkerUpdatableTask.listVoidTaskChecked(function, () -> cache.getChecked(key))
					: WorkerUpdatableTask.    voidTaskChecked(function,
					  	(p, c) -> cache.setChecked(key, () -> collectBi(function, supplier, arg)));
	}
}