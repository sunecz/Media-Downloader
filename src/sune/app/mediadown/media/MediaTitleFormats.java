package sune.app.mediadown.media;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** @since 00.02.05 */
public final class MediaTitleFormats {
	
	private static final Map<String, NamedMediaTitleFormat> formats = new LinkedHashMap<>();
	
	// Register builtin formats
	static {
		register("builtin_1", builtinFormat1());
		register("builtin_2", builtinFormat2());
	}
	
	// Example: Program name - 02x04 - Episode name
	private static final MediaTitleFormat builtinFormat1() {
		String season  = "{?is([season])|"
				+ "{tl({:tr('word_season')})} [season]|" // String
				+ "{f('%02d', [season])}{?o({?([split])}, {?!([episode])})|. {l({:tr('word_season')})}|}}"; // Number
		String episode = "{?is([episode])|"
				+ "{tl({:tr('word_episode')})} [episode]|" // String
				+ "{f('%02d', [episode])}{?o({?([split])}, {?!([season])})|. {l({:tr('word_episode')})}|}}"; // Number
		return MediaTitleFormat.of(
			// Program name
			"{tl([program_name])}"
		  + "{?a({?o({?is([season])}, {?>([season], 0)})}, {?o({?is([episode])}, {?>([episode], 0)})})"
		      // Both season and episode
		      + "| - " + season + "{?([split])| - |x}" + episode
		      + "|{?!([season])"
		          // Only episode or nothing
		          + "|{?o({?is([episode])}, {?>([episode], 0)})| - " + episode + "|}"
		          // Only season
		          + "|{?o({?is([season])}, {?>([season], 0)})| - " + season + "|}}}"
		    // Episode name
		  + "{?([episode_name])| - [episode_name]|}"
		);
	}
	
	// Example: Program.Name.S02E04.Episode.Name
	private static final MediaTitleFormat builtinFormat2() {
		String season  = "{?is([season])|[season]|{f('%02d', [season])}}";
		String episode = "{?is([episode])|[episode]|{f('%02d', [episode])}}";
		return MediaTitleFormat.of(
			// Program name
			"{r({t([program_name])}, ' ', '.')}"
			// Separator
		  + "{?o({?o({?is([season])}, {?>([season], 0)})}, {?o({?is([episode])}, {?>([episode], 0)})})|.|}"
		    // Season
		  + "{?o({?is([season])}, {?>([season], 0)})|S" + season + "|}"
		    // Episode
		  + "{?o({?is([episode])}, {?>([episode], 0)})|E" + episode + "|}"
		    // Episode name
		  + "{?([episode_name])|"
		        + "{?o({?o({?is([season])}, {?>([season], 0)})}, {?o({?is([episode])}, {?>([episode], 0)})})|.|_}"
		        + "{r({t([episode_name])}, ' ', '.')}|}"
		);
	}
	
	// Forbid anyone to create an instance of this class
	private MediaTitleFormats() {
	}
	
	public static final void register(String name, MediaTitleFormat format) {
		formats.put(Objects.requireNonNull(name), new NamedMediaTitleFormat(name, Objects.requireNonNull(format)));
	}
	
	public static final void unregister(String name) {
		formats.remove(Objects.requireNonNull(name));
	}
	
	public static final NamedMediaTitleFormat namedOfDefault() {
		return namedOfName(defaultName());
	}
	
	public static final NamedMediaTitleFormat namedOfName(String name) {
		return formats.get(name);
	}
	
	public static final MediaTitleFormat ofDefault() {
		return ofName(defaultName());
	}
	
	public static final MediaTitleFormat ofName(String name) {
		return Optional.ofNullable(formats.get(name)).map(NamedMediaTitleFormat::format).orElse(null);
	}
	
	public static final Map<String, MediaTitleFormat> all() {
		return allNamed().entrySet().stream()
					.map((e) -> Map.entry(e.getKey(), e.getValue().format()))
					.collect(Collectors.toMap(Map.Entry::getKey,
					                          Map.Entry::getValue,
					                          (a, b) -> a,
					                          LinkedHashMap::new));
	}
	
	public static final Map<String, NamedMediaTitleFormat> allNamed() {
		return Collections.unmodifiableMap(formats);
	}
	
	// Helper method
	public static final String defaultName() {
		return "builtin_1";
	}
	
	public static final class NamedMediaTitleFormat {
		
		private final String name;
		private final MediaTitleFormat format;
		
		public NamedMediaTitleFormat(String name, MediaTitleFormat format) {
			this.name = Objects.requireNonNull(name);
			this.format = Objects.requireNonNull(format);
		}
		
		public String name() { return name; }
		public MediaTitleFormat format() { return format; }
		
		@Override
		public String toString() {
			return name;
		}
	}
}