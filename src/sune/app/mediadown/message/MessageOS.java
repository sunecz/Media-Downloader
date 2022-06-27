package sune.app.mediadown.message;

import java.util.Objects;
import java.util.Set;

import sune.app.mediadown.util.OSUtils;

/** @since 00.02.05 */
public final class MessageOS {
	
	private static final Set<String> setNone = Set.of();
	private static final Set<String> setAll = Set.of(
		OSUtils.OS_NAME_WINDOWS,
		OSUtils.OS_NAME_UNIX,
		OSUtils.OS_NAME_MACOS
	);
	
	private static MessageOS NONE;
	private static MessageOS ALL;
	
	private final Set<String> values;
	
	private MessageOS(Set<String> values) {
		this.values = Objects.requireNonNull(values);
	}
	
	public static final MessageOS none() {
		return NONE == null ? (NONE = new MessageOS(setNone)) : NONE;
	}
	
	public static final MessageOS all() {
		return ALL == null ? (ALL = new MessageOS(setAll)) : ALL;
	}
	
	public static final MessageOS of(String... values) {
		Objects.requireNonNull(values);
		if(values.length == 0) return none();
		Set<String> set = Set.of(values);
		if(set.equals(setAll)) return all();
		return new MessageOS(set);
	}
	
	public boolean has(String os) {
		return values.contains(os);
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
		MessageOS other = (MessageOS) obj;
		return Objects.equals(values, other.values);
	}
}