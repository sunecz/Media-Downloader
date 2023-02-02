package sune.app.mediadown.util;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/** @since 00.02.08 */
public class PositionAwareQueueTaskExecutor<V> extends QueueTaskExecutor<V> {
	
	protected final AtomicInteger nextPosition = new AtomicInteger();
	
	public PositionAwareQueueTaskExecutor(int maxTaskCount) {
		super(maxTaskCount);
	}
	
	@Override
	protected InternalQueueTask createTask(QueueTask<V> task) {
		int position = nextPosition.getAndIncrement();
		int queuePosition = position - queuedTasks.size();
		
		return new PositionAwareInternalQueueTask(task, position, queuePosition);
	}
	
	@Override
	protected Future<V> submitTask(InternalQueueTask task) {
		PositionAwareInternalQueueTask castedTask = Utils.cast(task);
		
		if(castedTask.isCancelled()) {
			return null;
		}
		
		int position = castedTask.position();
		
		for(InternalQueueTask t : submittedTasks) {
			Utils.<PositionAwareInternalQueueTask>cast(t).taskSubmitted(position);
		}
		
		return super.submitTask(task);
	}
	
	@Override
	protected void cancelTask(InternalQueueTask task) {
		PositionAwareInternalQueueTask castedTask = Utils.cast(task);
		
		if(castedTask.isQueued()) {
			return;
		}
		
		int position = castedTask.position();
		
		for(InternalQueueTask t : submittedTasks) {
			Utils.<PositionAwareInternalQueueTask>cast(t).taskCancelled(position);
		}
	}
	
	@Override
	public PositionAwareQueueTaskResult<V> submit(QueueTask<V> task) {
		return Utils.cast(super.submit(task));
	}
	
	protected class PositionAwareInternalQueueTask extends InternalQueueTask
			implements PositionAwareQueueTaskResult<V> {
		
		private final AtomicInteger lowerSubmittedTaskCount;
		private final int position;
		private final IntegerProperty queuePosition;
		
		public PositionAwareInternalQueueTask(QueueTask<V> task, int position, int queuePosition) {
			super(task);
			this.position = position;
			this.queuePosition = new SimpleIntegerProperty(queuePosition);
			this.lowerSubmittedTaskCount = new AtomicInteger(position - queuePosition);
		}
		
		protected final void taskSubmitted(int position) {
			if(position > this.position) {
				return;
			}
			
			int queuePosition = this.position - lowerSubmittedTaskCount.incrementAndGet();
			queuePositionProperty().set(queuePosition);
		}
		
		protected final void taskCancelled(int position) {
			taskSubmitted(position);
		}
		
		@Override
		public IntegerProperty queuePositionProperty() {
			return queuePosition;
		}
		
		@Override
		public int position() {
			return position;
		}
		
		@Override
		public int queuePosition() {
			return queuePositionProperty().get();
		}
	}
	
	public static interface PositionAwareQueueTaskResult<V> extends QueueTaskResult<V> {
		
		IntegerProperty queuePositionProperty();
		
		int position();
		int queuePosition();
	}
}