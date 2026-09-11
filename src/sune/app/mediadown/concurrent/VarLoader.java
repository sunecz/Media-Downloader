package sune.app.mediadown.concurrent;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.function.Supplier;

import sune.app.mediadown.exception.UncheckedException;
import sune.app.mediadown.util.CheckedSupplier;

/** @since 00.02.08 */
public final class VarLoader<T> {
	
	private static final Object UNSET = new Object();
	/** @since 00.02.09 */
	private static final VarHandle HANDLE;
	
	private final CheckedSupplier<T> supplier;
	@SuppressWarnings("unused")
	private volatile Object value = UNSET;
	
	static {
		try {
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			HANDLE = lookup.findVarHandle(VarLoader.class, "value", Object.class);
		} catch(NoSuchFieldException | IllegalAccessException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
	
	private VarLoader(CheckedSupplier<T> supplier) {
		this.supplier = Objects.requireNonNull(supplier);
	}
	
	public static final <T> VarLoader<T> of(Supplier<T> supplier) {
		return new VarLoader<>(supplier::get);
	}
	
	public static final <T> VarLoader<T> ofChecked(CheckedSupplier<T> supplier) {
		return new VarLoader<>(supplier);
	}
	
	/** @since 00.02.09 */
	private final void atomicSet(Object value) {
		HANDLE.setRelease(this, value);
	}
	
	/** @since 00.02.09 */
	private final Object atomicGet() {
		return HANDLE.getAcquire(this);
	}
	
	/** @since 00.02.09 */
	private final Object valueRaw() throws Exception {
		Object ref = atomicGet();
		
		if(ref == UNSET) {
			synchronized(this) {
				ref = atomicGet();
				
				if(ref == UNSET) {
					ref = supplier.get();
					atomicSet(ref);
				}
			}
		}
		
		return ref;
	}
	
	/** @since 00.02.09 */
	public void unset() {
		Object ref = atomicGet();
		
		if(ref != UNSET) {
			synchronized(this) {
				ref = atomicGet();
				
				if(ref != UNSET) {
					atomicSet(UNSET);
				}
			}
		}
	}
	
	public T valueChecked() throws Exception {
		@SuppressWarnings("unchecked")
		T casted = (T) valueRaw();
		return casted;
	}
	
	public final T value() {
		try {
			return valueChecked();
		} catch(Exception ex) {
			throw new UncheckedException(ex);
		}
	}
	
	public final T valueOrElse(T defaultValue) {
		try {
			return valueChecked();
		} catch(Exception ex) {
			// Ignore
		}
		
		return defaultValue;
	}
	
	/** @since 00.02.09 */
	public final T valueOrElseGet(Supplier<T> supplier) {
		try {
			return valueChecked();
		} catch(Exception ex) {
			// Ignore
		}
		
		return supplier.get();
	}
	
	public final boolean isSet() {
		return atomicGet() != UNSET;
	}
	
	/** @since 00.02.09 */
	public final boolean isUnset() {
		return atomicGet() == UNSET;
	}
}