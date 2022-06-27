package sune.app.mediadown.util;

import java.util.Objects;

@FunctionalInterface
public interface CheckedBiFunction<T, U, R> {
	
	R apply(T t, U u) throws Exception;
	
	default <V> CheckedBiFunction<T, U, V> andThen(CheckedFunction<? super R, ? extends V> after) throws Exception {
		Objects.requireNonNull(after);
		return (T t, U u) -> after.apply(apply(t, u));
	}
}