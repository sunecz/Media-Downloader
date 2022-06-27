package sune.app.mediadown.util;

import java.util.Objects;

public final class Pair<A, B> {
	
	public final A a;
	public final B b;
	
	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}
	
	/** @since 00.01.27 */
	@Override
	public final String toString() {
		return String.format("Pair<%s, %s>(%s, %s)",
			Utils.getClass(a).getSimpleName(),
			Utils.getClass(b).getSimpleName(),
			Objects.toString(a),
			Objects.toString(b));
	}
}