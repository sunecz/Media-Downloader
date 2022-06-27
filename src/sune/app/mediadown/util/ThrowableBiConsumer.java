package sune.app.mediadown.util;

@FunctionalInterface
public interface ThrowableBiConsumer<T, U> {
	
	void accept(T t, U u) throws Exception;
}