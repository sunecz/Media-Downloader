package sune.app.mediadown.language;

import java.io.InputStream;

import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDF;

public final class Language {
	
	private final String path;
	private final String name;
	private final String version;
	private final String title;
	/** @since 00.02.02 */
	private final String code;
	private Translation translation;
	
	public Language(String path, String name, String version, String title, String code, Translation translation) {
		if((path == null || name == null || version == null || title == null || code == null || translation == null))
			throw new IllegalArgumentException();
		this.path        = path;
		this.name        = name;
		this.version     = version;
		this.title       = title;
		this.code        = code;
		this.translation = translation;
	}
	
	public static final Language from(String path, InputStream stream) {
		SSDCollection data = SSDF.read(stream);
		String name    = data.getDirectString("name");
		String version = data.getDirectString("version");
		String title   = data.getDirectString("title");
		String code    = data.getDirectString("code");
		return new Language(path, name, version, title, code, new Translation(data));
	}
	
	public final String getPath() {
		return path;
	}
	
	public final String getName() {
		return name;
	}
	
	public final String getVersion() {
		return version;
	}
	
	public final String getTitle() {
		return title;
	}
	
	/** @since 00.02.02 */
	public final String getCode() {
		return code;
	}
	
	public final Translation getTranslation() {
		return translation;
	}
	
	@Override
	public final String toString() {
		return title;
	}
}