package sune.app.mediadown.util;

/** @since 00.02.05 */
@FunctionalInterface
public interface ThrowableCallable<V> {
	
	V run() throws Throwable;
}