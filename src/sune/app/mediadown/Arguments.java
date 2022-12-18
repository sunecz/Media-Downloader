package sune.app.mediadown;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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
	
	/** @since 00.02.08 */
	public <T> T value(String name, Function<String, T> converter) {
		return value(name, converter);
	}
	
	/** @since 00.02.08 */
	public <T> T value(String name, Function<String, T> converter, T defaultValue) {
		Argument arg = get(name);
		return arg != null ? converter.apply(arg.value()) : defaultValue;
	}
	
	/** @since 00.02.08 */
	public boolean booleanValue(String name) {
		return booleanValue(name, false);
	}
	
	/** @since 00.02.08 */
	public boolean booleanValue(String name, boolean defaultValue) {
		return value(name, Boolean::valueOf, defaultValue);
	}
	
	/** @since 00.02.08 */
	public byte byteValue(String name) {
		return byteValue(name, (byte) 0);
	}
	
	/** @since 00.02.08 */
	public byte byteValue(String name, byte defaultValue) {
		return value(name, Byte::valueOf, defaultValue);
	}
	
	/** @since 00.02.08 */
	public char charValue(String name) {
		return charValue(name, (char) 0);
	}
	
	/** @since 00.02.08 */
	public char charValue(String name, char defaultValue) {
		return (char) shortValue(name, (short) defaultValue);
	}
	
	/** @since 00.02.08 */
	public short shortValue(String name) {
		return shortValue(name, (short) 0);
	}
	
	/** @since 00.02.08 */
	public short shortValue(String name, short defaultValue) {
		return value(name, Short::valueOf, defaultValue);
	}
	
	/** @since 00.02.08 */
	public int intValue(String name) {
		return intValue(name, 0);
	}
	
	/** @since 00.02.08 */
	public int intValue(String name, int defaultValue) {
		return value(name, Integer::valueOf, defaultValue);
	}
	
	/** @since 00.02.08 */
	public long longValue(String name) {
		return longValue(name, 0L);
	}
	
	/** @since 00.02.08 */
	public long longValue(String name, long defaultValue) {
		return value(name, Long::valueOf, defaultValue);
	}
	
	/** @since 00.02.08 */
	public float floatValue(String name) {
		return floatValue(name, 0.0f);
	}
	
	/** @since 00.02.08 */
	public float floatValue(String name, float defaultValue) {
		return value(name, Float::valueOf, defaultValue);
	}
	
	/** @since 00.02.08 */
	public double doubleValue(String name) {
		return doubleValue(name, 0.0);
	}
	
	/** @since 00.02.08 */
	public double doubleValue(String name, double defaultValue) {
		return value(name, Double::valueOf, defaultValue);
	}
	
	/** @since 00.02.08 */
	public String stringValue(String name) {
		return stringValue(name, null);
	}
	
	/** @since 00.02.08 */
	public String stringValue(String name, String defaultValue) {
		return getValue(name, defaultValue);
	}
	
	public List<Argument> all() {
		return List.copyOf(arguments.values());
	}
	
	/** @since 00.02.08 */
	public List<String> allNames() {
		return List.copyOf(arguments.keySet());
	}
	
	public String[] args() {
		return Arrays.copyOf(args, args.length);
	}
	
	/** @since 00.02.08 */
	public List<String> argsList() {
		return List.of(args);
	}
}