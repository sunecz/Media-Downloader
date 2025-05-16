package sune.app.mediadown.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import sune.app.mediadown.os.OS;

/** @since 00.02.09 */
public final class L10N {
	
	private static final CharsetEncoder ASCII_ENCODER = StandardCharsets.US_ASCII.newEncoder();
	
	// Forbid anyone to create an instance of this class
	private L10N() {
	}
	
	private static final String normalize(String string) {
		Objects.requireNonNull(string);
		return Normalizer.normalize(string, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
	}
	
	private static final boolean isOnlyAscii(String string) {
		return ASCII_ENCODER.canEncode(string);
	}
	
	private static final MDL parseResource(String path) throws IOException {
		Objects.requireNonNull(path);
		
		try(InputStream is = L10N.class.getResourceAsStream(path)) {
			return parseMDL(is);
		}
	}
	
	public static final MDL parseMDL(InputStream is) throws IOException {
		Objects.requireNonNull(is);
		
		try(Parser parser = new Parser(is)) {
			return parser.parse();
		}
	}
	
	public static final String messageOf(String message) {
		return message != null ? normalize(message) : null;
	}
	
	public static final String messageOf(Throwable throwable) {
		String message = Objects.requireNonNull(throwable).getMessage();
		return message != null ? normalize(message) : null;
	}
	
	public static final class Errors {
		
		private static final String OS_NEUTRAL = "any";
		private static final String LOCALE_NEUTRAL = "en";
		
		private static final Errors INSTANCE_OS = new Errors(currentOS(), currentLocale());
		private static final Errors INSTANCE_NEUTRAL = new Errors();
		
		private static final MDL mdl;
		
		static {
			MDL _mdl;
			try {
				_mdl = parseResource("/resources/internal/l10n-errors.mdlf");
			} catch(IOException ex) {
				throw new IllegalStateException(ex);
			}
			
			mdl = _mdl;
		}
		
		private final String os;
		private final String locale;
		
		private Errors() {
			this.os = OS_NEUTRAL;
			this.locale = LOCALE_NEUTRAL;
		}
		
		private Errors(String os, String locale) {
			this.os = Objects.requireNonNull(os);
			this.locale = Objects.requireNonNull(locale);
		}
		
		private static final String currentOS() {
			return OS.name();
		}
		
		private static final String currentLocale() {
			return Locale.getDefault().getLanguage();
		}
		
		public static final Errors ofOS() {
			return INSTANCE_OS;
		}
		
		public static final Errors ofNeutral() {
			return INSTANCE_NEUTRAL;
		}
		
		public MDL.Strings errorsOf(String name) {
			Objects.requireNonNull(name);
			MDL.Block block = mdl.block(String.format("%s:%s", os, name));
			return block != null ? block.strings(locale) : null;
		}
		
		public boolean anyEquals(String message, String name) {
			Objects.requireNonNull(message);
			MDL.Strings strings = errorsOf(name);
			return strings != null && strings.anyEquals(message);
		}
		
		public boolean anyContains(String message, String name) {
			Objects.requireNonNull(message);
			MDL.Strings strings = errorsOf(name);
			return strings != null && strings.anyContains(message);
		}
	}
	
	public static final class MDL {
		
		private final Map<String, Block> blocks;
		
		private MDL(Map<String, Block> blocks) {
			this.blocks = Objects.requireNonNull(blocks);
		}
		
		public Block block(String name) {
			return blocks.get(Objects.requireNonNull(name));
		}
		
		public static final class Block {
			
			private final Map<String, String> attributes;
			private final Map<String, Strings> strings;
			
			private Block(Map<String, String> attributes, Map<String, Strings> strings) {
				this.attributes = Objects.requireNonNull(attributes);
				this.strings = Objects.requireNonNull(strings);
			}
			
			public String name() {
				return attribute(Attribute.NAME);
			}
			
			public String fallback() {
				return attribute(Attribute.FALLBACK);
			}
			
			public String attribute(String name) {
				return attributes.get(Objects.requireNonNull(name));
			}
			
			public Strings strings(String locale) {
				Strings value = strings.get(Objects.requireNonNull(locale));
				return value != null ? value : strings.get(fallback());
			}
			
			private static final class Builder {
				
				private final Map<String, String> attributes = new HashMap<>();
				private final Map<String, Strings.Builder> strings = new HashMap<>();
				
				public void addAttribute(String name, String value) {
					Objects.requireNonNull(name);
					Objects.requireNonNull(value);
					attributes.putIfAbsent(name, value); // Keep the first attribute of a name
				}
				
				public void addString(String locale, String string) {
					Objects.requireNonNull(locale);
					Objects.requireNonNull(string);
					
					strings.compute(locale, (l, builder) -> {
						if(builder == null) {
							builder = new Strings.Builder();
						}
						
						builder.addString(string);
						return builder;
					});
				}
				
				public String attribute(String name) {
					return attributes.get(Objects.requireNonNull(name));
				}
				
				public Block build() {
					return new Block(
						Map.copyOf(attributes),
						strings.entrySet().stream()
							.map((e) -> Map.entry(e.getKey(), e.getValue().build()))
							.collect(Collectors.toUnmodifiableMap(
								Map.Entry::getKey,
								Map.Entry::getValue
							))
					);
				}
				
				public void reset() {
					attributes.clear();
					strings.clear();
				}
			}
			
			public static final class Attribute {
				
				public static final String NAME = "name";
				public static final String FALLBACK = "fallback";
				
				private Attribute() {
				}
			}
		}
		
		public static final class Strings {
			
			private final List<String> strings;
			
			private Strings(List<String> strings) {
				this.strings = Objects.requireNonNull(strings);
			}
			
			public boolean anyEquals(String string) {
				Objects.requireNonNull(string);
				return strings.stream().anyMatch(string::equals);
			}
			
			public boolean anyContains(String string) {
				Objects.requireNonNull(string);
				return strings.stream().anyMatch(string::contains);
			}
			
			public List<String> strings() {
				return Collections.unmodifiableList(strings);
			}
			
			private static final class Builder {
				
				private final List<String> strings = new ArrayList<>();
				
				public void addString(String string) {
					Objects.requireNonNull(string);
					strings.add(string);
				}
				
				public Strings build() {
					return new Strings(List.copyOf(strings));
				}
			}
		}
		
		private static final class Builder {
			
			private final Map<String, Block> blocks = new HashMap<>();
			
			public void addBlock(String name, Block block) {
				Objects.requireNonNull(name);
				Objects.requireNonNull(block);
				blocks.putIfAbsent(name, block); // Keep the first block of a name
			}
			
			public MDL build() {
				return new MDL(Map.copyOf(blocks));
			}
		}
	}
	
	public static final class Parser implements AutoCloseable {
		
		/* > INTRODUCTION
		 * 
		 * MDLF (Media Downloader Localization Format) is a simple UTF-8 encoded text format
		 * used to encode localization strings.
		 * 
		 * > USAGE
		 * 
		 * The format targets the use case of matching a pair of name and locale to a message.
		 * More specifically, Java may throw an exception that has locale-specific message,
		 * otherwise known as a localized message. Mostly, these messages are returned
		 * by the operating system directly and are dependent on the OS locale, thus the English
		 * version of that message may not and often is not available.
		 * 
		 * Since there is no additional information provided by the thrown exception, apart from
		 * the stack trace, that does not disclose any other information in the context of,
		 * for example, an OS call; and the localized message, we have to depend on the localized
		 * message itself.
		 * 
		 * In most cases, such as when having specialized exceptions available, the message itself
		 * is not important, but in the case of IOException which is used as a catch-all exception
		 * for an IO operation, to catch only a particular underlying error the need to check
		 * the (possibly localized) exception's message is required.
		 * 
		 * > SYNTAX
		 * 
		 * File consits of zero or more blocks. Each block is annotated with a meta line. Each
		 * meta line is denoted by the '#' symbol. Meta line contains attributes in the format
		 * of an attribute name, followed by the '=' symbol, followed by the attribute value.
		 * Multiple attributes are separated by the ',' symbol.
		 * 
		 * Block consists of zero or more content lines. Each content line contains the locale
		 * name, followed by '|' symbol, followed by the locale-specific string.
		 * 
		 * Meta line and content line locale may contain only ASCII characters. A content line
		 * string may contain any Unicode character, it is always normalized to the NFC form
		 * and then converted to lower case so that it is ready for matching.
		 * 
		 * > SEMANTICS
		 * 
		 * Each meta line must contain the following attributes: name, the name of the block;
		 * fallback, the locale to fallback on, when a requested locale mapping is not found.
		 */
		
		private static final Charset CHARSET = StandardCharsets.UTF_8;
		private static final int CHAR_META_LINE = '#';
		private static final int CHAR_SEPARATOR_ATTRIBUTE = ',';
		private static final int CHAR_SEPARATOR_KEY_VALUE = '=';
		private static final int CHAR_SEPARATOR_STRING = '|';
		
		private static final int[][] ATTRIBUTES_DFA = {
		//     = | , | OTHER
			{  1,  2,  0, }, // [S] name
			{  2,  0,  1, }, // [F] value
			{  2,  2,  2, }, // [E] error
		};
		
		private final InputStream is;
		private MDL.Builder builder;
		private String currentBlockName;
		private MDL.Block.Builder currentBlock;
		
		private Parser(InputStream is) {
			this.is = Objects.requireNonNull(is);
		}
		
		private final void finishPendingBlock() {
			if(currentBlockName == null || currentBlock == null) {
				return; // Ignore invalid or empty block
			}
			
			builder.addBlock(currentBlockName, currentBlock.build());
		}
		
		private final void parseAttributes(String line) {
			if(line.isEmpty()) {
				throw new IllegalStateException("Invalid meta line: empty attributes");
			}
			
			String name = null, value = null;
			int p = 0;
			
			for(int i = 0, l = line.length(), state = 0, c; i < l; ++i) {
				switch(line.charAt(i)) {
					case CHAR_SEPARATOR_KEY_VALUE: c = 0; break;
					case CHAR_SEPARATOR_ATTRIBUTE: c = 1; break;
					default:                       c = 2; break;
				}
				
				switch(state) {
					case 0: {
						if(c == 0) {
							name = line.substring(p, i);
							p = i + 1;
						}
						break;
					}
					case 1: {
						if(c == 1) {
							value = line.substring(p, i);
							currentBlock.addAttribute(name, value);
							name = value = null;
							p = i + 1;
						}
						break;
					}
					default: { // Error
						throw new IllegalStateException("Invalid meta line: invalid syntax");
					}
				}
				
				state = ATTRIBUTES_DFA[state][c];
			}
			
			if(name == null) {
				throw new IllegalStateException("Invalid meta line: invalid syntax");
			}
			
			value = line.substring(p);
			currentBlock.addAttribute(name, value);
			name = value = null;
		}
		
		private final void checkBlockName(String name) {
			if(name == null || name.isEmpty()) {
				throw new IllegalStateException("Invalid meta line: empty name");
			}
			
			if(!isOnlyAscii(name)) {
				throw new IllegalStateException("Invalid meta line: non-ASCII name");
			}
		}
		
		private final void checkBlockFallback(String fallback) {
			if(fallback == null || fallback.isEmpty()) {
				throw new IllegalStateException("Invalid meta line: empty fallback");
			}
			
			if(!isOnlyAscii(fallback)) {
				throw new IllegalStateException("Invalid meta line: non-ASCII fallback");
			}
		}
		
		private final void checkStringLocale(String locale) {
			if(locale == null || locale.isEmpty()) {
				throw new IllegalStateException("Invalid content line: empty locale");
			}
			
			if(!isOnlyAscii(locale)) {
				throw new IllegalStateException("Invalid content line: non-ASCII locale");
			}
		}
		
		private final void parseMetaLine(String line) {
			finishPendingBlock();
			
			if(currentBlock == null) {
				currentBlock = new MDL.Block.Builder();
			} else {
				currentBlock.reset();
			}
			
			parseAttributes(line.substring(1));
			
			String name = currentBlock.attribute(MDL.Block.Attribute.NAME);
			checkBlockName(name);
			
			String fallback = currentBlock.attribute(MDL.Block.Attribute.FALLBACK);
			checkBlockFallback(fallback);
			
			currentBlockName = name;
		}
		
		private final void parseContentLine(String line) {
			if(currentBlockName == null || currentBlock == null) {
				throw new IllegalStateException("Meta line is missing");
			}
			
			int index = line.indexOf(CHAR_SEPARATOR_STRING);
			
			if(index < 0) {
				throw new IllegalStateException("Invalid content line: no separator symbol");
			}
			
			String locale = line.substring(0, index).strip();
			checkStringLocale(locale);
			
			String string = normalize(line.substring(index + 1));
			currentBlock.addString(locale, string);
		}
		
		private final void parseLine(String line) {
			if(line.isBlank()) {
				return; // Ignore empty lines
			}
			
			if(line.charAt(0) == CHAR_META_LINE) {
				parseMetaLine(line);
				return;
			}
			
			parseContentLine(line);
		}
		
		public MDL parse() throws IOException {
			builder = new MDL.Builder();
			
			try(BufferedReader reader = new BufferedReader(new InputStreamReader(is, CHARSET))) {
				for(String line; (line = reader.readLine()) != null;) {
					parseLine(line);
				}
			}
			
			finishPendingBlock();
			return builder.build();
		}
		
		@Override
		public void close() throws IOException {
			is.close();
		}
	}
}
