package sune.app.mediadown.util;

/** @since 00.02.09 */
public interface Ref<T> {
	
	T get();
	
	static class Immutable<T> implements Ref<T> {
		
		private final T value;
		
		public Immutable(T value) {
			this.value = value;
		}
		
		@Override
		public T get() {
			return value;
		}
	}
	
	static class Mutable<T> implements Ref<T> {
		
		private T value;
		
		public Mutable() {
			this.value = null;
		}
		
		public Mutable(T value) {
			this.value = value;
		}
		
		public void set(T value) {
			this.value = value;
		}
		
		@Override
		public T get() {
			return value;
		}
	}
}