package sune.app.mediadown.util;

import java.util.Objects;

/** @since 00.02.04 */
public final class MutablePair<A, B> {
	
	private A a;
	private B b;
	
	public MutablePair(A a, B b) {
		this.a = a;
		this.b = b;
	}
	
	public void a(A a) {
		this.a = a;
	}
	
	public void b(B b) {
		this.b = b;
	}
	
	public A a() {
		return a;
	}
	
	public B b() {
		return b;
	}
	
	@Override
	public final String toString() {
		return String.format("MutablePair<%s, %s>(%s, %s)",
			Utils.getClass(a).getSimpleName(),
			Utils.getClass(b).getSimpleName(),
			Objects.toString(a),
			Objects.toString(b));
	}
}