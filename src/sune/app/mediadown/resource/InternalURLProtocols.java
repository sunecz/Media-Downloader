package sune.app.mediadown.resource;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Manages custom URL protocols that are used internally by the application.
 * Pre-defined URL protocols are mentioned below. Custom protocols may be
 * registered or unregistered, however the internal and plugin protocol cannot
 * be unregistered.
 * </p>
 * 
 * <p>
 * <b>Internal resource protocol</b> (internal://) is used for specifying URLs
 * of internal resources that does not exist externally, i.e. they have not yet
 * been extracted. This protocol is registered externally by the
 * {@linkplain sune.app.mediadown.MediaDownloader MediaDownloader} class.
 * </p>
 * 
 * <p>
 * <b>The plugin protocol</b> (plugin://) is used for specifying URLs of plugin
 * resource files. This protocol is registered externally by the
 * {@linkplain sune.app.mediadown.plugin.PluginResource PluginResource} class.
 * </p>
 * 
 * @since 00.02.09
 */
public final class InternalURLProtocols {
	
	private static final InternalURLStreamHandlerFactory streamHandlerFactory;
	private static final Set<String> requiredProtocols = Set.of("internal", "plugin");
	
	static {
		streamHandlerFactory = new InternalURLStreamHandlerFactory();
		URL.setURLStreamHandlerFactory(streamHandlerFactory);
	}
	
	private InternalURLProtocols() {
	}
	
	public static final void register(String protocol, URLStreamHandler handler) {
		streamHandlerFactory.register(protocol, handler);
	}
	
	public static final void unregister(String protocol) {
		streamHandlerFactory.unregister(protocol);
	}
	
	private static final class InternalURLStreamHandlerFactory implements URLStreamHandlerFactory {
		
		private final Map<String, URLStreamHandler> handlers = new HashMap<>();
		
		private InternalURLStreamHandlerFactory() {
		}
		
		public void register(String protocol, URLStreamHandler handler) {
			if(protocol == null || protocol.isEmpty() || handler == null) {
				throw new IllegalArgumentException();
			}
			
			handlers.putIfAbsent(protocol, handler);
		}
		
		public void unregister(String protocol) {
			if(protocol == null || protocol.isEmpty()) {
				throw new IllegalArgumentException();
			}
			
			if(requiredProtocols.contains(protocol)) {
				return; // Cannot unregister
			}
			
			handlers.remove(protocol);
		}
		
		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			return handlers.get(protocol);
		}
	}
}