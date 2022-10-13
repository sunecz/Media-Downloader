package sune.app.mediadown;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

/** @since 00.02.08 */
public final class InternalState {
	
	private final AtomicInteger state;
	
	public InternalState() {
		this(0);
	}
	
	public InternalState(int initialState) {
		state = new AtomicInteger(initialState);
	}
	
	private final void setValue(IntUnaryOperator op) {
		int oldValue = state.get();
		int newValue = op.applyAsInt(oldValue);
		
		while(oldValue != newValue
				&& !state.compareAndSet(oldValue, newValue)) {
			oldValue = state.get();
			newValue = op.applyAsInt(oldValue);
		}
	}
	
	public final void clear(int value) {
		state.set(value);
	}
	
	public final void set(int value) {
		setValue((current) -> current | value);
	}
	
	public final void unset(int value) {
		setValue((current) -> current & (~value));
	}
	
	public final int get() {
		return state.get();
	}
	
	public final boolean is(int value) {
		return (state.get() & value) == value;
	}
}