package sune.app.mediadown.manager;

import java.util.Objects;

import sune.app.mediadown.util.Cancellable;
import sune.app.mediadown.util.QueueContext;
import sune.app.mediadown.util.QueueTaskExecutor.QueueTaskResult;

/** @since 00.01.26 */
public class ManagerSubmitResult<A, B> implements QueueTaskResult<B>, Cancellable {
	
	private final A value;
	/** @since 00.02.08 */
	private final QueueTaskResult<B> taskResult;
	/** @since 00.02.08 */
	private final QueueContext context;
	
	/** @since 00.02.08 */
	public ManagerSubmitResult(A value, QueueTaskResult<B> taskResult, QueueContext context) {
		this.value = Objects.requireNonNull(value);
		this.taskResult = Objects.requireNonNull(taskResult);
		this.context = Objects.requireNonNull(context);
	}
	
	@Override
	public void cancel() throws Exception {
		taskResult.cancel();
	}
	
	/** @since 00.02.08 */
	@Override
	public boolean awaitQueued() {
		return taskResult.awaitQueued();
	}
	
	@Override
	public B get() throws Exception {
		return taskResult.get();
	}
	
	/** @since 00.02.08 */
	@Override
	public Exception exception() {
		return taskResult.exception();
	}
	
	public A value() {
		return value;
	}
	
	/** @since 00.02.08 */
	public QueueTaskResult<B> taskResult() {
		return taskResult;
	}
	
	/** @since 00.02.08 */
	public QueueContext context() {
		return context;
	}
}