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
	
	/** @since 00.02.09 */
	private static final int opSet(int current, int value) {
		return current | value;
	}
	
	/** @since 00.02.09 */
	private static final int opUnset(int current, int value) {
		return current & (~value);
	}
	
	private final void setValue(IntUnaryOperator op) {
		for(int value = state.get(), expected;
				(value = state.compareAndExchange(expected = value, op.applyAsInt(value))) != expected;
		);
	}
	
	/** @since 00.02.09 */
	private final boolean compareAndSetValue(int expected, int mask, IntUnaryOperator op) {
		int value = state.get(), check = expected;
		
		do {
			if((value = state.compareAndExchange(check, op.applyAsInt(value))) == check) {
				return true;
			}
			
			check = value;
		} while(((value & mask) ^ expected) == 0);
		
		return false;
	}
	
	public final void clear(int value) {
		state.set(value);
	}
	
	public final void set(int value) {
		setValue((current) -> opSet(current, value));
	}
	
	public final void unset(int value) {
		setValue((current) -> opUnset(current, value));
	}
	
	/** @since 00.02.09 */
	public final void setAndUnset(int set, int unset) {
		setValue((current) -> opUnset(opSet(current, set), unset));
	}
	
	/** @since 00.02.09 */
	public final boolean compareAndSet(int expected, int mask, int value) {
		return compareAndSetValue(expected, mask, (current) -> opSet(current, value));
	}
	
	/** @since 00.02.09 */
	public final boolean compareAndUnset(int expected, int mask, int value) {
		return compareAndSetValue(expected, mask, (current) -> opUnset(current, value));
	}
	
	/** @since 00.02.09 */
	public final boolean compareAndSetBit(boolean expected, int mask) {
		return compareAndSetBit(expected, mask, mask);
	}
	
	/** @since 00.02.09 */
	public final boolean compareAndSetBit(boolean expected, int mask, int value) {
		return compareAndSet(expected ? mask : 0, mask, value);
	}
	
	/** @since 00.02.09 */
	public final boolean compareAndUnsetBit(boolean expected, int mask) {
		return compareAndUnsetBit(expected, mask, mask);
	}
	
	/** @since 00.02.09 */
	public final boolean compareAndUnsetBit(boolean expected, int mask, int value) {
		return compareAndUnset(expected ? mask : 0, mask, value);
	}
	
	public final int get() {
		return state.get();
	}
	
	public final boolean is(int value) {
		return (state.get() & value) == value;
	}
	
	/** @since 00.02.09 */
	public final boolean is(int value, int mask) {
		return ((state.get() & mask) ^ value) == 0;
	}
}