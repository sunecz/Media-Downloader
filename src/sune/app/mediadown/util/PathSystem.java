package sune.app.mediadown.util;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class PathSystem {
	
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final String DIRECTORY = encodeString(getCurrentDirectory(PathSystem.class));
	
	/** @since 00.02.00 */
	// Forbid anyone to create an instance of this class
	private PathSystem() {
	}
	
	private static String encodeString(String string) {
		return new String(CHARSET.encode(string).array(), CHARSET).trim();
	}
	
	public static final String getCurrentDirectory(Class<?> clazz) {
		try {
			return new File(
				clazz
					.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.toURI())
				.getParentFile()
				.getAbsolutePath()
				.replace('\\', '/') + '/';
		} catch(Exception ex) {
			return new File("")
				.getAbsolutePath()
				.replace('\\', '/') + '/';
		}
	}
	
	public static final String getCurrentDirectory() {
		return DIRECTORY;
	}
	
	public static final String getFullPath(Class<?> clazz, String path) {
		return (getCurrentDirectory(clazz) + path).replace('\\', '/');
	}
	
	public static final String getFullPath(String path) {
		return (getCurrentDirectory() + path).replace('\\', '/');
	}
	
	/** @since 00.02.00 */
	public static final Path getPath(Class<?> clazz, String path) {
		return Path.of(getFullPath(clazz, path));
	}
	
	/** @since 00.02.00 */
	public static final Path getPath(String path) {
		return getPath(PathSystem.class, path);
	}
}