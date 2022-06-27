package sune.app.mediadown.util;

import java.util.Objects;

/** @since 00.02.05 */
@FunctionalInterface
public interface CheckedQuadFunction<A, B, C, D, R> {
	
	R apply(A a, B b, C c, D d) throws Exception;
	default <V> CheckedQuadFunction<A, B, C, D, V> andThen(CheckedFunction<? super R, ? extends V> after) {
		Objects.requireNonNull(after);
		return (A a, B b, C c, D d) -> after.apply(apply(a, b, c, d));
	}
}