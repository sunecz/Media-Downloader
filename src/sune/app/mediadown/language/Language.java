package sune.app.mediadown.language;

import java.io.InputStream;
import java.util.Objects;

import sune.app.mediadown.update.Version;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDF;

public final class Language {
	
	private final String path;
	private final String name;
	/** @since 00.02.07 */
	private final Version version;
	private final String title;
	/** @since 00.02.02 */
	private final String code;
	private Translation translation;
	
	public Language(String path, String name, Version version, String title, String code, Translation translation) {
		this.path        = Objects.requireNonNull(path);
		this.name        = Objects.requireNonNull(name);
		this.version     = Objects.requireNonNull(version);
		this.title       = Objects.requireNonNull(title);
		this.code        = Objects.requireNonNull(code);
		this.translation = Objects.requireNonNull(translation);
	}
	
	public static final Language from(String path, InputStream stream) {
		SSDCollection data = SSDF.read(stream);
		String name     = data.getDirectString("name");
		Version version = Version.of(data.getDirectString("version"));
		String title    = data.getDirectString("title");
		String code     = data.getDirectString("code");
		return new Language(path, name, version, title, code, new Translation(data));
	}
	
	/** @since 00.02.07 */
	public final String path() {
		return path;
	}
	
	/** @since 00.02.07 */
	public final String name() {
		return name;
	}
	
	/** @since 00.02.07 */
	public final Version version() {
		return version;
	}
	
	/** @since 00.02.07 */
	public final String title() {
		return title;
	}
	
	/** @since 00.02.07 */
	public final String code() {
		return code;
	}
	
	/** @since 00.02.07 */
	public final Translation translation() {
		return translation;
	}
	
	@Override
	public final String toString() {
		return title;
	}
}