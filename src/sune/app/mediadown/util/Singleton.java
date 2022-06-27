package sune.app.mediadown.util;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** @since 00.02.05 */
public final class Singleton {
	
	private static final int STACK_WALKER_MINIMAL_DEPTH = 4;
	private static final Set<Option> STACK_WALKER_OPTIONS = Set.of(Option.RETAIN_CLASS_REFERENCE);
	
	private static final Map<String, Object> singletons = new HashMap<>();
	
	// Forbid anyone to create an instance of this class
	private Singleton() {
	}
	
	private static final boolean notInSingleton(StackFrame frame) {
		return frame.getDeclaringClass() != Singleton.class;
	}
	
	private static final String signature(Object instance, String name) {
		return String.format("%d:%s", System.identityHashCode(instance), name);
	}
	
	private static final String signature(Object instance, StackFrame frame) {
		return String.format("%d:%s.%s%s", System.identityHashCode(instance), frame.getClassName(),
		                     frame.getMethodName(), frame.getDescriptor());
	}
	
	private static final String context(Object instance, String name) {
		// Since we use StackWalker as the current context, this class assumes
		// the knowledge of how deep in call stack we actually are relative to
		// the actual call by the user to the of(T) (or any other) method.
		return StackWalker.getInstance(STACK_WALKER_OPTIONS, STACK_WALKER_MINIMAL_DEPTH)
				          .walk((stream) -> name != null ? signature(instance, name)
				                                         : signature(instance, stream.filter(Singleton::notInSingleton)
				                                                                     .findFirst().orElseThrow()));
	}
	
	// Allows null values to also be stored
	@SuppressWarnings("unchecked")
	private static final <T> T store(Object instance, String name, Supplier<T> supplier) {
		String context = context(instance, name);
		Object stored  = singletons.get(context);
		if(stored != null || singletons.containsKey(context))
			return (T) stored;
		T value = supplier.get();
		singletons.put(context, value);
		return value;
	}
	
	private static final void discard(Object instance, String name) {
		singletons.remove(context(instance, name));
	}
	
	public static final <T> T ofStatic(T value) {
		return ofStatic(null, value);
	}
	
	public static final <T> T ofStatic(String name, T value) {
		return ofStatic(name, () -> value);
	}
	
	public static final <T> T ofStatic(Supplier<T> value) {
		return ofStatic(null, value);
	}
	
	public static final <T> T ofStatic(String name, Supplier<T> value) {
		return store(null, name, Objects.requireNonNull(value));
	}
	
	public static final <T> T of(Object instance, T value) {
		return of(instance, null, value);
	}
	
	public static final <T> T of(Object instance, String name, T value) {
		return of(Objects.requireNonNull(instance), name, () -> value);
	}
	
	public static final <T> T of(Object instance, Supplier<T> value) {
		return of(instance, null, value);
	}
	
	public static final <T> T of(Object instance, String name, Supplier<T> value) {
		return store(Objects.requireNonNull(instance), name, Objects.requireNonNull(value));
	}
	
	public static final void removeStatic(String name) {
		discard(null, name);
	}
	
	public static final void remove(Object instance, String name) {
		discard(Objects.requireNonNull(instance), name);
	}
}