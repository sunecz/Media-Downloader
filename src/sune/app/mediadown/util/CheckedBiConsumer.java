package sune.app.mediadown.util;

import java.util.Objects;

/** @since 00.02.05 */
@FunctionalInterface
public interface CheckedBiConsumer<T, U> {
	
	void accept(T t, U u) throws Exception;
	
	default CheckedBiConsumer<T, U> andThen(CheckedBiConsumer<? super T, ? super U> after) {
		Objects.requireNonNull(after);
		return (l, r) -> {
			accept(l, r);
			after.accept(l, r);
		};
	}
}