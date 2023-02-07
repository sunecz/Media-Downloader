package sune.app.mediadown.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDF;

/**
 * Class containing some useful JavaScript-related functions.
 * @see
 * <a href="https://sangupta.com/tech/encodeuricomponent-and.html">
 * 	https://sangupta.com/tech/encodeuricomponent-and.html
 * </a>
 * @author sangupta
 * @author Sune*/
public final class JavaScript {
	
	private static final class StringJoiner {
		
		static final StringBuilder JOINER;
		static {
			JOINER = new StringBuilder();
		}
		
		public static final void append(String text) {
			JOINER.append(text);
		}
		
		public static final void reset() {
			JOINER.setLength(0);
		}
		
		public static final String content() {
			return JOINER.toString();
		}
	}
	
	private static final Charset CHARSET      = StandardCharsets.UTF_8;
	private static final String  CHARS_ENCODE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";
	private static ScriptEngine ENGINE;
	
	private static final ScriptEngine getEngine() {
		if((ENGINE == null)) {
			ENGINE = new ScriptEngineManager().getEngineByName("JavaScript");
		}
		return ENGINE;
	}
	
	private static final String fileContent(String path) throws IOException {
		return new String(JavaScript.class.getResourceAsStream("/resources/util/" + path).readAllBytes());
	}
	
	public static final Object execute(String js) throws ScriptException {
		return getEngine().eval(js);
	}
	
	public static final Object execute(String jsFile, String js) throws IOException, ScriptException {
		StringJoiner.reset();
		StringJoiner.append(fileContent(jsFile));
		StringJoiner.append(js);
		return execute(StringJoiner.content());
	}
	
	public static final String encodeURIComponent(String s) {
		if((s == null || s.isEmpty())) return s;
		int l = s.length();
		StringBuilder b = new StringBuilder(l * 3);
		for(int i = 0; i < l; ++i) {
			String c = s.substring(i, i + 1);
			if((CHARS_ENCODE.indexOf(c) == -1)) b.append(getHex(c.getBytes(CHARSET)));
			else                                b.append(c);
		}
		return b.toString();
	}
	
	public static final String decodeURIComponent(String s) {
		StringBuilder b = new StringBuilder();
		for(int i = 0, k = -1, m = s.length(), p = 0, f = 0, c; i < m; ++i) {
			c = s.charAt(i);
			switch(c) {
				case '%':
					int h = (Character.isDigit(c = s.charAt(++i)) ? c - '0' : 10 + Character.toLowerCase(c) - 'a') & 0xf;
					int l = (Character.isDigit(c = s.charAt(++i)) ? c - '0' : 10 + Character.toLowerCase(c) - 'a') & 0xf;
					p = (h << 4) | l;
					break;
				case '+':
					p = ' ';
					break;
				default:
					p = c;
					break;
			}
			// 10xxxxxx
			if((p & 0xc0) == 0x80) {
				f = (f << 6) | (p & 0x3f);
				if((--k == 0))
					b.append((char) f);
			} else
			// 0xxxxxxx
			if((p & 0x80) == 0x00) {
				b.append((char) p);
			} else
			// 110xxxxx
			if((p & 0xe0) == 0xc0) {
				f = p & 0x1f;
				k = 1;
			} else
			// 1110xxxx
			if((p & 0xf0) == 0xe0) {
				f = p & 0x0f;
				k = 2;
			} else
			// 11110xxx
			if((p & 0xf8) == 0xf0) { 
				f = p & 0x07;
				k = 3;
			} else
			// 111110xx
			if((p & 0xfc) == 0xf8) {
				f = p & 0x03;
				k = 4;
			}
			// 1111110x
			else { 
				f = p & 0x01;
				k = 5;
			}
		}
		return b.toString();
	}
	
	private static final String getHex(byte[] a) {
		StringBuilder b = new StringBuilder(a.length * 3);
		for(int i = 0, l = a.length, c; i < l; ++i) {
			c = a[i] & 0xff;
			b.append("%");
			if((c < 0x10)) {
				b.append("0");
			}
			b.append(Long.toString(c, 16).toUpperCase());
		}
		return b.toString();
	}
	
	// Same as StringEscapeUtils for ECMAScript
	public static final String escape(String string) {
		StringBuilder sb = new StringBuilder();
		char[] chars 	 = string.toCharArray();
		for(int i = 0, l = chars.length; i < l; ++i) {
			char c = chars[i];
			if(c == '\'' || c == '"'  || c == '\\' ||
			   c == '/'  || c == '\b' || c == '\n' ||
			   c == '\t' || c == '\f' || c == '\r') {
				sb.append('\\');
			} else if(c < 32 || c > 127) {
				sb.append("\\u");
				sb.append(intToHex(c));
				continue;
			}
			sb.append(c);
		}
		return sb.toString();
	}
	
	public static final String intToHex(int v) {
		String s = Integer.toHexString(v);
		return ("0000".substring(0, 4-s.length()) + s).toUpperCase();
	}
	
	public static final String decodeXChars(String string) {
		String[] array = string.replace("\\", "")
							   .split("x");
		StringBuilder sb = new StringBuilder();
		for(int i = 1, l = array.length; i < l; ++i) {
		    int hex = Integer.parseInt(array[i], 16);
		    sb.append((char) hex);
		}
		return sb.toString();
	}
	
	public static final String varcontent(String script, String name) {
		StringBuilder sb = new StringBuilder();
		Regex pat = Regex.of("(?:var\\s+)?" + Regex.quote(name) + "\\s*=");
		Matcher mat = pat.matcher(script);
		
		if(mat.find()) {
			int start = mat.end();
			// Find the end of the var content (by finding ';', or script end)
			boolean dq = false;
			boolean sq = false;
			boolean wq = false;
			
			for(int i = start, l = script.length(), c, n; i < l; i += n) {
				c = script.codePointAt(i);
				n = Utils.charCount(c);
				
				// Quotes logic
				if(c == '\"' && !sq) dq = !dq; else
				if(c == '\'' && !dq) sq = !sq;
				
				// Finding logic
				if(!dq && !sq) {
					if(c == ';' || (wq && c != '\"' && c != '\''))
						break;
				} else {
					wq = true;
				}
				
				sb.appendCodePoint(c);
			}
			
			return sb.toString().trim();
		}
		
		return null;
	}
	
	public static final Map<String, String> varcontents(String script) {
		Map<String, String> vars = new LinkedHashMap<>();
		StringBuilder sb = new StringBuilder();
		Regex pat = Regex.of("(?:var\\s+)?([^\\s=]*)\\s*=");
		Matcher mat = pat.matcher(script);
		
		while(mat.find()) {
			int    start = mat.end();
			String name  = mat.group(1);
			// Find the end of the var content (by finding ';', or script end)
			boolean dq = false;
			boolean sq = false;
			int i = start;
			
			for(int l = script.length(), c, n; i < l; i += n) {
				c = script.codePointAt(i);
				n = Utils.charCount(c);
				
				// Quotes logic
				if(c == '\"' && !sq) dq = !dq; else
				if(c == '\'' && !dq) sq = !sq;
				
				// Finding logic
				if(!dq && !sq) {
					if(c == ';' || c == ',' || c == '{')
						break;
				}
				
				sb.appendCodePoint(c);
			}
			
			if(!name.isEmpty()) {
				vars.put(name, sb.toString().trim());
			}
			
			sb.setLength(0);
			mat.reset(script = script.substring(i));
		}
		
		return vars;
	}
	
	public static final String fromCharCode(int... codePoints) {
	    return new String(codePoints, 0, codePoints.length);
	}
	
	/** @since 00.02.08 */
	public static final SSDCollection readObject(String string) {
		return SSDF.read(string);
	}
	
	// forbid anyone to create an instance of this class
	private JavaScript() {
	}
}