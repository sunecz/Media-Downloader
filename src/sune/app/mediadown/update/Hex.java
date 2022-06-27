package sune.app.mediadown.update;

public final class Hex {
	
	// The HEX characters
	private static final char[] CHARS = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'A', 'B', 'C', 'D', 'E', 'F'
	};
	
	// Forbid anyone to create an instance of this class
	private Hex() {
	}
	
	public static final String string(byte[] bytes) {
		int    length = bytes.length;
		char[] chars  = new char[length*2];
		for(int i = 0, k = 0; i < length; ++i, k+=2) {
			int val = bytes[i] & 0xff;
			chars[k] 	 = CHARS[val >>> 4];
			chars[k + 1] = CHARS[val & 0xf];
		}
		return new String(chars);
	}
}