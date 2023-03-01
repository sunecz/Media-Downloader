package sune.app.mediadown.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import sune.app.mediadown.util.CheckedSupplier;
import sune.app.mediadown.util.UncheckedException;
import sune.app.mediadown.util.Utils.Ignore;

/** @since 00.02.08 */
public final class VarLoader<T> {
	
	private static final Object UNSET = new Object();
	
	private final CheckedSupplier<T> supplier;
	private final AtomicReference<Object> value = new AtomicReference<>(UNSET);
	
	private VarLoader(CheckedSupplier<T> supplier) {
		this.supplier = Objects.requireNonNull(supplier);
	}
	
	public static final <T> VarLoader<T> of(Supplier<T> supplier) {
		return new VarLoader<>(supplier::get);
	}
	
	public static final <T> VarLoader<T> ofChecked(CheckedSupplier<T> supplier) {
		return new VarLoader<>(supplier);
	}
	
	public final T valueChecked() throws Exception {
		Object val;
		
		if((val = value.get()) == UNSET) {
			for(Object supplied = supplier.get();
					(val = value.compareAndExchange(UNSET, supplied)) == UNSET;);
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
	
	public final boolean isSet() {
		return value.get() != UNSET;
	}
}