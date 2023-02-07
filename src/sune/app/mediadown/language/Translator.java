package sune.app.mediadown.language;

import java.util.regex.Matcher;

import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.plugin.PluginFile;
import sune.app.mediadown.plugin.Plugins;
import sune.app.mediadown.util.Regex;

/** @since 00.02.08 */
public final class Translator {
	
	private static final Regex REGEX_TRANSLATE = Regex.of("^tr\\(([^,]+),\\s*([^\\)]+)\\)$");
	
	// Forbid anyone to create an instance of this class
	private Translator() {
	}
	
	private static final boolean quickCanTranslate(String state) {
		return state != null && state.indexOf("tr(") == 0;
	}
	
	private static final Translation translation(String context) {
		int index = context.indexOf(':');
		
		if(index < 0) {
			return MediaDownloader.translation();
		}
		
		String schema = context.substring(0, index);
		switch(schema) {
			case "plugin": {
				String name = context.substring(index + 1);
				PluginFile plugin = Plugins.getLoaded(name);
				
				if(plugin != null) {
					return plugin.getInstance().translation();
				}
				
				// Fall-through
			}
			default: {
				return MediaDownloader.translation();
			}
		}
	}
	
	private static final String translate(String state) {
		Matcher matcher = REGEX_TRANSLATE.matcher(state);
		
		if(!matcher.matches()) {
			return state;
		}
		
		String context = matcher.group(1);
		String path = matcher.group(2);
		return translation(context).getSingle(path);
	}
	
	public static final String maybeTranslate(String state) {
		return quickCanTranslate(state) ? translate(state) : state;
	}
}