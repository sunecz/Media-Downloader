package sune.app.mediadown.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** @since 00.01.27 */
public final class History<T> {
	
	private final List<T> data = new LinkedList<>();
	private int position;
	private int size;
	
	public History() {
		position = -1;
	}
	
	private final void checkPosition() {
		position = Math.min(Math.max(-1, position), size - 1);
	}
	
	public final void add(T item) {
		if((position + 1 < size)) {
			data.subList(position + 1, size).clear();
			size = position + 1;
		}
		data.add(item);
		++size;
		++position;
	}
	
	private final void move(int n) {
		position += n;
		checkPosition();
	}
	
	public final void backward(int n) {
		if((n <= 0))
			throw new IllegalArgumentException();
		move(-n);
	}
	
	public final void forward(int n) {
		if((n <= 0))
			throw new IllegalArgumentException();
		move(n);
	}
	
	public final void backward() {
		backward(1);
	}
	
	public final void forward() {
		forward(1);
	}
	
	public final T get() {
		// Return null if empty rather than throwing an exception
		return position >= 0 ? data.get(position) : null;
	}
	
	public final T peekBackward() {
		return peekBackward(1);
	}
	
	public final T peekBackward(int n) {
		backward(n);
		T item = get();
		forward(n);
		return item;
	}
	
	public final T peekForward() {
		return peekForward(1);
	}
	
	public final T peekForward(int n) {
		forward(n);
		T item = get();
		backward(n);
		return item;
	}
	
	public final T backwardAndGet() {
		return backwardAndGet(1);
	}
	
	public final T backwardAndGet(int n) {
		backward(n);
		return get();
	}
	
	public final T getAndBackward() {
		return getAndBackward(1);
	}
	
	public final T getAndBackward(int n) {
		T item = get();
		backward(n);
		return item;
	}
	
	public final T forwardAndGet() {
		return forwardAndGet(1);
	}
	
	public final T forwardAndGet(int n) {
		forward(n);
		return get();
	}
	
	public final T getAndForward() {
		return getAndForward(1);
	}
	
	public final T getAndForward(int n) {
		T item = get();
		forward(n);
		return item;
	}
	
	public final boolean canGoBackward() {
		return position > 0;
	}
	
	public final boolean canGoForward() {
		return position < size - 1;
	}
	
	public final void clear() {
		data.clear();
		size = 0;
		position = -1;
	}
	
	public final int position() {
		return position;
	}
	
	public final int size() {
		return size;
	}
	
	public final List<T> toList() {
		return new ArrayList<>(data);
	}
}