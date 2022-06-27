package sune.app.mediadown.util;

/** @since 00.02.05 */
@FunctionalInterface
public interface CheckedSupplier<T> {
	
	T get() throws Exception;
}