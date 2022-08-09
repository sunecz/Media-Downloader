package sune.app.mediadown.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class WorkerTask<T> {
	
	private final Worker worker;
	private final Callable<T> callable;
	private WorkerResult<T> result;
	
	public WorkerTask(Runnable runnable) {
		this(createWorker(), Utils.<Callable<T>>cast(Utils.callable(Utils.checked(runnable))));
	}
	
	public WorkerTask(Runnable runnable, T result) {
		this(createWorker(), Utils.callable(Utils.checked(runnable), result));
	}
	
	public WorkerTask(Callable<T> callable) {
		this(createWorker(), callable);
	}
	
	public WorkerTask(Worker worker, Runnable runnable) {
		this(worker, Utils.<Callable<T>>cast(Utils.callable(Utils.checked(runnable))));
	}
	
	public WorkerTask(Worker worker, Runnable runnable, T result) {
		this(worker, Utils.callable(Utils.checked(runnable), result));
	}
	
	public WorkerTask(Worker worker, Callable<T> callable) {
		this.worker   = worker;
		this.callable = callable;
	}
	
	private static final Worker createWorker() {
		return Worker.createWorker();
	}
	
	public static final <R> void runTask(WorkerTask<R> task) {
		if((task == null))
			throw new IllegalArgumentException("Task action cannot be null");
		Threads.execute(() -> {
			task.startAndWait();
			task.cancel(); // interrupts the Worker
		});
	}
	
	public static final <R> void runTaskAndThen(WorkerTask<R> task, Consumer<WorkerResult<R>> then) {
		if((task == null || then == null))
			throw new IllegalArgumentException("Task and Then action cannot be null");
		Threads.execute(() -> {
			WorkerResult<R> result = task.startAndWait();
			then.accept(result);
			task.cancel(); // interrupts the Worker
		});
	}
	
	public void start() {
		worker.submit(callable);
	}
	
	public WorkerResult<T> startAndWait() {
		start();
		worker.waitTillDone();
		return getResult();
	}
	
	public void pause() {
		worker.pause();
	}
	
	public void resume() {
		worker.resume();
	}
	
	public void cancel() {
		worker.interrupt();
	}
	
	@SuppressWarnings("unchecked")
	private final void assignResult() {
		if(!worker.hasNextResult())
			return;
		result = (WorkerResult<T>) worker.nextResult();
	}
	
	public WorkerResult<T> getResult() {
		assignResult();
		return result;
	}
	
	public boolean isRunning() {
		return worker.isRunning();
	}
	
	public boolean isPaused() {
		return worker.isPaused();
	}
	
	public boolean isCanceled() {
		return worker.isInterrupted();
	}
}