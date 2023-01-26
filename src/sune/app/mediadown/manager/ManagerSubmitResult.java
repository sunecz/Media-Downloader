package sune.app.mediadown.manager;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import sune.app.mediadown.util.Cancellable;

/** @since 00.01.26 */
public class ManagerSubmitResult<A, B> implements Future<B>, Cancellable {
	
	private final A value;
	private final Future<B> future;
	
	public ManagerSubmitResult(A value, Future<B> future) {
		this.value = Objects.requireNonNull(value);
		this.future = Objects.requireNonNull(future);
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}
	
	@Override
	public void cancel() throws Exception {
		cancel(true);
	}
	
	@Override
	public boolean isCancelled() {
		return future.isCancelled();
	}
	
	@Override
	public boolean isDone() {
		return future.isDone();
	}
	
	@Override
	public B get() throws ExecutionException, InterruptedException {
		return future.get();
	}
	
	@Override
	public B get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException, InterruptedException {
		return future.get(timeout, unit);
	}
	
	public A value() {
		return value;
	}
}