package sune.app.mediadown.util;

public class Range<T extends Number & Comparable<T>> {
	
	private final T from;
	private final T to;
	
	public Range(T from, T to) {
		this.from = from;
		this.to   = to;
	}
	
	public static final <T extends Number & Comparable<T>> Range<T> of(T from, T to) {
		T _from = from;
		T _to   = to;
		if((to.compareTo(from) < 0)) {
			_from = to;
			_to   = from;
		}
		return new Range<>(_from, _to);
	}
	
	public Range<T> setFrom(T newFrom) {
		return new Range<>(newFrom, to);
	}
	
	public Range<T> setTo(T newTo) {
		return new Range<>(from, newTo);
	}
	
	public Range<T> copy() {
		return new Range<>(from, to);
	}
	
	public T from() {
		return from;
	}
	
	public T to() {
		return to;
	}
	
	@Override
	public String toString() {
		return String.format("Range[from=%s, to=%s]", from.toString(), to.toString());
	}
}