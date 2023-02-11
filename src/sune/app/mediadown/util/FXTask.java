package sune.app.mediadown.util;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import sune.app.mediadown.concurrent.SyncObject;

public final class FXTask<T> implements Runnable {
	
	private static final Object NO_RESULT = new Object();
	
	private final Callable<T> callable;
	private final AtomicReference<Object> result = new AtomicReference<>(NO_RESULT);
	private final SyncObject lock = new SyncObject();
	private Exception exception;
	
	public FXTask(Runnable runnable) {
		this(runnable, null);
	}
	
	public FXTask(Runnable runnable, T result) {
		this(Utils.callable(Utils.checked(runnable), result));
	}
	
	public FXTask(Callable<T> callable) {
		this.callable = callable;
	}
	
	@Override
	public final void run() {
		result.set(NO_RESULT);
		FXUtils.thread(() -> {
			try {
				result.set(callable.call());
			} catch(Exception ex) {
				exception = ex;
			} finally {
				lock.unlock();
			}
		});
	}
	
	public final T get() throws Exception {
		if((result.get() == NO_RESULT))
			lock.await();
		if((exception != null))
			throw exception;
		@SuppressWarnings("unchecked")
		T theResult = (T) result.get();
		return theResult;
	}
	
	public final Exception getException() {
		return exception;
	}
}