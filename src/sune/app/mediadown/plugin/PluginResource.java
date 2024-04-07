package sune.app.mediadown.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javafx.scene.image.Image;
import sune.app.mediadown.plugin.PluginMemory.MemoryFile;
import sune.app.mediadown.resource.InternalURLProtocols;

public final class PluginResource {
	
	private static final String BASE_PATH 		   = "resources/";
	private static final String BASE_PATH_STYLE    = BASE_PATH + "style/";
	private static final String BASE_PATH_LANGUAGE = BASE_PATH + "language/";
	private static final String BASE_PATH_ICON     = BASE_PATH + "icon/";
	
	static {
		InternalURLProtocols.register(PluginProtocol.PROTOCOL_NAME, new PluginProtocol());
	}
	
	// Forbid anyone to create an instance of this class
	private PluginResource() {
	}
	
	private static final String resource(PluginFile plugin, String path) {
		return "plugin:" + plugin.getPlugin().instance().name() + "," + path;
	}
	
	private static final InputStream resourceStream(PluginFile plugin, String path) {
		PluginMemory memory = plugin.getMemory();
		MemoryFile   file   = memory.getFile(path);
		try(ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			memory.getFileContent(file, output);
			return new ByteArrayInputStream(output.toByteArray());
		}  catch(IOException ex) {
		}
		return null;
	}
	
	public static final URL url(PluginFile plugin, String base, String path) {
		try {
			return new URL(resource(plugin, base + path));
		} catch(MalformedURLException ex) {
			// Ignore
		}
		return null;
	}
	
	public static final InputStream stream(String path) {
		return stream(PluginLoaderContext.getContext(), path);
	}
	
	public static final InputStream stream(PluginFile plugin, String path) {
		return stream(plugin, "", path);
	}
	
	public static final InputStream stream(String base, String path) {
		return stream(PluginLoaderContext.getContext(), base, path);
	}
	
	public static final InputStream stream(PluginFile plugin, String base, String path) {
		return resourceStream(plugin, base + path);
	}
	
	public static final String stylesheet(String path) {
		return stylesheet(PluginLoaderContext.getContext(), path);
	}
	
	public static final String stylesheet(PluginFile plugin, String path) {
		return resource(plugin, BASE_PATH_STYLE + path);
	}
	
	public static final InputStream language(String path) {
		return language(PluginLoaderContext.getContext(), path);
	}
	
	public static final InputStream language(PluginFile plugin, String path) {
		return stream(plugin, BASE_PATH_LANGUAGE, path);
	}
	
	public static final Image icon(String path) {
		return icon(PluginLoaderContext.getContext(), path);
	}
	
	public static final Image icon(PluginFile plugin, String path) {
		InputStream stream;
		return (stream = stream(plugin, BASE_PATH_ICON, path)) != null
					? new Image(stream)
					: null;
	}
	
	private static final class PluginProtocol extends URLStreamHandler {
		
		private static final String PROTOCOL_NAME = "plugin";
		
		private PluginProtocol() {
		}
		
		@Override
		protected URLConnection openConnection(URL url) throws IOException {
			return new Connection(url);
		}
		
		private static final class Connection extends URLConnection {
			
			private final String pluginName;
			private final String resourcePath;
			
			protected Connection(URL url) {
				super(checkUrl(url));
				String path = url.getPath();
				
				int index;
				if((index = path.indexOf(',')) <= 0) {
					throw new IllegalArgumentException("Unspecified resource path");
				}
				
				pluginName   = path.substring(0, index);
				resourcePath = path.substring(index + 1);
				
				if(resourcePath.length() <= 0) {
					throw new IllegalArgumentException("Invalid resource path");
				}
			}
			
			private static final URL checkUrl(URL url) {
				if(!url.getProtocol().equalsIgnoreCase(PROTOCOL_NAME)) {
					throw new IllegalArgumentException("Invalid protocol");
				}
				
				return url;
			}
			
			@Override
			public void connect() throws IOException {
				// Nothing to do
			}
			
			@Override
			public InputStream getInputStream() throws IOException {
				PluginFile plugin = Plugins.getLoaded(pluginName);
				
				PluginMemory memory;
				if((memory = plugin.getMemory()) != null) {
					MemoryFile file = memory.getFile(resourcePath);
					
					if(file != null) {
						try(ByteArrayOutputStream output = new ByteArrayOutputStream()) {
							memory.getFileContent(file, output);
							return new ByteArrayInputStream(output.toByteArray());
						}
					}
				}
				
				return null;
			}
		}
	}
}