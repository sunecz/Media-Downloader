package sune.app.mediadown.update;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.util.NIO;

/** @since 00.02.09 */
public class ArtifactChecker implements EventBindable<ArtifactCheckEvent> {
	
	protected final Map<DigestType, MessageDigest> mds = new HashMap<>();
	protected final ByteBuffer buf = ByteBuffer.allocateDirect(8192);
	protected final EventRegistry<ArtifactCheckEvent> eventRegistry = new EventRegistry<>();
	
	protected final Path root;
	protected Context context;
	
	public ArtifactChecker(Path root) {
		this.root = Objects.requireNonNull(root);
		this.context = new Context();
	}
	
	protected MessageDigest messageDigestOf(Digest digest) throws IOException {
		try {
			return mds.computeIfAbsent(digest.type(), (type) -> {
				try {
					return MessageDigest.getInstance(type.algorithm());
				} catch(NoSuchAlgorithmException ex) {
					throw new RuntimeException(ex);
				}
			});
		} catch(RuntimeException ex) {
			throw new IOException(ex.getCause());
		}
	}
	
	protected byte[] computeHash(Path path, MessageDigest md) throws IOException {
		try(FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			while(channel.read(buf) != -1) {
				buf.flip();
				md.update(buf);
				buf.clear();
			}
			
			return md.digest();
		}
	}
	
	protected ArtifactCheckResult doCheck(Artifact artifact) throws IOException {
		Path path = root.resolve(artifact.installPath());
		
		if(!NIO.isRegularFile(path)) {
			return ArtifactCheckResult.MISSING;
		}
		
		Digest digest = artifact.digest();
		byte[] remoteDigest = computeHash(path, messageDigestOf(digest));
		
		if(!Arrays.equals(digest.value(), remoteDigest)) {
			return ArtifactCheckResult.MISMATCH;
		}
		
		return ArtifactCheckResult.OK;
	}
	
	protected ArtifactCheckResult check(Artifact artifact) throws IOException {
		context.begin(artifact);
		eventRegistry.call(ArtifactCheckEvent.BEGIN, context);
		
		try {
			ArtifactCheckResult result = doCheck(artifact);
			context.end(result);
			return result;
		} catch(IOException ex) {
			context.end(ex);
			eventRegistry.call(ArtifactCheckEvent.ERROR, context);
			throw ex; // Rethrow
		} finally {
			eventRegistry.call(ArtifactCheckEvent.END, context);
		}
	}
	
	@Override
	public <V> void addEventListener(
		Event<? extends ArtifactCheckEvent, V> event,
		Listener<V> listener
	) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(
		Event<? extends ArtifactCheckEvent, V> event,
		Listener<V> listener
	) {
		eventRegistry.remove(event, listener);
	}
	
	protected class Context implements ArtifactCheckContext {
		
		protected Artifact artifact;
		protected ArtifactCheckResult result;
		protected IOException exception;
		
		protected void begin(Artifact artifact) {
			this.artifact = artifact;
			this.result = null;
			this.exception = null;
		}
		
		protected void end(ArtifactCheckResult result) {
			this.result = result;
		}
		
		protected void end(IOException exception) {
			this.exception = exception;
		}
		
		@Override public Artifact artifact() { return artifact; }
		@Override public ArtifactCheckResult result() { return result; }
		@Override public IOException exception() { return exception; }
		@Override public Path root() { return root; }
	}
}
