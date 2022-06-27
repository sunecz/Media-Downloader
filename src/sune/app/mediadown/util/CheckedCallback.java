package sune.app.mediadown.util;

/** @since 00.02.05 */
@FunctionalInterface
public interface CheckedCallback<P, R> {
	
	R call(P param) throws Exception;
}