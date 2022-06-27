package sune.app.mediadown.message;

import java.net.URI;
import java.util.Objects;

/** @since 00.02.02 */
public final class Message {
	
	/** @since 00.02.05 */
	private final String list;
	/** @since 00.02.05 */
	private final URI uri;
	private final String file;
	private final MessageLanguage language;
	private final MessageOS os;
	private int version;
	
	public Message(String list, URI uri, String file, MessageLanguage language, MessageOS os, int version) {
		this.list = Objects.requireNonNull(list);
		this.uri = Objects.requireNonNull(uri);
		this.file = Objects.requireNonNull(file);
		this.language = Objects.requireNonNull(language);
		this.os = Objects.requireNonNull(os);
		this.version = checkVersion(version);
	}
	
	/** @since 00.02.05 */
	private static final int checkVersion(int version) {
		if(version < 0)
			throw new IllegalArgumentException("Invalid message version, must be >= 0");
		return version;
	}
	
	public void version(int newVersion) {
		version = checkVersion(newVersion);
	}
	
	/** @since 00.02.05 */
	public String list() {
		return list;
	}
	
	/** @since 00.02.05 */
	public URI uri() {
		return uri;
	}
	
	/** @since 00.02.05 */
	public String file() {
		return file;
	}
	
	/** @since 00.02.05 */
	public MessageLanguage language() {
		return language;
	}
	
	/** @since 00.02.05 */
	public MessageOS os() {
		return os;
	}
	
	public int version() {
		return version;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(file, language, list, os, uri, version);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		return Objects.equals(file, other.file)
		        && Objects.equals(language, other.language)
		        && Objects.equals(list, other.list)
		        && Objects.equals(os, other.os)
		        && Objects.equals(uri, other.uri)
		        && version == other.version;
	}
}