package sune.app.mediadown.task;

import java.util.List;
import java.util.function.Supplier;

import sune.app.mediadown.resource.cache.Cache;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.CheckedFunction;
import sune.app.mediadown.util.CheckedSupplier;

/** @since 00.02.08 */
public final class Tasks {
	
	// Forbid anyone to create an instance of this class
	private Tasks() {
	}
	
	public static final <T> ListTask<T> list(CheckedConsumer<ListTask<T>> runnable) {
		return ListTask.of(runnable);
	}
	
	public static final <T> ListTask<T> listOfOne(CheckedSupplier<T> supplier) {
		return list((task) -> task.add(supplier.get()));
	}
	
	public static final <T> ListTask<T> listOfMany(CheckedSupplier<List<T>> supplier) {
		return list((task) -> task.addAll(supplier.get()));
	}
	
	public static final <T, K> ListTask<T> cachedList(Supplier<Cache> cacheSupplier, K key,
			CheckedFunction<K, ListTask<T>> creator) {
		return list((task) -> {
			Cache cache = cacheSupplier.get();
			
			if(cache.has(key)) {
				List<T> l = cache.getChecked(key);
				task.addAll(l);
			} else {
				cache.setChecked(key, () -> {
					ListTask<T> t = creator.apply(key);
					t.forwardAdd(task);
					t.startAndWait();
					return t.list();
				});
			}
		});
	}
}