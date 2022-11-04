package sune.app.mediadown.event;

/** @since 00.02.08 */
public interface EventBindableAction<T extends EventType, R> extends EventBindable<T> {
	
	R execute() throws Exception;
	
	static class OfNone<T extends EventType, R> implements EventBindableAction<T, R> {
		
		private final R value;
		
		public OfNone(R value) {
			this.value = value;
		}
		
		@Override
		public R execute() throws Exception {
			return value;
		}
		
		@Override
		public <V> void addEventListener(Event<? extends T, V> event, Listener<V> listener) {
			// Do nothing
		}
		
		@Override
		public <V> void removeEventListener(Event<? extends T, V> event, Listener<V> listener) {
			// Do nothing
		}
	}
	
	static abstract class OfBinder<T extends EventType, R> implements EventBindableAction<T, R> {
		
		private final EventBinder binder;
		
		public OfBinder(EventBinder binder) {
			this.binder = binder;
		}
		
		@Override
		public <V> void addEventListener(Event<? extends T, V> event, Listener<V> listener) {
			binder.addEventListener(event, listener);
		}
		
		@Override
		public <V> void removeEventListener(Event<? extends T, V> event, Listener<V> listener) {
			binder.addEventListener(event, listener);
		}
	}
}