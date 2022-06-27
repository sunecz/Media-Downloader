package sune.app.mediadown.util;

import java.lang.reflect.Array;
import java.util.Objects;

/** @since 00.02.05 */
public final class ArrayWrapper<T> {
	
	private final Object original;
	private Object copy;
	
	private ArrayWrapper(Object original) {
		this.original = Objects.requireNonNull(original);
	}
	
	public static final <T> ArrayWrapper<T> of(T[] array) {
		return new ArrayWrapper<>(array);
	}
	
	public final T[] array() {
		@SuppressWarnings("unchecked")
		T[] array = (T[]) original;
		return array;
	}
	
	public final T[] copy() {
		if(copy == null) {
			int length = Array.getLength(original);
			@SuppressWarnings("unchecked")
			T[] newArray = (T[]) Array.newInstance(original.getClass().getComponentType(), length);
			System.arraycopy(original, 0, newArray, 0, length);
			copy = newArray;
		}
		@SuppressWarnings("unchecked")
		T[] array = (T[]) copy;
		return array;
	}
}