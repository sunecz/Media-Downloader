package sune.app.mediadown.util;

import java.util.Objects;

@FunctionalInterface
public interface CheckedTriFunction<A, B, C, R> {
	
	R apply(A a, B b, C c) throws Exception;
	default <V> CheckedTriFunction<A, B, C, V> andThen(CheckedFunction<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (A a, B b, C c) -> after.apply(apply(a, b, c));
	}
}