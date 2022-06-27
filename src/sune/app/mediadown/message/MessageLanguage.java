package sune.app.mediadown.message;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import sune.app.mediadown.language.Language;
import sune.app.mediadown.resource.ResourceRegistry;

/** @since 00.02.05 */
public final class MessageLanguage {
	
	private static final Set<String> setNone = Set.of();
	private static final Set<String> setAll = setOfAll();
	
	private static MessageLanguage NONE;
	private static MessageLanguage ALL;
	
	private final Set<String> values;
	
	private MessageLanguage(Set<String> values) {
		this.values = Objects.requireNonNull(values);
	}
	
	private static final Set<String> setOfAll() {
		return ResourceRegistry.languages.allValues().stream()
					.map(Language::getCode)
					.collect(Collectors.toUnmodifiableSet());
	}
	
	public static final MessageLanguage none() {
		return NONE == null ? (NONE = new MessageLanguage(setNone)) : NONE;
	}
	
	public static final MessageLanguage all() {
		return ALL == null ? (ALL = new MessageLanguage(setAll)) : ALL;
	}
	
	public static final MessageLanguage of(String... values) {
		Objects.requireNonNull(values);
		if(values.length == 0) return none();
		Set<String> set = Set.of(values);
		if(set.equals(setAll)) return all();
		return new MessageLanguage(set);
	}
	
	public boolean has(String language) {
		return values.contains(language);
	}
	
	public Set<String> values() {
		return values;
	}
	
	public boolean isNone() {
		return values == setNone;
	}
	
	public boolean isAll() {
		return values == setAll;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(values);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MessageLanguage other = (MessageLanguage) obj;
		return Objects.equals(values, other.values);
	}
}