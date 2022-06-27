package sune.app.mediadown.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import sune.app.mediadown.language.Translation;
import sune.app.mediadown.util.Utils;

/**
 * Represents a format (format expression) used for Media titles.
 * 
 * <p>Formats are structured of text, variables and function calls.
 * Each format is then evaluated using a set of variables, possibly
 * an empty set.</p>
 * 
 * <strong>Text</strong>
 * <p>Text in a format is any text that is not a variable, function
 * call or a function argument.</p>
 * 
 * <strong>Variable</strong>
 * <p>Variable in a format is a variable name enclosed in square
 * brackets ({@code [...]}). Variable name is made of characters that
 * are either a letter, digit or an underscore character. Leading or
 * trailing spaces can be used inside the square brackets.</p>
 * 
 * <strong>Function call</strong>
 * <p>Function call in a format is either a simple function call,
 * that is a function name with its arguments enclosed in regular
 * brackets, or a conditional function call with three parts, each
 * divided with vertical line character ({@code |}), where the first
 * part is the simple function call and the other two parts are
 * format expressions.</p>
 * 
 * <p>Example #1:
 * <code>{r([s], ' ', '.')}</code><br>
 * Replaces each space by a dot in the contents of variable {@code s}.
 * </p>
 * 
 * <p>Example #2:
 * <code>{?o([s], [e])|A|B}</code><br>
 * Checks whether at least one of the contents of variables {@code s}
 * and {@code e} is non-empty. If so, returns {@code A}, otherwise
 * {@code B}.
 * </p>
 * 
 * <strong>Supported functions</strong>
 * <br>
 * <table border="1">
 * 	<tr>
 * 		<th>Function call</th>
 * 		<th>Description</th>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?(a)</code></td>
 * 		<td>Checks whether {@code a} is non-empty or {@code true}.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?!(a)</code></td>
 * 		<td>Checks whether {@code a} is empty or {@code false}.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?is(a)</code></td>
 * 		<td>Checks whether {@code a} is a string.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?ii(a)</code></td>
 * 		<td>Checks whether {@code a} is an integer.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?id(a)</code></td>
 * 		<td>Checks whether {@code a} is a decimal.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?ib(a)</code></td>
 * 		<td>Checks whether {@code a} is a boolean.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?o(a, b, c, ...)</code></td>
 * 		<td>Checks whether any of {@code a, b, c, ...} is non-empty.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?a(a, b, c, ...)</code></td>
 * 		<td>Checks whether all of {@code a, b, c, ...} are non-empty.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?=(a, b)</code></td>
 * 		<td>Checks whether {@code a} is equal to {@code b}.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?<(a, b)</code></td>
 * 		<td>Checks whether {@code a} is less than {@code b}.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>?>(a, b)</code></td>
 * 		<td>Checks whether {@code a} is greater to {@code b}.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>+(a, b)</code></td>
 * 		<td>{@code a + b}</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>-(a, b)</code></td>
 * 		<td>{@code a - b}</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>*(a, b)</code></td>
 * 		<td>{@code a * b}</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>/(a, b)</code></td>
 * 		<td>{@code a / b}</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>%(a, b)</code></td>
 * 		<td>{@code a % b}</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>f(f, a, ...)</code></td>
 * 		<td>
 * 			Returns a formatted string of format {@code f} and arguments {@code a, ...}.
 * 			<br>Uses {@linkplain String#format(String, Object...) String::format} syntax.
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>u(a)</code></td>
 * 		<td>Transforms {@code a} to upper-case.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>l(a)</code></td>
 * 		<td>Transforms {@code a} to lower-case.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>t(a)</code></td>
 * 		<td>Transforms each word of {@code a} to title-case.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>tu(a)</code></td>
 * 		<td>Transforms first word of {@code a} to title-case, everything else to upper-case.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>tl(a)</code></td>
 * 		<td>Transforms first word of {@code a} to title-case, everything else to lower-case.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>r(a, f, r)</code></td>
 * 		<td>Replaces all occurrences in {@code a} of {@code f} by {@code r}.</td>
 * 	</tr>
 * 	<tr>
 * 		<td><code>:tr(n, a, b, ...)</code></td>
 * 		<td>
 * 			Obtains a translation string with name {@code n} and optionally passes
 * 			<br>arguments {@code a, b, ...} to it.
 * 		</td>
 * 	</tr>
 * </table>
 * <br>
 * 
 * @author Sune
 * @since 00.02.05
 */
public final class MediaTitleFormat {
	
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	
	private final Tokens tokens;
	
	private MediaTitleFormat(Tokens tokens) {
		this.tokens = Objects.requireNonNull(tokens);
	}
	
	public static final MediaTitleFormat of(String string) {
		return of(string, DEFAULT_CHARSET);
	}
	
	public static final MediaTitleFormat of(String string, Charset charset) {
		return new MediaTitleFormat((new Lexer()).parse(string, charset));
	}
	
	public String format(Map<String, Object> variables) {
		return format(Variables.of(variables));
	}
	
	public String format(Object... variables) {
		return format(Variables.of(variables));
	}
	
	public String format(Variables variables) {
		return (new Evaluator(tokens, variables)).evaluate();
	}
	
	private static final class Functions {
		
		private static final Map<String, ContextFunction> functions = new HashMap<>();
		
		static {
			register("?",   new ConditionIsNonEmptyContextFunction());
			register("?!",  new ConditionIsEmptyContextFunction());
			register("?is", new ConditionIsStringContextFunction());
			register("?ii", new ConditionIsIntegerContextFunction());
			register("?id", new ConditionIsDecimalContextFunction());
			register("?ib", new ConditionIsBooleanContextFunction());
			register("?o",  new ConditionOrContextFunction());
			register("?a",  new ConditionAndContextFunction());
			register("?=",  new ConditionEqualContextFunction());
			register("?<",  new ConditionLessThanContextFunction());
			register("?>",  new ConditionGreaterThanContextFunction());
			register("+",   new AddContextFunction());
			register("-",   new SubtractContextFunction());
			register("*",   new MultiplyContextFunction());
			register("/",   new DivideContextFunction());
			register("%",   new ModuloContextFunction());
			register("f",   new FormatContextFunction());
			register("u",   new StringUpperCaseContextFunction());
			register("l",   new StringLowerCaseContextFunction());
			register("t",   new StringTitleCaseContextFunction());
			register("tu",  new StringTitleUpperCaseContextFunction());
			register("tl",  new StringTitleLowerCaseContextFunction());
			register("r",   new StringReplaceContextFunction());
			register(":tr", new TranslateContextFunction());
		}
		
		private static final void register(String name, ContextFunction function) {
			functions.put(Objects.requireNonNull(name), Objects.requireNonNull(function));
		}
		
		private static final ContextFunction get(String name) {
			ContextFunction function = functions.get(name);
			if(function == null)
				throw new NoSuchElementException("Function '" + name + "' does not exist");
			return function;
		}
		
		/**
		 * Checks whether a value is non-empty.
		 * <br>Syntax: <code>?(val)</code>
		 */
		private static final class ConditionIsNonEmptyContextFunction implements ConditionContextFunction {
			
			@Override
			public void execute(Context context) {
				boolean isNonEmpty = ConditionContextFunction.asBoolean(context.nextToken());
				context.addToken(new Token(TokenType.LITERAL_BOOLEAN, String.valueOf(isNonEmpty)));
				context.addToken(new Token(TokenType.JUMP_FALSE, BRANCH_FALSE));
			}
			
			@Override public int numberOfArguments() { return 1; }
		}
		
		/**
		 * Checks whether a value is empty.
		 * <br>Syntax: <code>?!(val)</code>
		 */
		private static final class ConditionIsEmptyContextFunction implements ConditionContextFunction {
			
			@Override
			public void execute(Context context) {
				boolean isEmpty = !ConditionContextFunction.asBoolean(context.nextToken());
				context.addToken(new Token(TokenType.LITERAL_BOOLEAN, String.valueOf(isEmpty)));
				context.addToken(new Token(TokenType.JUMP_FALSE, BRANCH_FALSE));
			}
			
			@Override public int numberOfArguments() { return 1; }
		}
		
		/**
		 * Checks whether a value is a string.
		 * <br>Syntax: <code>?is(val)</code>
		 */
		private static final class ConditionIsStringContextFunction extends ConditionTypeCheckContextFunction {
			
			@Override
			protected boolean isOfType(Token token) {
				return token.type() == TokenType.LITERAL_STRING;
			}
		}
		
		/**
		 * Checks whether a value is an integer.
		 * <br>Syntax: <code>?ii(val)</code>
		 */
		private static final class ConditionIsIntegerContextFunction extends ConditionTypeCheckContextFunction {
			
			@Override
			protected boolean isOfType(Token token) {
				return token.type() == TokenType.LITERAL_INTEGER;
			}
		}
		
		/**
		 * Checks whether a value is a decimal.
		 * <br>Syntax: <code>?id(val)</code>
		 */
		private static final class ConditionIsDecimalContextFunction extends ConditionTypeCheckContextFunction {
			
			@Override
			protected boolean isOfType(Token token) {
				return token.type() == TokenType.LITERAL_DECIMAL;
			}
		}
		
		/**
		 * Checks whether a value is a boolean.
		 * <br>Syntax: <code>?ib(val)</code>
		 */
		private static final class ConditionIsBooleanContextFunction extends ConditionTypeCheckContextFunction {
			
			@Override
			protected boolean isOfType(Token token) {
				return token.type() == TokenType.LITERAL_BOOLEAN;
			}
		}
		
		/**
		 * Checks whether at least one value is non-empty.
		 * <br>Syntax: <code>?o(val0, val1, ...)</code>
		 */
		private static final class ConditionOrContextFunction extends EarlyExitVariadicConditionContextFunction {
			
			@Override
			protected boolean initialValue() {
				return false;
			}
			
			@Override
			protected boolean condition(Token token) {
				return ConditionContextFunction.asBoolean(token);
			}
		}
		
		/**
		 * Checks whether all values are non-empty.
		 * <br>Syntax: <code>?a(val0, val1, ...)</code>
		 */
		private static final class ConditionAndContextFunction extends EarlyExitVariadicConditionContextFunction {
			
			@Override
			protected boolean initialValue() {
				return true;
			}
			
			@Override
			protected boolean condition(Token token) {
				return !ConditionContextFunction.asBoolean(token);
			}
		}
		
		/**
		 * Checks whether two values are equal.
		 * <br>Syntax: <code>?=(val0, val1)</code>
		 */
		private static final class ConditionEqualContextFunction extends ConditionCompareContextFunction {
			
			@Override
			protected boolean compare(TokenType type, String val0, String val1) {
				boolean equal = false;
				switch(type) {
					case LITERAL_STRING:
					case LITERAL_INTEGER:
					case LITERAL_DECIMAL:
					case LITERAL_BOOLEAN:
						// Do string equal even for integers and decimals
						equal = Objects.equals(val0, val1);
						break;
					default:
						throw new EvaluationException("Type is not comparable");
				}
				return equal;
			}
		}
		
		/**
		 * Checks whether the first value is less than the second one.
		 * <br>Syntax: <code>?<(val0, val1)</code>
		 */
		private static final class ConditionLessThanContextFunction extends ConditionCompareContextFunction {
			
			@Override
			protected boolean compare(TokenType type, String val0, String val1) {
				boolean lessThan = false;
				switch(type) {
					case LITERAL_STRING:
						lessThan = val0.compareTo(val1) < 0;
						break;
					case LITERAL_INTEGER:
						lessThan = Integer.valueOf(val0).compareTo(Integer.valueOf(val1)) < 0;
						break;
					case LITERAL_DECIMAL:
						lessThan = Double.valueOf(val0).compareTo(Double.valueOf(val1)) < 0;
						break;
					case LITERAL_BOOLEAN:
						lessThan = Boolean.valueOf(val0).compareTo(Boolean.valueOf(val1)) < 0;
						break;
					default:
						throw new EvaluationException("Type is not comparable");
				}
				return lessThan;
			}
		}
		
		/**
		 * Checks whether the first value is greater than the second one.
		 * <br>Syntax: <code>?>(val0, val1)</code>
		 */
		private static final class ConditionGreaterThanContextFunction extends ConditionCompareContextFunction {
			
			@Override
			protected boolean compare(TokenType type, String val0, String val1) {
				boolean lessThan = false;
				switch(type) {
					case LITERAL_STRING:
						lessThan = val0.compareTo(val1) > 0;
						break;
					case LITERAL_INTEGER:
						lessThan = Integer.valueOf(val0).compareTo(Integer.valueOf(val1)) > 0;
						break;
					case LITERAL_DECIMAL:
						lessThan = Double.valueOf(val0).compareTo(Double.valueOf(val1)) > 0;
						break;
					case LITERAL_BOOLEAN:
						lessThan = Boolean.valueOf(val0).compareTo(Boolean.valueOf(val1)) > 0;
						break;
					default:
						throw new EvaluationException("Type is not comparable");
				}
				return lessThan;
			}
		}
		
		/**
		 * Adds two numbers.
		 * <br>Syntax: <code>+(val0, val1)</code>
		 */
		private static final class AddContextFunction extends BinaryMathOperationContextFunction {
			
			@Override
			protected String doOperation(int a, int b) {
				return String.valueOf(a + b);
			}
			
			@Override
			protected String doOperation(double a, double b) {
				return String.valueOf(a + b);
			}
		}
		
		/**
		 * Subtracts two numbers.
		 * <br>Syntax: <code>-(val0, val1)</code>
		 */
		private static final class SubtractContextFunction extends BinaryMathOperationContextFunction {
			
			@Override
			protected String doOperation(int a, int b) {
				return String.valueOf(a - b);
			}
			
			@Override
			protected String doOperation(double a, double b) {
				return String.valueOf(a - b);
			}
		}
		
		/**
		 * Multiplies two numbers.
		 * <br>Syntax: <code>/(val0, val1)</code>
		 */
		private static final class MultiplyContextFunction extends BinaryMathOperationContextFunction {
			
			@Override
			protected String doOperation(int a, int b) {
				return String.valueOf(a * b);
			}
			
			@Override
			protected String doOperation(double a, double b) {
				return String.valueOf(a * b);
			}
		}
		
		/**
		 * Divides two numbers.
		 * <br>Syntax: <code>/(val0, val1)</code>
		 */
		private static final class DivideContextFunction extends BinaryMathOperationContextFunction {
			
			@Override
			protected String doOperation(int a, int b) {
				return String.valueOf(a / b);
			}
			
			@Override
			protected String doOperation(double a, double b) {
				return String.valueOf(a / b);
			}
		}
		
		/**
		 * Applies modulo operation to the first number.
		 * <br>Syntax: <code>%(num, mod)</code>
		 */
		private static final class ModuloContextFunction extends BinaryMathOperationContextFunction {
			
			@Override
			protected String doOperation(int a, int b) {
				return String.valueOf(a % b);
			}
			
			@Override
			protected String doOperation(double a, double b) {
				return String.valueOf(a % b);
			}
		}
		
		/**
		 * Formats a value using the given format. Uses Java's String::format.
		 * <br>Syntax: <code>f(val, format)</code>
		 */
		private static final class FormatContextFunction implements VariadicContextFunction {
			
			@Override
			public void execute(Context context) {
				String format = context.nextString();
				
				// Must remove all arguments from the stack first (including ARGUMENTS_END)
				List<Object> values = new ArrayList<>();
				for(Token token; (token = context.nextToken()) != null
									 && token.type() != TokenType.ARGUMENTS_END;) {
					Object value;
					switch(token.type()) {
						case LITERAL_STRING:  value = token.value(); break;
						case LITERAL_INTEGER: value = Integer.valueOf(token.value()); break;
						case LITERAL_DECIMAL: value = Double.valueOf(token.value()); break;
						case LITERAL_BOOLEAN: value = Boolean.valueOf(token.value()); break;
						case LITERAL_NONE:    value = token.value(); break;
						default:
							throw new EvaluationException("Invalid type");
					}
					values.add(value);
				}
				
				Object[] args = values.toArray(Object[]::new);
				String result = String.format(format, args);
				context.addToken(new Token(TokenType.LITERAL_STRING, result));
			}
		}
		
		/**
		 * Transforms a string to upper-case.
		 * <br>Syntax: <code>u(string)</code>
		 */
		private static final class StringUpperCaseContextFunction extends StringUnaryTransformContextFunction {
			
			@Override
			protected String transform(String value) {
				return value.toUpperCase();
			}
		}
		
		/**
		 * Transforms a string to lower-case.
		 * <br>Syntax: <code>l(string)</code>
		 */
		private static final class StringLowerCaseContextFunction extends StringUnaryTransformContextFunction {
			
			@Override
			protected String transform(String value) {
				return value.toLowerCase();
			}
		}
		
		/**
		 * Transforms a string to title-case (applies to each word).
		 * <br>Syntax: <code>t(string)</code>
		 */
		private static final class StringTitleCaseContextFunction extends StringUnaryTransformContextFunction {
			
			@Override
			protected String transform(String value) {
				return Utils.titlize(value, true);
			}
		}
		
		/**
		 * Transforms string's first word to title-case, everything else to upper-case.
		 * <br>Syntax: <code>tu(string)</code>
		 */
		private static final class StringTitleUpperCaseContextFunction extends StringUnaryTransformContextFunction {
			
			@Override
			protected String transform(String value) {
				return Utils.transformEachWord(value, (str, ctr) -> ctr == 0 ? Utils.titlize(str) : str.toUpperCase());
			}
		}
		
		/**
		 * Transforms string's first word to title-case, everything else to lower-case.
		 * <br>Syntax: <code>tl(string)</code>
		 */
		private static final class StringTitleLowerCaseContextFunction extends StringUnaryTransformContextFunction {
			
			@Override
			protected String transform(String value) {
				return Utils.transformEachWord(value, (str, ctr) -> ctr == 0 ? Utils.titlize(str) : str.toLowerCase());
			}
		}
		
		/**
		 * Replaces all occurrences of {@code find} by {@code replace} in {@code string}.
		 * <br>Syntax: <code>r(string, find, replace)</code>
		 */
		private static final class StringReplaceContextFunction implements ContextFunction {
			
			@Override
			public void execute(Context context) {
				String string = context.nextString();
				String find = context.nextString();
				String replace = context.nextString();
				context.addToken(new Token(TokenType.LITERAL_STRING, string.replace(find, replace)));
			}
			
			@Override public int numberOfArguments() { return 3; }
		}
		
		/**
		 * Obtains a translation string with name {@code name} and optionally passes
		 * arguments {@code arg0, arg1, ...} to it.
		 * <br>Syntax: <code>:tr(name, arg0, arg1, ...)</code>
		 */
		private static final class TranslateContextFunction implements VariadicContextFunction {
			
			@Override
			public void execute(Context context) {
				Variable varTranslation = context.var("translation");
				if(varTranslation == null || varTranslation.type() != VariableType.OBJECT
						|| !(varTranslation.value() instanceof Translation)) {
					throw new IllegalArgumentException("Variable 'translation' is not of type Translation");
				}
				
				Translation translation = (Translation) varTranslation.value();
				String name = context.nextString();
				List<Object> args = new ArrayList<>();
				for(Token token; (token = context.nextToken()) != null
						             && token.type() != TokenType.ARGUMENTS_END;) {
					Object arg;
					switch(token.type()) {
						case LITERAL_STRING:  arg = token.value(); break;
						case LITERAL_INTEGER: arg = Integer.valueOf(token.value()); break;
						case LITERAL_DECIMAL: arg = Double.valueOf(token.value()); break;
						case LITERAL_BOOLEAN: arg = Boolean.valueOf(token.value()); break;
						case LITERAL_NONE:    arg = token.value(); break;
						default: arg = null; break;
					}
					if(arg != null) args.add(arg);
				}
				
				String translated = translation.getSingle(name, args.toArray(Object[]::new));
				context.addToken(new Token(TokenType.LITERAL_STRING, translated));
			}
		}
		
		// Helper interface for condition functions
		private static interface ConditionContextFunction extends ContextFunction {
			
			public static final String BRANCH_FALSE = "0";
			
			public static boolean asBoolean(Token token) {
				switch(token.type()) {
					case LITERAL_STRING:  return !token.value().isEmpty();
					case LITERAL_INTEGER: return Integer.valueOf(token.value()) != 0;
					case LITERAL_DECIMAL: return Double.valueOf(token.value()) != 0.0;
					case LITERAL_BOOLEAN: return Boolean.valueOf(token.value());
					case LITERAL_NONE:    return false;
					default:              return !token.value().isEmpty();
				}
			}
			
			@Override
			default int numberOfParts() {
				return 2;
			}
		}
		
		// Helper interface for variadic functions
		private static interface VariadicContextFunction extends ContextFunction {
			
			@Override
			default boolean isVariadic() {
				return true;
			}
		}
		
		// Helper class for comparing values
		private static abstract class ConditionCompareContextFunction implements ConditionContextFunction {
			
			protected abstract boolean compare(TokenType type, String val0, String val1);
			
			@Override
			public void execute(Context context) {
				Token token0 = context.nextToken();
				Token token1 = context.nextToken();
				boolean compare = false;
				
				if(token0.type() == TokenType.LITERAL_NONE
						|| token1.type() == TokenType.LITERAL_NONE) {
					compare = token0.type() == token1.type();
				} else {
					if(token0.type() != token1.type()) {
						throw new EvaluationException("Cannot compare values of different types");
					}
					
					compare = compare(token0.type(), token0.value(), token1.value());
				}
				
				context.addToken(new Token(TokenType.LITERAL_BOOLEAN, String.valueOf(compare)));
				context.addToken(new Token(TokenType.JUMP_FALSE, BRANCH_FALSE));
			}
			
			@Override public int numberOfArguments() { return 2; }
		}
		
		// Helper class for checking value type
		private static abstract class ConditionTypeCheckContextFunction implements ConditionContextFunction {
			
			protected abstract boolean isOfType(Token token);
			
			@Override
			public void execute(Context context) {
				boolean isOfType = isOfType(context.nextToken());
				context.addToken(new Token(TokenType.LITERAL_BOOLEAN, String.valueOf(isOfType)));
				context.addToken(new Token(TokenType.JUMP_FALSE, BRANCH_FALSE));
			}
			
			@Override public int numberOfArguments() { return 1; }
		}
		
		// Helper class for early exit variadic condition functions
		private static abstract class EarlyExitVariadicConditionContextFunction
				implements ConditionContextFunction, VariadicContextFunction {
			
			protected abstract boolean initialValue();
			protected abstract boolean condition(Token token);
			
			@Override
			public void execute(Context context) {
				boolean success = initialValue();
				boolean endReached = false;
				
				// Poll and execute tokens one by one to execute only required tokens
				for(Token token; (token = context.nextToken()) != null;) {
					// Keep track of the end of the arguments
					if(token.type() == TokenType.ARGUMENTS_END) {
						endReached = true;
						break;
					}
					
					if(condition(token)) {
						success = !success;
						break; // Do not process other tokens
					}
				}
				
				// If the end was not reached, remove all left-over tokens
				if(!endReached) {
					// Use raw mode to prevent processing of tokens
					int ctr = 1;
					for(Token token; (token = context.rawNextToken()) != null;) {
						TokenType type = token.type();
						if(type == TokenType.ARGUMENTS_END) {
							// Only exit the loop when the actual ARGUMENTS_END was reached
							if(--ctr == 0) break;
						}
						
						// Must check whether there are any functions with variadic arguments,
						// that also have their ARGUMENTS_END.
						if(type == TokenType.IDENTIFIER_FUNCTION) {
							ContextFunction function = Functions.get(token.value());
							if(function.isVariadic()) ++ctr;
						}
					}
				}
				
				context.addToken(new Token(TokenType.LITERAL_BOOLEAN, String.valueOf(success)));
				context.addToken(new Token(TokenType.JUMP_FALSE, BRANCH_FALSE));
			}
	    }
		
		// Helper class for doing binary mathematical operations
		private static abstract class BinaryMathOperationContextFunction implements ContextFunction {
			
			private static final boolean isAllowedType(TokenType type) {
				return type == TokenType.LITERAL_INTEGER || type == TokenType.LITERAL_DECIMAL;
			}
			
			protected static final int toInteger(String val) {
				return Integer.valueOf(val);
			}
			
			protected static final double toDouble(String val) {
				return Double.valueOf(val);
			}
			
			protected abstract String doOperation(int a, int b);
			protected abstract String doOperation(double a, double b);
			
			@Override
			public void execute(Context context) {
				Token token0 = context.nextToken();
				Token token1 = context.nextToken();
				
				TokenType type0 = token0.type();
				TokenType type1 = token1.type();
				
				if(!isAllowedType(type0) || !isAllowedType(type1)) {
					throw new EvaluationException("Invalid type for math operation");
				}
				
				String val0 = token0.value();
				String val1 = token1.value();
				
				TokenType resultType;
				String result;
				if(type0 == type1) {
					switch(type0) {
						case LITERAL_INTEGER:
							result = doOperation(toInteger(val0), toInteger(val1));
							resultType = TokenType.LITERAL_INTEGER;
							break;
						case LITERAL_DECIMAL:
							result = doOperation(toDouble(val0), toDouble(val1));
							resultType = TokenType.LITERAL_DECIMAL;
							break;
						default:
							throw new AssertionError("Invalid type"); // Should not happen
					}
				} else {
					// One value is integer, the other one is decimal. Just convert both to Double.
					result = doOperation(toDouble(val0), toDouble(val1));
					resultType = TokenType.LITERAL_DECIMAL;
				}
				
				context.addToken(new Token(resultType, result));
			}
			
			@Override public int numberOfArguments() { return 2; }
		}
		
		// Helper class for string transformations
		private static abstract class StringUnaryTransformContextFunction implements ContextFunction {
			
			protected abstract String transform(String value);
			
			@Override
			public void execute(Context context) {
				String string = context.nextString();
				String result = transform(string);
				context.addToken(new Token(TokenType.LITERAL_STRING, result));
			}
			
			@Override public int numberOfArguments() { return 1; }
		}
	}
	
	private static final class Evaluator implements Context {
		
		private final Tokens tokens;
		private final Variables variables;
		
		public Evaluator(Tokens tokens, Variables variables) {
			this.tokens = Objects.requireNonNull(new Tokens(tokens));
			this.variables = Objects.requireNonNull(variables);
		}
		
		private final Token processToken(Token token) {
			switch(token.type()) {
				case IDENTIFIER_VARIABLE:
					String name = token.value();
					Variable var = var(name);
					
					TokenType type;
					String value;
					switch(var.type()) {
						case STRING:
							type = TokenType.LITERAL_STRING;
							value = (String) var.value();
							break;
						case INTEGER:
							type = TokenType.LITERAL_INTEGER;
							value = String.valueOf((Integer) var.value());
							break;
						case DECIMAL:
							type = TokenType.LITERAL_DECIMAL;
							value = String.valueOf((Double) var.value());
							break;
						case BOOLEAN:
							type = TokenType.LITERAL_BOOLEAN;
							value = String.valueOf((Boolean) var.value());
							break;
						case OBJECT:
							type = TokenType.LITERAL_STRING;
							value = String.valueOf(var.value());
							break;
						default:
							type = TokenType.LITERAL_NONE;
							value = "";
							break;
					}
					
					token = new Token(type, value);
					break;
				case IDENTIFIER_FUNCTION:
					Functions.get(token.value()).execute(this);
					
					boolean hasParts = false;
					Token tokenJump = rawNextToken();
					if(tokenJump != null) {
						if(tokenJump.type() == TokenType.JUMP_FALSE) {
							Token tokenBool = rawNextToken();
							if(tokenBool == null || tokenBool.type() != TokenType.LITERAL_BOOLEAN) {
								throw new EvaluationException("Missing boolean value for jump");
							}
							
							Token tokenBoundaryContext = rawNextToken();
							if(tokenBoundaryContext != null) {
								if(tokenBoundaryContext.type() == TokenType.BOUNDARY_CONTEXT) {
									// Token for the boundary found, we have a function with parts
									hasParts = true;
								}
								// Add read token back
								addToken(tokenBoundaryContext);
							}
							
							// Add read token back
							addToken(tokenBool);
							
							// Add the jump token back only if there are no parts, since
							// without parts the jumping is useless.
							if(hasParts) {
								addToken(tokenJump);
							}
						} else {
							// Add any other token back
							addToken(tokenJump);
						}
					}
					
					token = nextToken();
					break;
				case JUMP_FALSE:
					Token next = nextToken();
					if(next.type() != TokenType.LITERAL_BOOLEAN) {
						throw new EvaluationException("Not a boolean value");
					}
					
					boolean val = Boolean.valueOf(next.value());
					if(val) {
						token = nextToken();
						break; // Do not jump, since value is true
					}
					
					// Fall-through
				case JUMP:
					int jumpTo = Integer.valueOf(token.value());
					Deque<Integer> ctrs = new ArrayDeque<>();
					int ctr = 0;
					
					jumpLoop:
					for(Token t; (t = rawNextToken()) != null;) {
						switch(t.type()) {
							case BOUNDARY_CONTEXT:
								if(ctr > 0) {
									ctrs.addFirst(ctr);
								}
								ctr = Integer.valueOf(t.value());
								break;
							case BOUNDARY:
								if(ctrs.isEmpty()
										&& Integer.valueOf(t.value()) == jumpTo) {
									token = nextToken();
									break jumpLoop;
								}
								
								--ctr;
								if(ctr == 0) {
									ctr = ctrs.pollFirst();
								}
								break;
							default:
								// Ignore
								break;
						}
					}
					break;
				// Token types to be skipped
				case BOUNDARY_CONTEXT:
				case BOUNDARY:
				case TEXT_BEGIN:
				case TEXT_END:
					token = nextToken();
					break;
				default:
					// Do nothing
					break;
			}
			return token;
		}
		
		public String evaluate() {
			StringBuilder str = new StringBuilder();
			
			for(Token token; (token = nextToken()) != null;) {
				switch(token.type()) {
					case TEXT:
					case LITERAL_STRING:
					case LITERAL_INTEGER:
					case LITERAL_DECIMAL:
					case LITERAL_BOOLEAN:
					case LITERAL_NONE:
						str.append(token.value());
						break;
					default:
						throw new EvaluationException("Invalid token: " + token);
				}
			}
			
			return str.toString();
		}
		
		@Override
		public final Token rawNextToken() {
			return tokens.nextToken();
		}
		
		@Override
		public void addToken(Token token) {
			tokens.addToken(token);
		}
		
		@Override
		public void addTokens(Token... tokens) {
			this.tokens.addTokens(tokens);
		}
		
		@Override
		public Token nextToken() {
			Token token = rawNextToken();
			
			if(token == null)
				return null;
			
			return processToken(token);
		}
		
		@Override
		public Token nextToken(TokenType type) {
			Token next = nextToken();
			if(next == null)
				throw new NoSuchElementException("No token present");
			if(next.type() != type)
				throw new NoSuchElementException("Incorrect token type");
			return next;
		}
		
		@Override
		public String nextString() {
			return nextToken(TokenType.LITERAL_STRING).value();
		}
		
		@Override
		public int nextInteger() {
			return Integer.valueOf(nextToken(TokenType.LITERAL_INTEGER).value());
		}
		
		@Override
		public double nextDecimal() {
			return Double.valueOf(nextToken(TokenType.LITERAL_DECIMAL).value());
		}
		
		@Override
		public boolean nextBoolean() {
			return Boolean.valueOf(nextToken(TokenType.LITERAL_BOOLEAN).value());
		}
		
		@Override
		public Variable var(String name) {
			Variable var = variables.get(name);
			return var != null ? var : new Variable(VariableType.NONE, null);
		}
	}
	
	/**
	 * Processes a string to a list of tokens.
	 */
	private static final class Lexer {
		
		private LexerReader reader;
		
		private static final ReadableByteChannel stringChannel(String string, Charset charset) {
			return Channels.newChannel(
				new ByteArrayInputStream(Objects.requireNonNull(string).getBytes(Objects.requireNonNull(charset)))
			);
		}
		
		public Tokens parse(String string, Charset charset) {
			try(ReadableByteChannel channel = stringChannel(string, charset)) {
				reader = new LexerReader(channel, charset);
				return reader.read();
			} catch(IOException ex) {
				throw new AssertionError(ex); // Should not happen
			}
		}
		
		private static final class LexerReader extends ChannelReader<Tokens> {
			
			private static final int CHAR_STRING_QUOTES = '\'';
			private static final int CHAR_ESCAPE_SLASH = '\\';
			private static final int CHAR_VARIABLE_OPEN = '[';
			private static final int CHAR_VARIABLE_CLOSE = ']';
			private static final int CHAR_FUNCTION_CALL_OPEN = '{';
			private static final int CHAR_FUNCTION_CALL_CLOSE = '}';
			private static final int CHAR_FUNCTION_CALL_PART_SEPARATOR = '|';
			private static final int CHAR_FUNCTION_ARGUMENTS_OPEN = '(';
			private static final int CHAR_FUNCTION_ARGUMENTS_CLOSE = ')';
			private static final int CHAR_FUNCTION_ARGUMENTS_SEPARATOR = ',';
			
			private final Deque<Token> tokens = new ArrayDeque<>();
			private final StringBuilder str = new StringBuilder();
			private int c;
			
			protected LexerReader(ReadableByteChannel input, Charset charset) {
				super(input, charset);
			}
			
			private static final boolean isMathSymbol(int c) {
				return c == '+' || c == '-' || c == '*' || c == '/' || c == '%';
			}
			
			private static final boolean isIdentifierCharacter(int c) {
				return Character.isLetterOrDigit(c) || c == '_';
			}
			
			private static final boolean isExtendedIdentifierCharacter(int c) {
				return isMathSymbol(c) || c == '?' || c == '!' || c == '=' || c == '<' || c == '>' || c == ':';
			}
			
			private static final boolean isSpecialEscapeCharacter(int c) {
				return c == 'n' || c == 'r' || c == 't' || c == 'b' || c == 'f' || c == 'u';
			}
			
			private static final ContextFunction function(Token token) {
				if(token.type() != TokenType.IDENTIFIER_FUNCTION)
					throw new AssertionError("Not a function token");
				return Functions.get(token.value());
			}
			
			private final void throwParseException(String message) throws ParseException {
				throw new ParseException(message + (c != -1 ? " (char='" + Character.toString(c) + "')" : ""));
			}
			
			private final void checkCharacter(int expected, String message) throws ParseException {
				if(c != expected) {
					throwParseException(message);
				}
			}
			
			private final void append(int codePoint) {
				str.appendCodePoint(codePoint);
			}
			
			private final void addToken(TokenType type) {
				tokens.addLast(new Token(type, str.toString()));
				str.setLength(0);
			}
			
			private final void addToken(TokenType type, String value) {
				tokens.addLast(new Token(type, value));
			}
			
			private final int skipWhitespaces() throws IOException {
				while(Character.isWhitespace(c))
					c = next();
				return c;
			}
			
			private final int nextAndSkipWhitespaces() throws IOException {
				c = next();
				return skipWhitespaces();
			}
			
			private final void readString() throws IOException, ParseException {
				checkCharacter(CHAR_STRING_QUOTES, "String has no opening quote");
				
				str.ensureCapacity(lim - pos); // Optimization
				boolean escaped = false;
				while((c = next()) != -1) {
					if(c == CHAR_STRING_QUOTES && !escaped) {
						break; // String closed
					} else if(c == CHAR_ESCAPE_SLASH) {
						// Special case for \\
						if(escaped) {
							append(c);
						}
						
						escaped = true;
					} else {
						if(escaped) {
							escaped = false;
							
							if(isSpecialEscapeCharacter(c)) {
								append(CHAR_ESCAPE_SLASH);
							}
						}
						
						append(c);
					}
				}
				
				checkCharacter(CHAR_STRING_QUOTES, "String has no closing quote");
				addToken(TokenType.LITERAL_STRING);
			}
			
			private final void readNumber() throws IOException, ParseException {
				// Handle negative numbers
				if(c == '-') {
					append(c);
					c = next();
				}
				
				// At least one digit must be present
				if(!Character.isDigit(c)) {
					throwParseException("Number has no digits");
				}
				
				boolean fraction = false;
				boolean exponent = false;
				loop:
				do {
					if(Character.isDigit(c)) {
						append(c);
					} else {
						switch(c) {
							case '.':
								if(fraction) {
									throwParseException("Multiple fractions at number");
								}
								if(exponent) {
									throwParseException("Fraction specified after exponent");
								}
								append(c);
								fraction = true;
								break;
							case 'e':
							case 'E':
								if(exponent) {
									throwParseException("Multiple exponents at number");
								}
								append(c);
								exponent = true;
								c = next();
								if(c != '-' && c != '+') {
									throwParseException("Exponent has no '-' or '+' specified");
								}
								append(c);
								c = next();
								if(!Character.isDigit(c)) {
									throwParseException("Exponent has no digits");
								}
								append(c);
								break;
							default:
								break loop;
						}
					}
				} while((c = next()) != -1);
				
				addToken(fraction ? TokenType.LITERAL_DECIMAL : TokenType.LITERAL_INTEGER);
				c = prev(); // Must push back the last character
			}
			
			private final void readAndMatchSequence(String sequence) throws IOException, ParseException {
				boolean success = true;
				for(int i = 0, l = sequence.length(); i < l && (c = next()) != -1; ++i) {
					if(c != sequence.codePointAt(i)) {
						success = false;
						break;
					}
					append(c);
				}
				
				if(!success) {
					throwParseException("Sequence not matched");
				}
			}
			
			private final void readFalse() throws IOException, ParseException {
				readAndMatchSequence("false");
			}
			
			private final void readTrue() throws IOException, ParseException {
				readAndMatchSequence("true");
			}
			
			private final void readIdentifier(boolean extended, int until) throws IOException, ParseException {
				boolean empty = true;
				while((c = next()) != -1 && c != until) {
					// Allow leading whitespaces before the identifier (e.g. [  a])
					if(empty && Character.isWhitespace(c)) {
						c = skipWhitespaces();
						c = prev(); // So that next() = non-whitespace character
						continue;
					}
					
					// Allow trailing whitespaces after the identifier (e.g. [a  ])
					if(Character.isWhitespace(c) && str.length() > 0) {
						c = skipWhitespaces();
						
						// There must be only the closing character after trailing whitespaces
						if(c != until) {
							throwParseException("Invalid closing character");
						}
						
						c = prev(); // So that next() = non-whitespace character
						continue;
					}
					
					if(!isIdentifierCharacter(c)
							&& (!extended || !isExtendedIdentifierCharacter(c))) {
						throwParseException("Invalid identifier character");
					}
					
					append(c);
					empty = false;
				}
			}
			
			private final void readVariable() throws IOException, ParseException {
				checkCharacter(CHAR_VARIABLE_OPEN, "Variable has no opening bracket");
				readIdentifier(false, CHAR_VARIABLE_CLOSE);
				checkCharacter(CHAR_VARIABLE_CLOSE, "Variable has no closing bracket");
				addToken(TokenType.IDENTIFIER_VARIABLE);
			}
			
			private final void readFunctionName() throws IOException, ParseException {
				readIdentifier(true, CHAR_FUNCTION_ARGUMENTS_OPEN);
				addToken(TokenType.IDENTIFIER_FUNCTION);
			}
			
			private final void readFunctionArguments(ContextFunction function) throws IOException, ParseException {
				checkCharacter(CHAR_FUNCTION_ARGUMENTS_OPEN, "Function arguments have no opening bracket");
				boolean isVariadic = function.isVariadic();
				
				int num = 0;
				while((c = next()) != -1 && c != CHAR_FUNCTION_ARGUMENTS_CLOSE) {
					c = prev(); // Checked character must be put back
					readFunctionArgument();
					++num;
				}
				
				if(!isVariadic && num != function.numberOfArguments()) {
					throwParseException("Invalid number of arguments: " + num);
				}
				
				if(isVariadic) {
					addToken(TokenType.ARGUMENTS_END);
				}
				
				checkCharacter(CHAR_FUNCTION_ARGUMENTS_CLOSE, "Function arguments have no closing bracket");
			}
			
			private final void readFunctionArgument() throws IOException, ParseException {
				while((c = nextAndSkipWhitespaces()) != -1
							&& (c != CHAR_FUNCTION_ARGUMENTS_SEPARATOR
							&&  c != CHAR_FUNCTION_ARGUMENTS_CLOSE)) {
					switch(c) {
						case CHAR_VARIABLE_OPEN: readVariable(); break;
						case CHAR_FUNCTION_CALL_OPEN: readFunctionCall(); break;
						case CHAR_STRING_QUOTES: readString(); break;
						case 'f': c = prev(); readFalse(); break;
						case 't': c = prev(); readTrue(); break;
						default:
							if(c == '-' || Character.isDigit(c)) readNumber();
							else throwParseException("Invalid function argument separator or closing");
							break;
					}
				}
				
				// Put the closing character back due to outside checking
				if(c == CHAR_FUNCTION_ARGUMENTS_CLOSE) {
					c = prev();
				}
			}
			
			private final void readFunctionCall() throws IOException, ParseException {
				checkCharacter(CHAR_FUNCTION_CALL_OPEN, "Function call has no opening bracket");
				
				// Allow leading spaces in the function call
				c = skipWhitespaces();
				readFunctionName();
				ContextFunction function = function(tokens.peekLast());
				readFunctionArguments(function);
				
				int numOfParts;
				if((numOfParts = function.numberOfParts()) > 0) {
					boolean readParts = true;
					
					// Allow trailing spaces in the function call, first part only,
					// since other parts are parsed as expressions.
					c = skipWhitespaces();
					
					// Handle the case where there are no parts
					if((c = next()) != CHAR_FUNCTION_CALL_PART_SEPARATOR) {
						if(!function.arePartsOptional()) {
							throwParseException("Missing required function call parts");
						}
						
						readParts = false;
					}
					
					if(readParts) {
						c = prev(); // Put the part separator back
						addToken(TokenType.BOUNDARY_CONTEXT, String.valueOf(numOfParts));
						int partsLeft = numOfParts;
						
						// Parse each of the parts as an expression within function context
						for(int boundaryId = 0; (c = next()) != -1 && partsLeft > 0; --partsLeft, ++boundaryId) {
							checkCharacter(CHAR_FUNCTION_CALL_PART_SEPARATOR, "No function call part separator present");
							readExpression(true);
							
							if(partsLeft != 1) { // Not the last part
								addToken(TokenType.JUMP, String.valueOf(numOfParts - 1)); // Jump to the last boundary
							}
							
							addToken(TokenType.BOUNDARY, String.valueOf(boundaryId));
						}
						
						if(partsLeft != 0) {
							throwParseException("Invalid number of function call parts");
						}
					}
				} else {
					c = next(); // Must move to the closing character
				}
				
				// Allow trailing spaces in the function call
				c = skipWhitespaces();
				
				checkCharacter(CHAR_FUNCTION_CALL_CLOSE, "Function call has no closing bracket");
			}
			
			private final boolean readText(boolean isFunctionContext) throws IOException, ParseException {
				boolean exit = false;
				boolean escaped = false;
				
				loop:
				do {
					if(c == CHAR_ESCAPE_SLASH) {
						// Special case for \\
						if(escaped) {
							append(c);
						}
						
						escaped = true;
						continue;
					} else if(escaped) {
						escaped = false;
						
						// Additionally keep the slash when the character does not need to be escaped
						if((c != CHAR_VARIABLE_OPEN && c != CHAR_VARIABLE_CLOSE)
								&& (c != CHAR_FUNCTION_CALL_OPEN && c != CHAR_FUNCTION_CALL_CLOSE)
								&& (!isFunctionContext || c != CHAR_FUNCTION_CALL_PART_SEPARATOR)) {
							append(CHAR_ESCAPE_SLASH);
						}
						
						append(c);
						continue;
					}
					
					// No parsing of literals needed outside of a function condition
					switch(c) {
						case CHAR_VARIABLE_OPEN:
						case CHAR_FUNCTION_CALL_OPEN:
							c = prev(); // Character must be put back
							break loop;
						case CHAR_FUNCTION_CALL_CLOSE:
						case CHAR_FUNCTION_CALL_PART_SEPARATOR:
							if(isFunctionContext) {
								c = prev(); // Character must be put back
								exit = true;
								break loop;
							}
							// Fall-through
						default:
							append(c);
							break;
					}
				} while((c = next()) != -1);
				
				addToken(TokenType.TEXT);
				return exit;
			}
			
			private final void readExpression(boolean isFunctionContext) throws IOException, ParseException {
				boolean text = false;
				
				loop:
				while((c = next()) != -1) {
					// No parsing of literals needed outside of a function condition
					switch(c) {
						case CHAR_VARIABLE_OPEN: readVariable(); break;
						case CHAR_FUNCTION_CALL_OPEN: readFunctionCall(); break;
						default:
							if(!text) {
								addToken(TokenType.TEXT_BEGIN);
								text = true;
							}
							if(readText(isFunctionContext)) break loop;
							break;
					}
				}
				
				if(text) {
					addToken(TokenType.TEXT_END);
				}
			}
			
			@Override
			public Tokens read() throws IOException, ParseException {
				readExpression(false);
				return new Tokens(tokens);
			}
		}
	}
	
	public static final class Variables {
		
		private final Map<String, Variable> variables = new HashMap<>();
		
		private Variables() {
		}
		
		public static final Variables empty() {
			return new Variables();
		}
		
		public static final Variables of(Object... vars) {
			return of(Utils.toMap(vars));
		}
		
		public static final Variables of(Map<String, Object> vars) {
			Variables v = new Variables();
			Objects.requireNonNull(vars).entrySet().stream()
				.forEach((t) -> v.set(t.getKey(), t.getValue()));
			return v;
		}
		
		private final VariableType variableType(Object value) {
			if(value == null) return VariableType.NONE;
			
			Class<?> clazz = value.getClass();
			if(clazz == String.class) return VariableType.STRING;
			if(clazz == Character.class) return VariableType.STRING;
			if(clazz == Byte.class) return VariableType.INTEGER;
			if(clazz == Short.class) return VariableType.INTEGER;
			if(clazz == Integer.class) return VariableType.INTEGER;
			if(clazz == Long.class) return VariableType.INTEGER;
			if(clazz == Float.class) return VariableType.DECIMAL;
			if(clazz == Double.class) return VariableType.DECIMAL;
			if(clazz == Boolean.class) return VariableType.BOOLEAN;
			
			return VariableType.OBJECT;
		}
		
		private final Object castValue(Object value, VariableType type) {
			switch(type) {
				case STRING:  return String.valueOf(value);
				case INTEGER: return (int) value;
				case DECIMAL: return (double) value;
				case BOOLEAN: return (boolean) value;
				case OBJECT:  return (Object) value;
				default:      return null;
			}
		}
		
		private final void set(String name, VariableType type, Object value) {
			variables.put(name, new Variable(type, castValue(value, type)));
		}
		
		public void set(String name, Object value) {
			Objects.requireNonNull(name);
			Objects.requireNonNull(value);
			set(name, variableType(value), value);
		}
		
		public Variable get(String name) {
			return variables.get(Objects.requireNonNull(name));
		}
	}
	
	public static final class Variable {
		
		private final VariableType type;
		private final Object value;
		
		private Variable(VariableType type, Object value) {
			this.type = Objects.requireNonNull(type);
			this.value = value; // Can be null
		}
		
		public static final Variable ofString(String value) {
			return new Variable(VariableType.STRING, Objects.requireNonNull(value));
		}
		
		public static final Variable ofInteger(int value) {
			return new Variable(VariableType.INTEGER, value);
		}
		
		public static final Variable ofDecimal(double value) {
			return new Variable(VariableType.DECIMAL, value);
		}
		
		public static final Variable ofBoolean(boolean value) {
			return new Variable(VariableType.BOOLEAN, value);
		}
		
		public static final Variable ofObject(Object object) {
			return new Variable(VariableType.OBJECT, Objects.requireNonNull(object));
		}
		
		public VariableType type() {
			return type;
		}
		
		public Object value() {
			return value;
		}
	}
	
	public static enum VariableType {
		
		STRING, INTEGER, DECIMAL, BOOLEAN, OBJECT, NONE;
	}
	
	private static interface Context {
		
		void addToken(Token token);
		void addTokens(Token... tokens);
		
		Token rawNextToken();
		Token nextToken();
		Token nextToken(TokenType type);
		String nextString();
		int nextInteger();
		double nextDecimal();
		boolean nextBoolean();
		
		Variable var(String name);
	}
	
	private static interface ContextFunction {
		
		void execute(Context context);
		
		default boolean isVariadic() {
			return false;
		}
		
		default int numberOfArguments() {
			return 0;
		}
		
		default int numberOfParts() {
			return 0;
		}
		
		default boolean arePartsOptional() {
			return true;
		}
	}
	
	private static final class Tokens {
		
		private final Deque<Token> tokens;
		
		public Tokens(Tokens tokens) {
			this.tokens = new ArrayDeque<>(Objects.requireNonNull(tokens).tokens);
		}
		
		public Tokens(Deque<Token> tokens) {
			this.tokens = Objects.requireNonNull(tokens);
		}
		
		public void addToken(Token token) {
			tokens.addFirst(token);
		}
		
		public void addTokens(Token... tokens) {
			// Add in reverse to keep order
			for(int i = tokens.length - 1; i >= 0; --i)
				this.tokens.addFirst(tokens[i]);
		}
		
		public Token nextToken() {
			return tokens.pollFirst();
		}
	}
	
	private static final class Token {
		
		private final TokenType type;
		private final String value;
		
		public Token(TokenType type, String value) {
			this.type = Objects.requireNonNull(type);
			this.value = value; // Can be null
		}
		
		public TokenType type() {
			return type;
		}
		
		public String value() {
			return value;
		}
		
		@Override
		public String toString() {
			return "Token[type=" + type + ", value=" + value + "]";
		}
	}
	
	private static enum TokenType {
		
		/* Note:
		 * No other token types are necessary, since we don't support comments,
		 * keywords, and operators. All is either an identifier (variable name,
		 * function), separator (for various brackets and comma), and literals
		 * (for integers, decimals and strings)
		 */
		
		/**
		 * Represents all variables, such as {@code v} in '{@code [v]}'.
		 */
		IDENTIFIER_VARIABLE,
		/**
		 * Represents all functions, such as {@code u} in '<code>{u(a,b)}</code>'.
		 */
		IDENTIFIER_FUNCTION,
		/**
		 * Represents all string literals, such as {@code 's'} in '<code>{r([a],'s')}</code>'.
		 */
		LITERAL_STRING,
		/**
		 * Represents all integer literals, such as {@code 2} in '<code>{f(a,2)}</code>'.
		 */
		LITERAL_INTEGER,
		/**
		 * Represents all decimal literals, such as {@code 8.35} in '<code>{g(8.35)}</code>'.
		 */
		LITERAL_DECIMAL,
		/**
		 * Represents all boolean literals, i.e. {@code true} and {@code false}.
		 */
		LITERAL_BOOLEAN,
		/**
		 * Represents a literal without a value, i.e. behaves like {@code null}.
		 */
		LITERAL_NONE,
		/**
		 * Represents all text that is outside of a variable or a function call context,
		 * such as '{@code and this is a text}' in '{@code [a] and this is a text [b]}'.
		 */
		TEXT,
		/**
		 * Helper token type that represents the beginning of text that should be concatenated.
		 */
		TEXT_BEGIN,
		/**
		 * Helper token type that represents the ending of text that should be concatenated.
		 */
		TEXT_END,
		/**
		 * Helper token type that represents the ending of a function arguments sequence.
		 * This allows functions to have variadic arguments.
		 */
		ARGUMENTS_END,
		/**
		 * Helper token type that represents a boundary context that contains some number
		 * of boundaries. This allows conditional branches to be implemented.
		 */
		BOUNDARY_CONTEXT,
		/**
		 * Helper token type that represents a boundary to which it is possible to jump.
		 * This allows conditional branches to be implemented.
		 */
		BOUNDARY,
		/**
		 * Helper token type that represents an unconditional jump. This allows conditional
		 * branches to be implemented.
		 */
		JUMP,
		/**
		 * Helper token type that represents a jump, if a value on the stack is false.
		 * This allows conditional branches to be implemented.
		 */
		JUMP_FALSE;
	}
	
	public static class EvaluationException extends RuntimeException {
		
		private static final long serialVersionUID = -8429238573126115063L;
		
		public EvaluationException() { super(); }
		public EvaluationException(String s) { super(s); }
		public EvaluationException(String message, Throwable cause) { super(message, cause); }
		public EvaluationException(Throwable cause) { super(cause); }
	}
	
	public static class ParseException extends RuntimeException {
		
		private static final long serialVersionUID = -8429238573126115063L;
		
		public ParseException() { super(); }
		public ParseException(String s) { super(s); }
		public ParseException(String message, Throwable cause) { super(message, cause); }
		public ParseException(Throwable cause) { super(cause); }
	}
	
	private static abstract class ChannelReader<T> {
		
		/* Implementation note:
		 * Use absolute CharBuffer::get(int) method for better performance, since Java allegedly generates
		 * worse code for the relative CharBuffer::get() method.
		 */
		
		private static final int BUFFER_SIZE = 8192;
		
		protected final Reader input;
		protected final CharBuffer buf;
		protected int pos;
		protected int lim;
		
		protected ChannelReader(ReadableByteChannel input, Charset charset) {
			this.input = Channels.newReader(Objects.requireNonNull(input), charset);
			this.buf = CharBuffer.allocate(BUFFER_SIZE).flip();
		}
		
		protected int fill() throws IOException {
			buf.position(pos);
			buf.compact();
			int read = input.read(buf);
			buf.flip();
			pos = 0;
			lim = buf.limit();
			return read;
		}
		
		protected int next() throws IOException {
			int r = lim - pos;
			if(r < 2 && fill() == -1) {
				if(r == 0)
					return -1;
				char a = buf.get(pos++);
				if(Character.isHighSurrogate(a))
					throw new IOException("Invalid character");
				return a;
			}
			char a = buf.get(pos++);
			if(!Character.isHighSurrogate(a))
				return a;
			char b = buf.get(pos++);
			if(!Character.isLowSurrogate(b))
				throw new IOException("Invalid character");
			return Character.toCodePoint(a, b);
		}
		
		protected int prev() throws IOException {
			char b = buf.get(pos - 1);
			if(Character.isLowSurrogate(b)) {
				char a = buf.get(pos - 2);
				if(!Character.isHighSurrogate(a)) { // Be paranoid
					throw new IOException("Invalid character");
				}
				pos -= 2;
				return Character.toCodePoint(a, b);
			} else {
				pos -= 1;
				return b;
			}
		}
		
		public abstract T read() throws IOException;
	}
}