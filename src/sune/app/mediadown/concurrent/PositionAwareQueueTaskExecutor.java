package sune.app.mediadown.concurrent;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import sune.app.mediadown.util.Property;
import sune.app.mediadown.util.Utils;

/** @since 00.02.08 */
public class PositionAwareQueueTaskExecutor<V> extends QueueTaskExecutor<V> {
	
	/** @since 00.02.09 */
	protected final AtomicInteger nextPosition = new AtomicInteger();
	/** @since 00.02.09 */
	protected final AtomicInteger taskTotalCount = new AtomicInteger();
	
	public PositionAwareQueueTaskExecutor(int maxTaskCount) {
		super(maxTaskCount);
	}
	
	/** @since 00.02.09 */
	protected void notifySubmittedTasks(BiConsumer<PositionAwareInternalQueueTask, Integer> action, int position) {
		for(InternalQueueTask t : submittedTasks) {
			action.accept((PositionAwareInternalQueueTask) t, position);
		}
	}
	
	/** @since 00.02.09 */
	protected PositionAwareQueueTaskState taskNormalState() {
		int position = nextPosition.getAndIncrement();
		int lowerFence = taskTotalCount.get();
		return new PositionAwareQueueTaskState(position, lowerFence);
	}
	
	@Override
	protected InternalQueueTask createTask(QueueTask<V> task) {
		return new PositionAwareInternalQueueTask(task, taskNormalState());
	}
	
	@Override
	protected Future<V> submitTask(InternalQueueTask task) {
		PositionAwareInternalQueueTask castedTask = Utils.cast(task);
		
		if(!castedTask.isCancelled()) {
			int position = castedTask.position();
			
			notifySubmittedTasks(
				PositionAwareInternalQueueTask::taskSubmitted,
				position
			);
		}
		
		taskTotalCount.getAndIncrement();
		return super.submitTask(task);
	}
	
	@Override
	protected void cancelTask(InternalQueueTask task) {
		super.cancelTask(task);
		
		PositionAwareInternalQueueTask castedTask = Utils.cast(task);
		int position = castedTask.position();
		
		notifySubmittedTasks(
			PositionAwareInternalQueueTask::taskCancelled,
			position
		);
	}
	
	@Override
	protected void pauseSubmittedTask(InternalQueueTask task) {
		super.pauseSubmittedTask(task);
		
		PositionAwareInternalQueueTask castedTask = Utils.cast(task);
		int position = castedTask.position();
		
		notifySubmittedTasks(
			PositionAwareInternalQueueTask::taskPaused,
			position
		);
	}
	
	@Override
	protected void pauseDelayedTask(QueueTaskExecutor<V>.InternalQueueTask task) {
		PositionAwareInternalQueueTask castedTask = Utils.cast(task);
		int position = castedTask.position();
		
		// Treat the delayed task as resumed
		notifySubmittedTasks(
			PositionAwareInternalQueueTask::taskResumed,
			position
		);
	}
	
	@Override
	protected void resumeSubmittedTask(InternalQueueTask task) {
		super.resumeSubmittedTask(task);
		
		PositionAwareInternalQueueTask castedTask = Utils.cast(task);
		int position = castedTask.position();
		
		notifySubmittedTasks(
			PositionAwareInternalQueueTask::taskResumed,
			position
		);
	}
	
	@Override
	protected void resumeDelayedTask(InternalQueueTask task) {
		PositionAwareInternalQueueTask castedTask = Utils.cast(task);
		castedTask.setState(taskNormalState()); // Resubmit
		
		super.resumeDelayedTask(task);
	}
	
	@Override
	public PositionAwareQueueTaskResult<V> submit(QueueTask<V> task) {
		return Utils.cast(super.submit(task));
	}
	
	/** @since 00.02.09 */
	protected static class PositionAwareQueueTaskState {
		
		protected final int position;
		protected final AtomicInteger lowerFence;
		
		public PositionAwareQueueTaskState(int position, int lowerFence) {
			this.position = position;
			this.lowerFence = new AtomicInteger(lowerFence);
		}
		
		public int decreaseQueuePosition() {
			return position - lowerFence.incrementAndGet();
		}
		
		public int increaseQueuePosition() {
			return position - lowerFence.decrementAndGet();
		}
		
		public int position() {
			return position;
		}
		
		public int queuePosition() {
			return position - lowerFence.get();
		}
	}
	
	protected class PositionAwareInternalQueueTask extends InternalQueueTask
			implements PositionAwareQueueTaskResult<V> {
		
		/** @since 00.02.09 */
		protected volatile PositionAwareQueueTaskState state;
		/** @since 00.02.09 */
		protected final VarLoader<Property<Integer>> queuePositionProperty;
		
		public PositionAwareInternalQueueTask(QueueTask<V> task, PositionAwareQueueTaskState state) {
			super(task);
			this.state = Objects.requireNonNull(state);
			this.queuePositionProperty = VarLoader.of(this::createQueuePositionProperty);
		}
		
		/** @since 00.02.09 */
		protected final Property<Integer> createQueuePositionProperty() {
			return new Property<>(state.queuePosition());
		}
		
		/** @since 00.02.09 */
		protected void setState(PositionAwareQueueTaskState state) {
			this.state = Objects.requireNonNull(state);
			queuePosition(state.queuePosition());
		}
		
		/** @since 00.02.09 */
		protected void queuePosition(int queuePosition) {
			queuePositionProperty().set(queuePosition);
		}
		
		/** @since 00.02.09 */
		protected void maybeDecreaseQueuePosition(PositionAwareQueueTaskState state, int position) {
			if(position > state.position()) {
				return;
			}
			
			queuePosition(state.decreaseQueuePosition());
		}
		
		/** @since 00.02.09 */
		protected void maybeIncreaseQueuePosition(PositionAwareQueueTaskState state, int position) {
			if(position > state.position()) {
				return;
			}
			
			queuePosition(state.increaseQueuePosition());
		}
		
		protected void taskSubmitted(int position) {
			maybeDecreaseQueuePosition(state, position);
		}
		
		protected void taskCancelled(int position) {
			maybeDecreaseQueuePosition(state, position);
		}
		
		/** @since 00.02.09 */
		protected void taskPaused(int position) {
			maybeDecreaseQueuePosition(state, position);
		}
		
		/** @since 00.02.09 */
		protected void taskResumed(int position) {
			maybeIncreaseQueuePosition(state, position);
		}
		
		@Override
		public Property<Integer> queuePositionProperty() {
			return queuePositionProperty.value();
		}
		
		@Override
		public int position() {
			return state.position();
		}
		
		@Override
		public int queuePosition() {
			return state.queuePosition();
		}
	}
	
	public static interface PositionAwareQueueTaskResult<V> extends QueueTaskResult<V> {
		
		/** @since 00.02.09 */
		static final int UNDEFINED_POSITION = Integer.MIN_VALUE;
		
		Property<Integer> queuePositionProperty();
		int position();
		int queuePosition();
	}
}