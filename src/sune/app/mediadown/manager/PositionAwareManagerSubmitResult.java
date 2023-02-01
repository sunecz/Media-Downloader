package sune.app.mediadown.manager;

import sune.app.mediadown.util.PositionAwareQueueTaskExecutor.PositionAwareQueueTaskResult;
import sune.app.mediadown.util.QueueContext;
import sune.app.mediadown.util.Utils;

/** @since 00.02.08 */
public class PositionAwareManagerSubmitResult<A, B> extends ManagerSubmitResult<A, B> {
	
	public PositionAwareManagerSubmitResult(A value, PositionAwareQueueTaskResult<B> taskResult, QueueContext context) {
		super(value, taskResult, context);
	}
	
	public PositionAwareQueueTaskResult<B> taskResult() {
		return Utils.cast(super.taskResult());
	}
}