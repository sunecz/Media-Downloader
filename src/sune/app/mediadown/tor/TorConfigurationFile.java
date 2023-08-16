package sune.app.mediadown.tor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.util.NIO;

/** @since 00.02.09 */
public final class TorConfigurationFile {
	
	private static final StandardOpenOption[] options = new StandardOpenOption[] {
		StandardOpenOption.WRITE,
		StandardOpenOption.TRUNCATE_EXISTING,
		StandardOpenOption.CREATE,
	};
	
	private static final VarLoader<TorConfigurationFile> empty = VarLoader.of(TorConfigurationFile::createEmpty);
	
	private final Path path;
	private final TorConfiguration configuration;
	
	private TorConfigurationFile(Path path, TorConfiguration configuration) {
		this.path = path;
		this.configuration = Objects.requireNonNull(configuration);
	}
	
	private static final TorConfigurationFile createEmpty() {
		return new TorConfigurationFile(null, TorConfiguration.empty());
	}
	
	public static final TorConfigurationFile of(Path path, TorConfiguration configuration) {
		return new TorConfigurationFile(Objects.requireNonNull(path), configuration);
	}
	
	public static final TorConfigurationFile ofTemporary(TorConfiguration configuration) throws IOException {
		return new TorConfigurationFile(NIO.tempFile("torrc", ""), configuration);
	}
	
	public static final TorConfigurationFile empty() {
		return empty.value();
	}
	
	public void write() throws IOException {
		try(BufferedWriter writer = Files.newBufferedWriter(path, options)) {
			configuration.write(writer);
		}
	}
	
	public Path path() {
		return path;
	}
	
	public TorConfiguration configuration() {
		return configuration;
	}
}