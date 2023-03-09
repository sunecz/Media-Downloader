package sune.app.mediadown.media;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import sune.app.mediadown.concurrent.ValidableValue;
import sune.app.mediadown.util.Utils;

/** @since 00.02.05 */
public final class MediaLanguage {
	
	private static final Map<String, MediaLanguage> registered = new LinkedHashMap<>();
	private static final ValidableValue<MediaLanguage[]> values;
	private static final ValidableValue<MediaLanguage[]> determinedLanguages;
	
	static {
		values = ValidableValue.of(MediaLanguage::newValues);
		determinedLanguages = ValidableValue.of(MediaLanguage::newDeterminedLanguages);
	}
	
	// Special media languages
	public static final MediaLanguage UNKNOWN;
	public static final MediaLanguage UNDETERMINED;
	// Common media languages (see: https://www.loc.gov/standards/iso639-2/php/code_list.php)
	public static final MediaLanguage ENGLISH;
	public static final MediaLanguage CZECH;
	public static final MediaLanguage SLOVAK;
	public static final MediaLanguage ALBANIAN;
	public static final MediaLanguage ARABIC;
	public static final MediaLanguage AUSTRALIAN;
	public static final MediaLanguage BELARUSIAN;
	public static final MediaLanguage BENGALI;
	public static final MediaLanguage BOSNIAN;
	public static final MediaLanguage BULGARIAN;
	public static final MediaLanguage CHINESE;
	public static final MediaLanguage CROATIAN;
	public static final MediaLanguage DANISH;
	public static final MediaLanguage DUTCH;
	public static final MediaLanguage EGYPTIAN;
	public static final MediaLanguage ESPERANTO;
	public static final MediaLanguage ESTONIAN;
	public static final MediaLanguage FINNISH;
	public static final MediaLanguage FRENCH;
	public static final MediaLanguage GERMAN;
	public static final MediaLanguage GREEK;
	public static final MediaLanguage HAWAIIAN;
	public static final MediaLanguage HEBREW;
	public static final MediaLanguage HINDI;
	public static final MediaLanguage HUNGARIAN;
	public static final MediaLanguage ICELANDIC;
	public static final MediaLanguage INDONESIAN;
	public static final MediaLanguage IRISH;
	public static final MediaLanguage ITALIAN;
	public static final MediaLanguage JAPANESE;
	public static final MediaLanguage KOREAN;
	public static final MediaLanguage LATIN;
	public static final MediaLanguage LATVIAN;
	public static final MediaLanguage MACEDONIAN;
	public static final MediaLanguage MONGOLIAN;
	public static final MediaLanguage MONTENEGRIN;
	public static final MediaLanguage NORWEGIAN;
	public static final MediaLanguage PANJABI;
	public static final MediaLanguage PERSIAN;
	public static final MediaLanguage POLISH;
	public static final MediaLanguage PORTUGUESE;
	public static final MediaLanguage ROMANIAN;
	public static final MediaLanguage RUSSIAN;
	public static final MediaLanguage SERBIAN;
	public static final MediaLanguage SLOVENIAN;
	public static final MediaLanguage SPANISH;
	public static final MediaLanguage SWEDISH;
	public static final MediaLanguage THAI;
	public static final MediaLanguage TURKISH;
	public static final MediaLanguage UKRAINIAN;
	public static final MediaLanguage VIETNAMESE;
	
	static {
		UNKNOWN      = new Builder().name("UNKNOWN").build();
		UNDETERMINED = new Builder().name("UNDETERMINED").codes("und").build();
		ENGLISH      = new Builder().name("ENGLISH").codes("eng", "en").build();
		CZECH        = new Builder().name("CZECH").codes("cze", "ces", "cs").build();
		SLOVAK       = new Builder().name("SLOVAK").codes("slo", "slk", "sk").build();
		ALBANIAN     = new Builder().name("ALBANIAN").codes("alb", "sqi", "sq").build();
		ARABIC       = new Builder().name("ARABIC").codes("ara", "ar").build();
		AUSTRALIAN   = new Builder().name("AUSTRALIAN").codes("aus").build();
		BELARUSIAN   = new Builder().name("BELARUSIAN").codes("bel", "be").build();
		BENGALI      = new Builder().name("BENGALI").codes("ben", "bn").build();
		BOSNIAN      = new Builder().name("BOSNIAN").codes("bos", "bs").build();
		BULGARIAN    = new Builder().name("BULGARIAN").codes("bul", "bg").build();
		CHINESE      = new Builder().name("CHINESE").codes("chi", "zho", "zh").build();
		CROATIAN     = new Builder().name("CROATIAN").codes("hrv", "hr").build();
		DANISH       = new Builder().name("DANISH").codes("dan", "da").build();
		DUTCH        = new Builder().name("DUTCH").codes("dut", "nld", "nl").build();
		EGYPTIAN     = new Builder().name("EGYPTIAN").codes("egy").build();
		ESPERANTO    = new Builder().name("ESPERANTO").codes("epo", "eo").build();
		ESTONIAN     = new Builder().name("ESTONIAN").codes("est", "et").build();
		FINNISH      = new Builder().name("FINNISH").codes("fin", "fi").build();
		FRENCH       = new Builder().name("FRENCH").codes("fre", "fra", "fr").build();
		GERMAN       = new Builder().name("GERMAN").codes("ger", "deu", "de").build();
		GREEK        = new Builder().name("GREEK").codes("gre", "ell", "el").build();
		HAWAIIAN     = new Builder().name("HAWAIIAN").codes("haw").build();
		HEBREW       = new Builder().name("HEBREW").codes("heb", "he").build();
		HINDI        = new Builder().name("HINDI").codes("hin", "hi").build();
		HUNGARIAN    = new Builder().name("HUNGARIAN").codes("hun", "hu").build();
		ICELANDIC    = new Builder().name("ICELANDIC").codes("ice", "isl", "is").build();
		INDONESIAN   = new Builder().name("INDONESIAN").codes("ind", "id").build();
		IRISH        = new Builder().name("IRISH").codes("gle", "ga").build();
		ITALIAN      = new Builder().name("ITALIAN").codes("ita", "it").build();
		JAPANESE     = new Builder().name("JAPANESE").codes("jpn", "ja").build();
		KOREAN       = new Builder().name("KOREAN").codes("kor", "ko").build();
		LATIN        = new Builder().name("LATIN").codes("lat", "la").build();
		LATVIAN      = new Builder().name("LATVIAN").codes("lav", "lv").build();
		MACEDONIAN   = new Builder().name("MACEDONIAN").codes("mac", "mkd", "mk").build();
		MONGOLIAN    = new Builder().name("MONGOLIAN").codes("mon", "mn").build();
		MONTENEGRIN  = new Builder().name("MONTENEGRIN").codes("cnr").build();
		NORWEGIAN    = new Builder().name("NORWEGIAN").codes("nor", "no").build();
		PANJABI      = new Builder().name("PANJABI").codes("pan", "pa").build();
		PERSIAN      = new Builder().name("PERSIAN").codes("per", "fas", "fa").build();
		POLISH       = new Builder().name("POLISH").codes("pol", "pl").build();
		PORTUGUESE   = new Builder().name("PORTUGUESE").codes("por", "pt").build();
		ROMANIAN     = new Builder().name("ROMANIAN").codes("rum", "ron", "ro").build();
		RUSSIAN      = new Builder().name("RUSSIAN").codes("rus", "ru").build();
		SERBIAN      = new Builder().name("SERBIAN").codes("srp", "sr").build();
		SLOVENIAN    = new Builder().name("SLOVENIAN").codes("slv", "sl").build();
		SPANISH      = new Builder().name("SPANISH").codes("spa", "es").build();
		SWEDISH      = new Builder().name("SWEDISH").codes("swe", "sv").build();
		THAI         = new Builder().name("THAI").codes("tha", "th").build();
		TURKISH      = new Builder().name("TURKISH").codes("tur", "tr").build();
		UKRAINIAN    = new Builder().name("UKRAINIAN").codes("ukr", "uk").build();
		VIETNAMESE   = new Builder().name("VIETNAMESE").codes("vie", "vi").build();
	}
	
	private final String name;
	private final List<String> codes;
	
	private MediaLanguage(String name, List<String> codes) {
		this.name = requireValidName(name);
		this.codes = Objects.requireNonNull(Utils.nonNullContent(codes));
		register(this);
	}
	
	private static final String requireValidName(String name) {
		if(name == null || name.isBlank())
			throw new IllegalArgumentException("Name may be neither null nor blank.");
		return name;
	}
	
	private static final void register(MediaLanguage language) {
		synchronized(registered) {
			if(registered.putIfAbsent(language.name.toLowerCase(), language) != null) {
				throw new IllegalStateException("Media language \"" + language.name + "\" already registered.");
			}
			
			values.invalidate();
			determinedLanguages.invalidate();
		}
	}
	
	private static final Stream<MediaLanguage> allLanguages() {
		List<MediaLanguage> languages;
		
		synchronized(registered) {
			languages = List.copyOf(registered.values());
		}
		
		return languages.stream();
	}
	
	private static final MediaLanguage[] newValues() {
		return allLanguages().toArray(MediaLanguage[]::new);
	}
	
	private static final MediaLanguage[] newDeterminedLanguages() {
		return allLanguages()
					.filter((l) -> !l.isAnyOf(MediaLanguage.UNKNOWN, MediaLanguage.UNDETERMINED))
					.toArray(MediaLanguage[]::new);
	}
	
	public static final MediaLanguage[] values() {
		return values.value();
	}
	
	public static final MediaLanguage[] determinedLanguages() {
		return determinedLanguages.value();
	}
	
	public static final MediaLanguage ofName(String name) {
		return registered.entrySet().stream()
					.filter((e) -> e.getKey().equalsIgnoreCase(name))
					.map(Map.Entry::getValue)
					.findFirst().orElse(UNKNOWN);
	}
	
	public static final MediaLanguage ofCodes(Set<String> codes) {
		Objects.requireNonNull(codes);
		return registered.entrySet().stream()
					.filter((e) -> e.getValue().codes().stream().filter(codes::contains).findAny().isPresent())
					.map(Map.Entry::getValue)
					.findFirst().orElse(UNKNOWN);
	}
	
	public static final MediaLanguage ofCodes(List<String> codes) {
		return ofCodes(new HashSet<>(Objects.requireNonNull(codes)));
	}
	
	public static final MediaLanguage ofCodes(String... codes) {
		return ofCodes(Set.of(Objects.requireNonNull(codes)));
	}
	
	// Conveniently-named method for a single language code
	public static final MediaLanguage ofCode(String code) {
		return ofCodes(Objects.requireNonNull(code));
	}
	
	public String name() {
		return name;
	}
	
	public List<String> codes() {
		return Collections.unmodifiableList(codes);
	}
	
	// Method just for convenience
	public boolean is(MediaLanguage language) {
		return equals(language);
	}
	
	// Method just for convenience
	public boolean isAnyOf(MediaLanguage... languages) {
		return Stream.of(languages).anyMatch(this::is);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(codes, name);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MediaLanguage other = (MediaLanguage) obj;
		return Objects.equals(codes, other.codes) && Objects.equals(name, other.name);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public static final class Builder {
		
		private static final List<String> EMPTY_CODES = List.of();
		
		private String name;
		private List<String> codes;
		
		public Builder() {
			codes = EMPTY_CODES;
		}
		
		public MediaLanguage build() {
			return new MediaLanguage(requireValidName(name), Objects.requireNonNull(Utils.nonNullContent(codes)));
		}
		
		public Builder name(String name) {
			this.name = requireValidName(name);
			return this;
		}
		
		public Builder codes(String... codes) {
			this.codes = List.of(Objects.requireNonNull(Utils.nonNullContent(codes)));
			return this;
		}
		
		public String name() {
			return name;
		}
		
		public List<String> codes() {
			return Collections.unmodifiableList(codes);
		}
	}
}