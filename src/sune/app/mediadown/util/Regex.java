package sune.app.mediadown.util;

import java.lang.ref.WeakReference;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.util.Callback;

/**
 * @since 00.02.08
 * @see Pattern
 */
public final class Regex {
	
	private static final int NO_LIMIT = 0;
	
	private final String pattern;
	private final int flags;
	private WeakReference<Pattern> ref;
	
	private Regex(Pattern pattern) {
		this.pattern = pattern.pattern();
		this.flags = pattern.flags();
		this.ref = new WeakReference<>(pattern);
	}
	
	private static final Pattern compile(String pattern, int flags) {
		return Pattern.compile(pattern, flags);
	}
	
	public static final String quote(String string) {
		return Pattern.quote(string);
	}
	
	public static final boolean matches(String pattern, CharSequence input) {
		return Pattern.matches(pattern, input);
	}
	
	public static final Regex of(String pattern) {
		return new Regex(compile(pattern, Flags.NONE));
	}
	
	public static final Regex of(String pattern, int flags) {
		return new Regex(compile(pattern, flags));
	}
	
	public Matcher matcher(CharSequence input) {
		return pattern().matcher(input);
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
	
	public String replaceAll(CharSequence input, Callback<MatchResult, String> callback) {
		StringBuilder str = new StringBuilder(input);
		Matcher matcher = matcher(str);
		
		while(matcher.find()) {
			MatchResult result = matcher.toMatchResult();
			int start = result.start(), end = result.end();
			String replacement = callback.call(result);
			str.replace(start, end, replacement);
			matcher.reset(str);
			matcher.region(start + replacement.length(), str.length());
		}
		
		return str.toString();
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
}