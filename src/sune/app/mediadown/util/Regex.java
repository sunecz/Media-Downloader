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
	
	/** @since 00.02.09 */
	public static final String quoteReplacement(String string) {
		return Matcher.quoteReplacement(string);
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
	
	/** @since 00.02.09 */
	public Matcher matcher() {
		return Matchers.matcher(this);
	}
	
	public Matcher matcher(CharSequence input) {
		return matcher().reset(input);
	}
	
	/** @since 00.02.09 */
	public ReusableMatcher reusableMatcher() {
		return new ReusableMatcher(this);
	}
	
	/** @since 00.02.09 */
	public ReusableMatcher reusableMatcher(CharSequence input) {
		return reusableMatcher().reset(input);
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
	public static final class ReusableMatcher implements MatchResult {
		
		private WeakReference<Regex> ref;
		private Matcher matcher;
		
		private ReusableMatcher(Regex regex) {
			this.ref = new WeakReference<>(regex);
		}
		
		private final Matcher matcher() {
			if(matcher == null) {
				Regex regex;
				if((regex = ref.get()) != null) {
					matcher = regex.matcher();
				}
			}
			
			return matcher;
		}
		
		public void dispose() {
			if(matcher != null) {
				Regex regex;
				if((regex = ref.get()) != null) {
					regex.dispose(matcher);
				}
			}
			
			matcher = null;
		}
		
		@Override public int start() { return matcher().start(); }
		@Override public int start(int group) { return matcher().start(group); }
		@Override public int end() { return matcher().end(); }
		@Override public int end(int group) { return matcher().end(group); }
		@Override public String group() { return matcher().group(); }
		@Override public String group(int group) { return matcher().group(group); }
		@Override public int groupCount() { return matcher().groupCount(); }
		
		public Pattern pattern() { return matcher().pattern(); }
		public MatchResult toMatchResult() { return matcher().toMatchResult(); }
		public ReusableMatcher reset() { matcher().reset(); return this; }
		public ReusableMatcher reset(CharSequence input) { matcher().reset(input); return this; }
		public int start(String name) { return matcher().start(name); }
		public int end(String name) { return matcher().end(name); }
		public String group(String name) { return matcher().group(name); }
		public boolean matches() { return matcher().matches(); }
		public boolean find() { return matcher().find(); }
		public boolean find(int start) { return matcher().find(start); }
		public boolean lookingAt() { return matcher().lookingAt(); }
		public ReusableMatcher appendReplacement(StringBuffer sb, String replacement) { matcher().appendReplacement(sb, replacement); return this; }
		public ReusableMatcher appendReplacement(StringBuilder sb, String replacement) { matcher().appendReplacement(sb, replacement); return this; }
		public StringBuffer appendTail(StringBuffer sb) { return matcher().appendTail(sb); }
		public StringBuilder appendTail(StringBuilder sb) { return matcher().appendTail(sb); }
		public String replaceAll(String replacement) { return matcher().replaceAll(replacement); }
		public String replaceAll(Function<MatchResult, String> replacer) { return matcher().replaceAll(replacer); }
		public Stream<MatchResult> results() { return matcher().results(); }
		public String replaceFirst(String replacement) { return matcher().replaceFirst(replacement); }
		public String replaceFirst(Function<MatchResult, String> replacer) { return matcher().replaceFirst(replacer); }
		public ReusableMatcher region(int start, int end) { matcher().region(start, end); return this; }
		public int regionStart() { return matcher().regionStart(); }
		public int regionEnd() { return matcher().regionEnd(); }
		public boolean hasTransparentBounds() { return matcher().hasTransparentBounds(); }
		public ReusableMatcher useTransparentBounds(boolean b) { matcher().useTransparentBounds(b); return this; }
		public boolean hasAnchoringBounds() { return matcher().hasAnchoringBounds(); }
		public ReusableMatcher useAnchoringBounds(boolean b) { matcher().useAnchoringBounds(b); return this; }
		public String toString() { return matcher().toString(); }
		public boolean hitEnd() { return matcher().hitEnd(); }
		public boolean requireEnd() { return matcher().requireEnd(); }
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