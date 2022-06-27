package sune.app.mediadown.util;

@FunctionalInterface
public interface CheckedFunction<T, R> {
	
	R apply(T t) throws Exception;
}