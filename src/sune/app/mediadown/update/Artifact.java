package sune.app.mediadown.update;

import java.net.URI;
import java.util.Objects;

/** @since 00.02.09 */
public final class Artifact {
	
	private final ComponentRegistry registry;
	private final String component;
	private final String version;
	private final URI uri;
	private final String installPath;
	private final Digest digest;
	private final Encoding encoding;
	private final long size;
	private final long encodedSize;
	private final boolean executable;
	
	public Artifact(
		ComponentRegistry registry,
		String component,
		String version,
		URI uri,
		String installPath,
		Digest digest,
		Encoding encoding,
		long size,
		long encodedSize,
		boolean executable
	) {
		this.registry = Objects.requireNonNull(registry);
		this.component = Objects.requireNonNull(component);
		this.version = Objects.requireNonNull(version);
		this.uri = Objects.requireNonNull(uri);
		this.installPath = Objects.requireNonNull(installPath);
		this.digest = Objects.requireNonNull(digest);
		this.encoding = Objects.requireNonNull(encoding);
		this.size = size;
		this.encodedSize = encodedSize;
		this.executable = executable;
	}
	
	public ComponentRegistry registry() { return registry; }
	public String component() { return component; }
	public String version() { return version; }
	public URI uri() { return uri; }
	public String installPath() { return installPath; }
	public Digest digest() { return digest; }
	public Encoding encoding() { return encoding; }
	public long size() { return size; }
	public long encodedSize() { return encodedSize; }
	public boolean executable() { return executable; }
}
