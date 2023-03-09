package sune.app.mediadown.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import sune.app.mediadown.util.CheckedSupplier;
import sune.app.mediadown.util.UncheckedException;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.08 */
public final class ValidableValue<T> {
	
	private static final Object INVALID = new Object();
	
	private final AtomicReference<Object> value;
	private final CheckedSupplier<T> supplier;
	
	private ValidableValue(CheckedSupplier<T> supplier, Object value) {
		this.value = new AtomicReference<>(value);
		this.supplier = Objects.requireNonNull(supplier);
	}
	
	public static final <T> ValidableValue<T> of(Supplier<T> supplier) {
		return ofChecked(supplier::get);
	}
	
	public static final <T> ValidableValue<T> of(Supplier<T> supplier, T validValue) {
		return ofChecked(supplier::get, validValue);
	}
	
	public static final <T> ValidableValue<T> ofChecked(CheckedSupplier<T> supplier) {
		return new ValidableValue<>(supplier, INVALID);
	}
	
	public static final <T> ValidableValue<T> ofChecked(CheckedSupplier<T> supplier, T validValue) {
		return new ValidableValue<>(supplier, validValue);
	}
	
	public final void invalidate() {
		value.set(INVALID);
	}
	
	public final void set(T newValue) {
		value.set(newValue);
	}
	
	public final T valueChecked() throws Exception {
		Object val;
		
		if((val = value.get()) == INVALID) {
			for(Object supplied = supplier.get();
					(val = value.compareAndExchange(INVALID, supplied)) == INVALID;);
		}
		
		@SuppressWarnings("unchecked")
		T casted = (T) val;
		
		return casted;
	}
	
	public final T value() {
		return Ignore.call(this::valueChecked, UncheckedException::new);
	}
	
	public final T valueOrElse(T defaultValue) {
		return Ignore.defaultValue(this::valueChecked, defaultValue);
	}
	
	public final boolean isValid() {
		return value.get() != INVALID;
	}
}