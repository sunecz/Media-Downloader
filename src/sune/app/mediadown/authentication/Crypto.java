package sune.app.mediadown.authentication;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import sune.app.mediadown.exception.UncheckedException;

/** @since 00.02.09 */
public final class Crypto {
	
	// Forbid anyone to create an instance of this class
	private Crypto() {
	}
	
	public static final class AES256 {
		
		private static final int KEY_SIZE = 256;
		private static final IvParameterSpec IV_ZERO = new IvParameterSpec(zeroIV(16));
		private static final byte[] KEY_SALT_BYTES = { 12, 114, -4, 36, 91, 55, 10, 21 };
		private static final int KEY_NUM_ITERATIONS = 65536;
		
		private AES256() {
		}
		
		private static final KeyGenerator keyGenerator() {
			try {
				return KeyGenerator.getInstance("AES");
			} catch(NoSuchAlgorithmException ex) {
				throw new UncheckedException(ex); // Should not happen
			}
		}
		
		private static final Cipher cipher() {
			try {
				return Cipher.getInstance("AES/CBC/PKCS5Padding");
			} catch(NoSuchAlgorithmException
						| NoSuchPaddingException ex) {
				throw new UncheckedException(ex); // Should not happen
			}
		}
		
		private static final SecretKeyFactory keyFactory() {
			try {
				return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			} catch(NoSuchAlgorithmException ex) {
				throw new UncheckedException(ex); // Should not happen
			}
		}
		
		private static final byte[] zeroIV(int size) {
			byte[] iv = new byte[size];
			Arrays.fill(iv, (byte) 0);
			return iv;
		}
		
		private static final char[] charArray(byte[] data) {
			char[] arr = new char[data.length];
			for(int i = 0, l = data.length; i < l; ++i) arr[i] = (char) (data[i] & 0xff);
			return arr;
		}
		
		private static final SecretKey generateKey(int size) {
			KeyGenerator generator = keyGenerator();
			generator.init(size);
			return generator.generateKey();
		}
		
		private static final byte[] doMode(int mode, byte[] data, SecretKey key, IvParameterSpec iv) {
			Objects.requireNonNull(data);
			Objects.requireNonNull(key);
			Objects.requireNonNull(iv);
			
			try {
				Cipher cipher = cipher();
				cipher.init(mode, key, iv);
				return cipher.doFinal(data);
			} catch(InvalidKeyException
						| InvalidAlgorithmParameterException
						| IllegalBlockSizeException
						| BadPaddingException ex) {
				throw new UncheckedException(ex); // Should not happen
			}
		}
		
		public static final SecretKey generateKey() {
			return generateKey(KEY_SIZE);
		}
		
		public static final SecretKey key(byte[] data) {
			try {
				SecretKeyFactory factory = keyFactory();
		        PBEKeySpec spec = new PBEKeySpec(charArray(data), KEY_SALT_BYTES, KEY_NUM_ITERATIONS, KEY_SIZE);
				SecretKey secretKey = factory.generateSecret(spec);
				return new SecretKeySpec(secretKey.getEncoded(), "AES");
			} catch(InvalidKeySpecException ex) {
				throw new UncheckedException(ex); // Should not happen
			}
		}
		
		public static final byte[] encrypt(byte[] data, SecretKey key) {
			return doMode(Cipher.ENCRYPT_MODE, data, key, IV_ZERO);
		}
		
		public static final byte[] decrypt(byte[] data, SecretKey key) {
			return doMode(Cipher.DECRYPT_MODE, data, key, IV_ZERO);
		}
	}
}