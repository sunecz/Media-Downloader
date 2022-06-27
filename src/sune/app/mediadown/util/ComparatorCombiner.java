package sune.app.mediadown.util;

import java.util.Comparator;
import java.util.Objects;

/** @since 00.02.05 */
public final class ComparatorCombiner<T> implements Comparator<T> {
	
	private Comparator<T> comparator;
	
	private ComparatorCombiner(Comparator<T> comparator) {
		this.comparator = comparator; // Can be null
	}
	
	private static final <T> Comparator<T> combining(Comparator<T> first, Comparator<T> second) {
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
		return ((a, b) -> { int cmp; return (cmp = first.compare(a, b)) == 0 ? second.compare(a, b) : cmp; });
	}
	
	public static final <T> ComparatorCombiner<T> empty() {
		return new ComparatorCombiner<>(null);
	}
	
	public static final <T extends Comparable<? super T>> ComparatorCombiner<T> natural() {
		return new ComparatorCombiner<>(Comparator.<T>naturalOrder());
	}
	
	public static final <T> ComparatorCombiner<T> of(Comparator<T> comparator) {
		return new ComparatorCombiner<>(Objects.requireNonNull(comparator));
	}
	
	public final ComparatorCombiner<T> combine(Comparator<T> other) {
		comparator = comparator == null ? other : combining(comparator, other);
		return this;
	}
	
	@Override
	public int compare(T a, T b) {
		if(comparator == null)
			throw new IllegalStateException("Invalid comparator");
		return comparator.compare(a, b);
	}
	
	public final boolean isValid() {
		return comparator != null;
	}
}