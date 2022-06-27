package sune.app.mediadown.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a class file that is used to bootstrap a plugin.
 * The bootstrap process is a process where all required classes and files
 * are initialized and made ready for loading the plugin afterwards.
 * For example, a bootstrap process may load additional libraries.
 * @since 00.02.02
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PluginBootstrap {
	
	Class<? extends PluginBase> pluginClass();
}