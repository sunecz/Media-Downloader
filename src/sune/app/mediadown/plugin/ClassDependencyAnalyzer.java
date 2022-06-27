package sune.app.mediadown.plugin;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import sune.app.mediadown.util.ThrowableFunction;

/** @since 00.02.02 */
// Source: https://stackoverflow.com/a/19928470
public final class ClassDependencyAnalyzer {
	
	private static final byte CONSTANT_Utf8 = 1;
	private static final byte CONSTANT_Integer = 3;
	private static final byte CONSTANT_Float = 4;
	private static final byte CONSTANT_Long = 5;
	private static final byte CONSTANT_Double = 6;
	private static final byte CONSTANT_Class = 7;
	private static final byte CONSTANT_String = 8;
	private static final byte CONSTANT_FieldRef = 9;
	private static final byte CONSTANT_MethodRef = 10;
	private static final byte CONSTANT_InterfaceMethodRef = 11;
	private static final byte CONSTANT_NameAndType = 12;
	private static final byte CONSTANT_MethodHandle = 15;
	private static final byte CONSTANT_MethodType = 16;
	private static final byte CONSTANT_InvokeDynamic = 18;
	
	private static final void addName(Set<String> names, ByteBuffer src, int s, int strSize) {
		final int e = s + strSize;
		StringBuilder dst = new StringBuilder(strSize);
		ascii: {
			for(byte b; s < e; ++s) {
				b = src.get(s);
				if(b < 0) break ascii;
				dst.append((char) (b == '/' ? '.' : b));
			}
			names.add(dst.toString());
			return;
		}
		final int oldLimit = src.limit(), oldPos = dst.length();
		src.limit(e).position(s);
		dst.append(StandardCharsets.UTF_8.decode(src));
		src.limit(oldLimit);
		for(int pos = oldPos, len = dst.length(); pos < len; ++pos) {
			if(dst.charAt(pos) == '/') dst.setCharAt(pos, '.');
		}
		names.add(dst.toString());
	}
	
	private static final void addNames(Set<String> names, ByteBuffer bb, int s, int l) {
		final int e = s + l;
		for(int p; s < e; ++s) {
			if(bb.get(s) == 'L') {
				p = s + 1;
				while(bb.get(p) != ';') ++p;
				addName(names, bb, s + 1, p - s - 1);
				s = p;
			}
		}
	}
	
	public static final Set<String> getDependencies(ByteBuffer bb) {
		if(bb.getInt() != 0xcafebabe)
			throw new IllegalArgumentException("Not a class file");
		bb.position(8);
		final int numC = bb.getChar();
		BitSet clazz = new BitSet(numC), sign = new BitSet(numC);
		for(int c = 1; c < numC; ++c) {
			switch(bb.get()) {
				case CONSTANT_Utf8:
					bb.position(bb.getChar() + bb.position());
					break;
				case CONSTANT_Integer:
				case CONSTANT_Float:
				case CONSTANT_FieldRef:
				case CONSTANT_MethodRef:
				case CONSTANT_InterfaceMethodRef:
				case CONSTANT_InvokeDynamic:
					bb.position(bb.position() + 4);
					break;
				case CONSTANT_Long:
				case CONSTANT_Double:
					bb.position(bb.position() + 8);
					++c;
					break;
				case CONSTANT_String:
					bb.position(bb.position() + 2);
					break;
				case CONSTANT_NameAndType:
					bb.position(bb.position() + 2); // Skip name
					// Fall through
				case CONSTANT_MethodType:
					sign.set(bb.getChar());
					break;
				case CONSTANT_Class:
					clazz.set(bb.getChar());
					break;
				case CONSTANT_MethodHandle:
					bb.position(bb.position() + 3);
					break;
				default:
					throw new IllegalArgumentException("Unknown constant pool item type: "
							+ (bb.get(bb.position() - 1) & 0xff));
			}
		}
		bb.position(bb.position() + 6);
		int interfacesCount = bb.getChar();
		bb.position(bb.position() + interfacesCount * 2);
		// Fields and methods
		for(int type = 0; type < 2; ++type) {
			int numMember = bb.getChar();
			for(int member = 0; member < numMember; ++member) {
				bb.position(bb.position() + 4);
				sign.set(bb.getChar());
				int numAttr = bb.getChar();
				for(int attr = 0; attr < numAttr; ++attr) {
					bb.position(bb.position() + 2);
					int attrLength = bb.getInt();
					bb.position(bb.position() + attrLength);
				}
			}
		}
		bb.position(10);
		Set<String> names = new LinkedHashSet<>();
		for(int c = 1; c < numC; ++c) {
			switch(bb.get()) {
				case CONSTANT_Utf8:
					int strSize = bb.getChar(), strStart = bb.position();
					boolean s = sign.get(c);
					if(clazz.get(c)) {
						if(bb.get(bb.position()) == '[') s = true;
						else addName(names, bb, strStart, strSize);
					}
					if(s) addNames(names, bb, strStart, strSize);
					bb.position(strStart + strSize);
					break;
				case CONSTANT_Integer:
				case CONSTANT_Float:
				case CONSTANT_FieldRef:
				case CONSTANT_MethodRef:
				case CONSTANT_InterfaceMethodRef:
				case CONSTANT_NameAndType:
				case CONSTANT_InvokeDynamic:
					bb.position(bb.position() + 4);
					break;
				case CONSTANT_Long:
				case CONSTANT_Double:
					bb.position(bb.position() + 8);
					++c;
					break;
				case CONSTANT_String:
				case CONSTANT_Class:
				case CONSTANT_MethodType:
					bb.position(bb.position() + 2);
					break;
				case CONSTANT_MethodHandle:
					bb.position(bb.position() + 3);
					break;
				default:
					throw new AssertionError();
			}
		}
		return names;
	}
	
	public static final Set<Class<?>> getDependencies(Class<?> clazz, ThrowableFunction<String, InputStream> resolver) throws Exception {
		while(clazz.isArray()) clazz = clazz.getComponentType();
		if(clazz.isPrimitive()) return Collections.emptySet();
		byte[] bytes = null;
		try(InputStream stream = resolver.apply(clazz.getName().replace('.', '/') + ".class")) {
			bytes = stream.readAllBytes();
		}
		Set<String> names = getDependencies(ByteBuffer.wrap(bytes));
		Set<Class<?>> classes = new HashSet<>(names.size());
		ClassLoader loader = clazz.getClassLoader();
		for(String name : names) classes.add(Class.forName(name, false, loader));
		classes.remove(clazz);// Remove self-reference
		return classes;
	}
}