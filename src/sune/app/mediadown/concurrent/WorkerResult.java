package sune.app.mediadown.concurrent;

public class WorkerResult<T> {
	
	private final T result;
	private final Exception exception;
	
	public WorkerResult(T result, Exception exception) {
		this.result    = result;
		this.exception = exception;
	}
	
	public T getResultOrThrowException() throws Exception {
		if((exception != null))
			throw exception;
		return result;
	}
	
	public T getResult() {
		return result;
	}
	
	public Exception getException() {
		return exception;
	}
}