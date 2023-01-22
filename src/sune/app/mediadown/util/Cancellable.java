package sune.app.mediadown.util;

/** @since 00.02.08 */
@FunctionalInterface
public interface Cancellable {
	
	void cancel() throws Exception;
}