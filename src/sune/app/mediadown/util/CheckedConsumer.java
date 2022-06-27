package sune.app.mediadown.util;

@FunctionalInterface
public interface CheckedConsumer<T> {
	
	void accept(T t) throws Exception;
}