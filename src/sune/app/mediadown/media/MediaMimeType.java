package sune.app.mediadown.media;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;

import sune.app.mediadown.util.Regex;

/** @since 00.02.05 */
public final class MediaMimeType {
	
	private static final Map<String, String> DEFAULT_MAP = Map.of();
	
	public static final MediaMimeType NONE = new MediaMimeType("", "", DEFAULT_MAP);
	public static final MediaMimeType DEFAULT = new MediaMimeType("application", "octet-stream", DEFAULT_MAP);
	
	private static final Regex REGEX;
	private static final Regex REGEX_PARAMETER;
	
	static {
		// See: https://datatracker.ietf.org/doc/html/rfc7230#section-3.2.6
		//    & https://datatracker.ietf.org/doc/html/rfc7231#section-3.1.1.1
		String rbase = Regex.quote("!#$%&'*+-.^_`|~") + "\\p{Alnum}";
		String token = "[" + rbase + "]+";
		String param = "[" + rbase + "=\\s\"]+";
		REGEX = Regex.of("^(" + token + ")/(" + token + ")((?:\\s*;" + param + ")*)$");
		REGEX_PARAMETER = Regex.of("^\\s*(" + token + ")\\s*=\\s*(" + token + "|\"" + token + "\")\\s*$");
	}
	
	private final String type;
	private final String subtype;
	private final Map<String, String> parameters;
	
	private MediaMimeType(String type, String subtype, Map<String, String> parameters) {
		this.type = Objects.requireNonNull(type);
		this.subtype = Objects.requireNonNull(subtype);
		this.parameters = unmodifiableMap(Objects.requireNonNull(parameters));
	}
	
	private static final Map<String, String> unmodifiableMap(Map<String, String> map) {
		return map == DEFAULT_MAP ? map : Collections.unmodifiableMap(map);
	}
	
	public static final MediaMimeType fromString(String string) {
		Matcher matcher = REGEX.matcher(Objects.requireNonNull(string));
		if(!matcher.matches()) return DEFAULT;
		
		String type = matcher.group(1);
		String subtype = matcher.group(2);
		String params = matcher.group(3);
		if(type.isBlank() || subtype.isBlank())
			return DEFAULT;
		
		type = type.toLowerCase();
		subtype = subtype.toLowerCase();
		
		Map<String, String> parameters;
		String[] paramsArray;
		if(!params.isBlank() && (paramsArray = params.split(";")).length > 0) {
			parameters = new LinkedHashMap<>();
			Matcher m;
			for(String param : paramsArray) {
				m = REGEX_PARAMETER.matcher(param);
				if(m.matches()) {
					String name = m.group(1).toLowerCase();
					String value = m.group(2).replaceAll("^\"?(.*?)\"?$", "$1");
					parameters.put(name, value);
				}
			}
		} else {
			parameters = Map.of();
		}
		
		return new MediaMimeType(type, subtype, parameters);
	}
	
	public String type() {
		return type;
	}
	
	public String subtype() {
		return subtype;
	}
	
	public String typeAndSubtype() {
		return type + '/' + subtype;
	}
	
	public Map<String, String> parameters() {
		return parameters;
	}
	
	public String parameter(String name) {
		return parameters.get(name);
	}
	
	public String parameter(String name, String defaultValue) {
		return parameters.getOrDefault(name, defaultValue);
	}
	
	public String charset() {
		return parameter("charset");
	}
	
	public List<String> codecs() {
		return List.of(parameter("codecs", "").split(","));
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(parameters, subtype, type);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MediaMimeType other = (MediaMimeType) obj;
		return Objects.equals(parameters, other.parameters)
		        && Objects.equals(subtype, other.subtype)
		        && Objects.equals(type, other.type);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(type).append('/').append(subtype);
		
		if(!parameters.isEmpty()) {
			for(Entry<String, String> param : parameters.entrySet()) {
				builder.append(';').append(param.getKey()).append('=').append(param.getValue());
			}
		}
		
		return builder.toString();
	}
}