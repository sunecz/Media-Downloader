package sune.app.mediadown.concurrent;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import sune.app.mediadown.util.Utils;

/** @since 00.02.08 */
public class PositionAwareQueueTaskExecutor<V> extends QueueTaskExecutor<V> {
	
	/** @since 00.02.09 */
	protected final AtomicInteger nextNormalPosition = new AtomicInteger();
	/** @since 00.02.09 */
	protected final AtomicInteger nextPausePosition = new AtomicInteger();
	
	/** @since 00.02.09 */
	protected final AtomicInteger taskTotalCount = new AtomicInteger();
	/** @since 00.02.09 */
	protected final AtomicInteger taskPausedCount = new AtomicInteger();
	
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
		int position = nextNormalPosition.getAndIncrement();
		int lowerFence = taskTotalCount.get();
		return new PositionAwareQueueTaskState(position, lowerFence);
	}
	
	/** @since 00.02.09 */
	protected PositionAwareQueueTaskState taskPauseState() {
		int position = nextPausePosition.getAndIncrement();
		int lowerFence = taskPausedCount.get();
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
	protected void pauseTask(InternalQueueTask task) {
		super.pauseTask(task);
		taskPausedCount.getAndIncrement();
		
		PositionAwareInternalQueueTask castedTask = Utils.cast(task);
		int position = castedTask.position();
		
		notifySubmittedTasks(
			PositionAwareInternalQueueTask::taskPaused,
			position
		);
	}
	
	@Override
	protected void resumeTask(InternalQueueTask task) {
		super.resumeTask(task);
		
		PositionAwareInternalQueueTask castedTask = Utils.cast(task);
		int position = castedTask.position();
		
		notifySubmittedTasks(
			PositionAwareInternalQueueTask::taskResumed,
			position
		);
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
		
		public int position() {
			return position;
		}
		
		public int queuePosition() {
			return position - lowerFence.get();
		}
	}
	
	protected class PositionAwareInternalQueueTask extends InternalQueueTask
			implements PositionAwareQueueTaskResult<V> {
		
		protected final PositionAwareQueueTaskState normalState;
		/** @since 00.02.09 */
		protected final VarLoader<IntegerProperty> queuePositionProperty;
		/** @since 00.02.09 */
		protected final VarLoader<PositionAwareQueueTaskState> pauseState;
		/** @since 00.02.09 */
		protected final VarLoader<IntegerProperty> pauseQueuePositionProperty;
		
		public PositionAwareInternalQueueTask(QueueTask<V> task, PositionAwareQueueTaskState state) {
			super(task);
			this.normalState = Objects.requireNonNull(state);
			this.queuePositionProperty = VarLoader.of(this::createQueuePositionProperty);
			this.pauseState = VarLoader.of(this::createPauseState);
			this.pauseQueuePositionProperty = VarLoader.of(this::createPauseQueuePositionProperty);
		}
		
		/** @since 00.02.09 */
		protected final IntegerProperty createQueuePositionProperty() {
			return new SimpleIntegerProperty(normalState.queuePosition());
		}
		
		/** @since 00.02.09 */
		protected final IntegerProperty createPauseQueuePositionProperty() {
			return new SimpleIntegerProperty(
				pauseState.isSet()
					? pauseState.value().queuePosition()
					: UNDEFINED_POSITION
			);
		}
		
		/** @since 00.02.09 */
		protected final PositionAwareQueueTaskState createPauseState() {
			PositionAwareQueueTaskState state = taskPauseState();
			
			if(pauseQueuePositionProperty.isSet()) {
				IntegerProperty prop = pauseQueuePositionProperty.value();
				prop.set(state.queuePosition());
			}
			
			return state;
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
		
		protected void taskSubmitted(int position) {
			maybeDecreaseQueuePosition(normalState, position);
		}
		
		protected void taskCancelled(int position) {
			maybeDecreaseQueuePosition(normalState, position);
		}
		
		/** @since 00.02.09 */
		protected void taskPaused(int position) {
			pauseState.value(); // Create the pause state
		}
		
		/** @since 00.02.09 */
		protected void taskResumed(int position) {
			if(!pauseState.isSet()) {
				return; // Has not yet been paused
			}
			
			maybeDecreaseQueuePosition(pauseState.value(), position);
		}
		
		@Override
		public IntegerProperty queuePositionProperty() {
			return queuePositionProperty.value();
		}
		
		@Override
		public int position() {
			return normalState.position();
		}
		
		@Override
		public int queuePosition() {
			return normalState.queuePosition();
		}
		
		@Override
		public IntegerProperty pauseQueuePositionProperty() {
			return pauseQueuePositionProperty.value();
		}
		
		@Override
		public int pausePosition() {
			return pauseState.isSet() ? pauseState.value().position() : UNDEFINED_POSITION;
		}
		
		@Override
		public int pauseQueuePosition() {
			return pauseState.isSet() ? pauseState.value().queuePosition() : UNDEFINED_POSITION;
		}
	}
	
	public static interface PositionAwareQueueTaskResult<V> extends QueueTaskResult<V> {
		
		/** @since 00.02.09 */
		static final int UNDEFINED_POSITION = Integer.MIN_VALUE;
		
		IntegerProperty queuePositionProperty();
		int position();
		int queuePosition();
		
		/** @since 00.02.09 */
		IntegerProperty pauseQueuePositionProperty();
		/** @since 00.02.09 */
		int pausePosition();
		/** @since 00.02.09 */
		int pauseQueuePosition();
	}
}