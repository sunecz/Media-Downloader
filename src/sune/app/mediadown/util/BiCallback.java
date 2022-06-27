package sune.app.mediadown.util;

@FunctionalInterface
public interface BiCallback<A, B, R> {
	
	R call(A a, B b);
}