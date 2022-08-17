package sune.app.mediadown.util;

import java.util.ArrayList;
import java.util.List;

public final class CSSParser {
	
	private static enum CSSTokenType {
		SKIP, SELECTOR, PROPERTY_NAME, PROPERTY_VALUE, COMMENT;
	}
	
	private static final class CSSToken {
		
		private final CSSTokenType type;
		private final String value;
		
		public CSSToken(CSSTokenType type, String value) {
			if((type == null))
				throw new IllegalArgumentException();
			this.type = type;
			this.value = value;
		}
		
		public final CSSTokenType getType() {
			return type;
		}
		
		public final String getValue() {
			return value;
		}
	}
	
	private static final class CSSTokenizer {
		
		private static final int CHAR_RULE_OPEN  = '{';
		private static final int CHAR_RULE_CLOSE = '}';
		private static final int CHAR_NV_DELIM   = ':';
		private static final int CHAR_PROP_DELIM = ';';
		private static final int CHAR_COMMENT_1  = '/';
		private static final int CHAR_COMMENT_2  = '*';
		
		private static final CSSToken skipToken = new CSSToken(CSSTokenType.SKIP, null);
		
		private final String css;
		private final int len;
		private final StringBuilder builder = new StringBuilder();
		private int off;
		private int mark;
		private boolean inRule;
		
		public CSSTokenizer(String css) {
			if((css == null))
				throw new IllegalArgumentException();
			this.css = css;
			this.len = css.length();
		}
		
		private final boolean isTokenDelimiter(int c) {
			return (c == CHAR_RULE_OPEN
						|| c == CHAR_RULE_CLOSE
						|| c == CHAR_NV_DELIM
						|| c == CHAR_PROP_DELIM
						|| c == CHAR_COMMENT_1
						|| c == CHAR_COMMENT_2);
		}
		
		private final void addCharacter(int c) {
			boolean isSingleChar = Character.isBmpCodePoint(c);
			if((isSingleChar)) builder.append((char) c);
			else               builder.append(Character.toChars(c));
		}
		
		private final void mark() {
			mark = off;
		}
		
		private final void reset() {
			off = mark;
		}
		
		private final CSSTokenType getTokenType(int c) {
			switch(c) {
				case CHAR_RULE_OPEN:  return CSSTokenType.SELECTOR;
				case CHAR_RULE_CLOSE: return CSSTokenType.PROPERTY_VALUE;
				case CHAR_NV_DELIM:   return CSSTokenType.PROPERTY_NAME;
				case CHAR_PROP_DELIM: return CSSTokenType.PROPERTY_VALUE;
				case CHAR_COMMENT_1:  return CSSTokenType.COMMENT;
				case CHAR_COMMENT_2:  return CSSTokenType.SELECTOR;
			}
			return null;
		}
		
		private final CSSToken getToken(int c) {
			CSSTokenType type = getTokenType(c);
			if((c == CHAR_COMMENT_1 && off + 1 < len)) {
				mark();
				int a = css.codePointAt(off);
				int n = Character.charCount(a);
				off += n;
				if((a != CHAR_COMMENT_2)) {
					type = CSSTokenType.SKIP;
					addCharacter(c);
					reset();
				} else {
					// Loop through the whole comment's content
					for(; off < len; off += n) {
						a = css.codePointAt(off);
						n = Character.charCount(a);
						if((a == CHAR_COMMENT_2)) {
							mark();
							a = css.codePointAt(off += n);
							int j = Character.charCount(a);
							if((a == CHAR_COMMENT_1)) {
								off += j; break;
							}
							addCharacter(a);
							reset();
						} else {
							addCharacter(a);
						}
					}
				}
			}
			if(!inRule && type != CSSTokenType.COMMENT) {
				type = CSSTokenType.SELECTOR;
			}
			switch(c) {
				case CHAR_RULE_OPEN:  inRule = true;  break;
				case CHAR_RULE_CLOSE: inRule = false; break;
			}
			if((type == CSSTokenType.SELECTOR && c != CHAR_RULE_OPEN)) {
				addCharacter(c);
				// Loop through the whole selector's content
				for(int a, n; off < len; off += n) {
					a = css.codePointAt(off);
					n = Character.charCount(a);
					if((a == CHAR_RULE_OPEN)) {
						off -= n; break;
					}
					addCharacter(a);
				}
			}
			if((type == CSSTokenType.SKIP
					|| builder.length() == 0)) {
				return skipToken;
			}
			String value = builder.toString().trim();
			if((type != CSSTokenType.COMMENT))
				value = value.trim();
			builder.setLength(0);
			return !value.isEmpty() ? new CSSToken(type, value) : skipToken;
		}
		
		public final CSSToken nextToken() {
			for(int c, n; off < len; off += n) {
				c = css.codePointAt(off);
				n = Character.charCount(c);
				if((isTokenDelimiter(c))) {
					off += n;
					return getToken(c);
				} else {
					addCharacter(c);
				}
			}
			return null;
		}
	}
	
	public static final class CSSProperty {
		
		private final String name;
		private final String value;
		
		public CSSProperty(String name, String value) {
			if((name == null || value == null))
				throw new IllegalArgumentException();
			this.name = name;
			this.value = value;
		}
		
		public final String getName() {
			return name;
		}
		
		public final String getValue() {
			return value;
		}
		
		@Override
		public final String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(name);
			sb.append((char) CSSTokenizer.CHAR_NV_DELIM);
			sb.append(' ');
			sb.append(value);
			return sb.toString();
		}
	}
	
	public static final class CSSRule {
		
		private final String selector;
		private final List<CSSProperty> properties;
		
		public CSSRule(String selector, List<CSSProperty> properties) {
			if((selector == null || properties == null))
				throw new IllegalArgumentException();
			this.selector = selector;
			this.properties = properties;
		}
		
		public final String getSelector() {
			return selector;
		}
		
		public final List<CSSProperty> getProperties() {
			return properties;
		}
		
		@Override
		public final String toString() {
			String newLine = "\n"; // Always use Unix-like new line
			String tabChar = "\t";
			StringBuilder sb = new StringBuilder();
			sb.append(selector);
			sb.append(' ');
			sb.append((char) CSSTokenizer.CHAR_RULE_OPEN);
			sb.append(newLine);
			for(CSSProperty property : properties) {
				sb.append(tabChar);
				sb.append(property.toString());
				sb.append((char) CSSTokenizer.CHAR_PROP_DELIM);
				sb.append(newLine);
			}
			sb.append((char) CSSTokenizer.CHAR_RULE_CLOSE);
			return sb.toString();
		}
	}
	
	public static final class CSS {
		
		private final List<CSSRule> rules;
		
		public CSS(List<CSSRule> rules) {
			if((rules == null))
				throw new IllegalArgumentException();
			this.rules = rules;
		}
		
		public final List<CSSRule> getRules() {
			return rules;
		}
		
		@Override
		public final String toString() {
			String newLine = "\n"; // Always use Unix-like new line
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for(CSSRule rule : rules) {
				if((first)) {
					first = false;
				} else {
					sb.append(newLine);
					sb.append(newLine);
				}
				sb.append(rule.toString());
			}
			return sb.toString();
		}
	}
	
	// Forbid anyone to create an instance of this class
	private CSSParser() {
	}
	
	public static final CSS parse(String css) {
		if((css == null))
			throw new IllegalArgumentException();
		List<CSSRule> rules = new ArrayList<>();
		CSSRule lastRule = null;
		String lastPropertyName = null;
		CSSTokenizer tokenizer = new CSSTokenizer(css);
		for(CSSToken token; (token = tokenizer.nextToken()) != null;) {
			String value = token.getValue();
			switch(token.getType()) {
				case SKIP:
				case COMMENT:
					continue;
				case SELECTOR:
					lastRule = new CSSRule(value, new ArrayList<>());
					rules.add(lastRule);
					break;
				case PROPERTY_NAME:
					lastPropertyName = value;
					break;
				case PROPERTY_VALUE:
					lastRule.getProperties().add(new CSSProperty(lastPropertyName, value));
					break;
			}
		}
		return new CSS(rules);
	}
}