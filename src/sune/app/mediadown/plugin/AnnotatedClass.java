package sune.app.mediadown.plugin;

import java.lang.annotation.Annotation;

/** @since 00.02.02 */
public final class AnnotatedClass<T extends Annotation> {
	
	private final T instance;
	private final String className;
	
	public AnnotatedClass(T instance, String className) {
		this.instance = instance;
		this.className = className;
	}
	
	public T instance() {
		return instance;
	}
	
	public String className() {
		return className;
	}
}