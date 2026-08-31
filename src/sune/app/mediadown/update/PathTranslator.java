package sune.app.mediadown.update;

import java.util.Map;
import java.util.Objects;

/** @since 00.02.09 */
public final class PathTranslator {
	
	private final Map<String, String> translations;
	
	public PathTranslator(Map<String, String> translations) {
		this.translations = Objects.requireNonNull(translations);
	}
	
	public String translate(String path) {
		for(int end = path.length();
				end >= 0;
				end = path.lastIndexOf('/', end - 1)) {
			String prefix = path.substring(0, end);
			String translated = translations.get(prefix);
			
			if(translated != null) {
				return translated + path.substring(end);
			}
		}
		
		return path;
	}
}
