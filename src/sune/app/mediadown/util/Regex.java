package sune.app.mediadown.util;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @since 00.02.08
 * @see Pattern
 */
public final class Regex {
	
	private static final int NO_LIMIT = 0;
	
	private final String pattern;
	private final int flags;
	private WeakReference<Pattern> ref;
	
	private Regex(String pattern, int flags) {
		this.pattern = pattern;
		this.flags = flags;
		this.ref = new WeakReference<>(null);
	}
	
	private static final Pattern compile(String pattern, int flags) {
		return Pattern.compile(pattern, flags);
	}
	
	public static final String quote(String string) {
		return Pattern.quote(string);
	}
	
	public static final Regex of(String pattern) {
		return new Regex(pattern, Flags.NONE);
	}
	
	public static final Regex of(String pattern, int flags) {
		return new Regex(pattern, flags);
	}
	
	/** @since 00.02.09 */
	private final void dispose(Matcher matcher) {
		Matchers.dispose(this, matcher);
	}
	
	public Matcher matcher(CharSequence input) {
		return Matchers.matcher(this).reset(input);
	}
	
	public String[] split(CharSequence input) {
		return split(input, NO_LIMIT);
	}
	
	public String[] split(CharSequence input, int limit) {
		return pattern().split(input, limit);
	}
	
	public Stream<String> splitAsStream(CharSequence input) {
		return pattern().splitAsStream(input);
	}
	
	public Predicate<String> asMatchPredicate() {
		return pattern().asMatchPredicate();
	}
	
	public Predicate<String> asPredicate() {
		return pattern().asPredicate();
	}
	
	public String replaceAll(CharSequence input, String replacement) {
		Matcher matcher = matcher(input);
		String result = matcher.replaceAll(replacement);
		dispose(matcher);
		return result;
	}
	
	public String replaceAll(CharSequence input, Function<MatchResult, String> replacer) {
		Matcher matcher = matcher(input);
		String result = matcher.replaceAll(replacer);
		dispose(matcher);
		return result;
	}
	
	public String replaceFirst(CharSequence input, String replacement) {
		Matcher matcher = matcher(input);
		String result = matcher.replaceFirst(replacement);
		dispose(matcher);
		return result;
	}
	
	public String replaceFirst(CharSequence input, Function<MatchResult, String> replacer) {
		Matcher matcher = matcher(input);
		String result = matcher.replaceFirst(replacer);
		dispose(matcher);
		return result;
	}
	
	/** @since 00.02.09 */
	public boolean matches(CharSequence input) {
		Matcher matcher = matcher(input);
		boolean result = matcher.matches();
		dispose(matcher);
		return result;
	}
	
	public Pattern pattern() {
		Pattern p;
		if((p = ref.get()) == null) {
			p = compile(pattern, flags);
			ref = new WeakReference<>(p);
		}
		
		return p;
	}
	
	public static final class Flags {
		
		public static final int NONE = 0;
		/**
		 * @see Pattern#UNIX_LINES
		 */
		public static final int UNIX_LINES = Pattern.UNIX_LINES;
		/**
		 * @see Pattern#CASE_INSENSITIVE
		 */
	    public static final int CASE_INSENSITIVE = Pattern.CASE_INSENSITIVE;
	    /**
		 * @see Pattern#COMMENTS
		 */
	    public static final int COMMENTS = Pattern.COMMENTS;
	    /**
		 * @see Pattern#MULTILINE
		 */
	    public static final int MULTILINE = Pattern.MULTILINE;
	    /**
		 * @see Pattern#LITERAL
		 */
	    public static final int LITERAL = Pattern.LITERAL;
	    /**
		 * @see Pattern#DOTALL
		 */
	    public static final int DOTALL = Pattern.DOTALL;
	    /**
		 * @see Pattern#UNICODE_CASE
		 */
	    public static final int UNICODE_CASE = Pattern.UNICODE_CASE;
	    /**
		 * @see Pattern#CANON_EQ
		 */
	    public static final int CANON_EQ = Pattern.CANON_EQ;
	    /**
		 * @see Pattern#UNICODE_CHARACTER_CLASS
		 */
	    public static final int UNICODE_CHARACTER_CLASS = Pattern.UNICODE_CHARACTER_CLASS;
		
		// Forbid anyone to create an instance of this class
		private Flags() {
		}
	}
	
	/** @since 00.02.09 */
	private static final class Matchers {
		
		private static final Object UNSET = new Object();
		private static final ThreadLocal<Map<Regex, Object>> POOLS = ThreadLocal.withInitial(WeakHashMap::new);
		
		private static final MatcherPool pool(Regex regex) {
			Map<Regex, Object> values = POOLS.get();
			Object value = values.getOrDefault(regex, UNSET);
			
			if(value == UNSET) {
				value = new MatcherPool(regex);
				values.put(regex, value);
			}
			
			return (MatcherPool) value;
		}
		
		public static final Matcher matcher(Regex regex) {
			return pool(regex).create();
		}
		
		public static final void dispose(Regex regex, Matcher matcher) {
			pool(regex).dispose(matcher);
		}
		
		private static final class MatcherPool {
			
			private final WeakReference<Regex> ref;
			private Map<Matcher, Boolean> matchers;
			
			public MatcherPool(Regex regex) {
				this.ref = new WeakReference<Regex>(regex);
			}
			
			private final Matcher emptyMatcher() {
				Regex regex;
				if((regex = ref.get()) == null) {
					return null;
				}
				
				return regex.pattern().matcher("");
			}
			
			public Matcher create() {
				if(matchers == null) {
					matchers = new WeakHashMap<>(4);
				}
				
				for(Entry<Matcher, Boolean> entry : matchers.entrySet()) {
					if(!entry.getValue()) {
						entry.setValue(true);
						return entry.getKey();
					}
				}
				
				Matcher matcher;
				if((matcher = emptyMatcher()) != null) {
					matchers.put(matcher, true);
				}
				
				return matcher;
			}
			
			public void dispose(Matcher matcher) {
				if(matchers == null || matcher == null) {
					return;
				}
				
				matchers.put(matcher, false);
			}
		}
	}
}