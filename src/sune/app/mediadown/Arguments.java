package sune.app.mediadown;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** @since 00.02.02 */
public final class Arguments {
	
	private static final Pattern PATTERN_ARG = Pattern.compile("^--?(?<name>[A-Za-z0-9_\\-]+)(?:=(?<value>.*))?$");
	
	private final Map<String, Argument> arguments;
	private final String[] args;
	
	private Arguments(Map<String, Argument> arguments, String[] args) {
		this.arguments = Objects.requireNonNull(arguments);
		this.args = args;
	}
	
	public Arguments(List<Argument> arguments) {
		this(arguments.stream()
		        .map((a) -> Map.entry(a.name(), a))
		        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
		                                  (a, b) -> a, () -> new LinkedHashMap<>())),
		     null);
	}
	
	public static final Arguments parse(String[] args) {
		Map<String, Argument> arguments = new LinkedHashMap<>();
		Matcher matcher; String lastName = null;
		for(String arg : args) {
			if((matcher = PATTERN_ARG.matcher(arg)).matches()) {
				String name = matcher.group("name");
				String value = matcher.group("value");
				// Add any leftover argument with no value
				if(lastName != null) {
					arguments.put(lastName, new Argument(lastName, null));
					lastName = null;
				}
				if(value != null) {
					arguments.put(name, new Argument(name, value));
				} else {
					lastName = name;
				}
			} else if(lastName != null) {
				arguments.put(lastName, new Argument(lastName, arg));
				lastName = null;
			}
		}
		// Add any leftover argument with no value
		if(lastName != null) {
			arguments.put(lastName, new Argument(lastName, null));
			lastName = null;
		}
		return new Arguments(arguments, args);
	}
	
	public boolean has(String name) {
		return arguments.containsKey(name);
	}
	
	public Argument get(String name) {
		return arguments.get(name);
	}
	
	public String getValue(String name) {
		return getValue(name, null);
	}
	
	public String getValue(String name, String defaultValue) {
		Argument arg = get(name);
		return arg != null ? arg.value() : defaultValue;
	}
	
	public List<Argument> all() {
		return List.copyOf(arguments.values());
	}
	
	public String[] args() {
		return args;
	}
}