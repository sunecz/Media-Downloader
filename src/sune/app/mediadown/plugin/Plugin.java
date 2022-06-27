package sune.app.mediadown.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {
	
	// General properties
	String name();
	String title();
	String version();
	String author();
	/** @since 00.02.02 */
	String moduleName() default "";
	// Update-related properties
	boolean updatable() default false;
	String updateBaseURL() default "";
	// Miscellaneous properties
	String url() default "";
	String icon() default "";
}