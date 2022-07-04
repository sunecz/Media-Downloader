package sune.app.mediadown.message;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.Shared;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.PathSystem;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Web;
import sune.app.mediadown.util.Web.GetRequest;
import sune.app.mediadown.util.Web.StreamResponse;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDF;

/** @since 00.02.02 */
public final class MessageManager {
	
	private static final Path PATH_LOCAL = PathSystem.getPath("resources/messages.ssdf");
	private static final URI  URI_BASE   = Utils.uri("https://app.sune.tech/mediadown/msg/");
	
	private static MessageList EMPTY;
	private static MessageList LOCAL;
	
	// Forbid anyone to create an instance of this class
	private MessageManager() {
	}
	
	public static final MessageList empty() {
		return EMPTY == null ? (EMPTY = Utils.ignore(() -> new MessageList(URI_BASE))) : EMPTY;
	}
	
	/** @since 00.02.05 */
	public static final MessageList ofVersion(String version) throws Exception {
		return MessageListObtainer.ofVersion(version).list();
	}
	
	public static final MessageList current() throws Exception {
		return ofVersion(MediaDownloader.version().string());
	}
	
	public static final MessageList local() throws Exception {
		return LOCAL == null ? (LOCAL = MessageListObtainer.local().list()) : LOCAL;
	}
	
	public static final void updateLocal() throws Exception {
		NIO.save(PATH_LOCAL, local().data().toString(false));
	}
	
	public static final void deleteLocal() throws Exception {
		local().reset();
		NIO.deleteFile(PATH_LOCAL);
	}
	
	/** @since 00.02.05 */
	private static final class MessageListObtainer {
		
		private static final Map<String, MessageListObtainer> cache = new HashMap<>();
		
		private MessageList list;
		
		private MessageListObtainer(MessageList list) {
			this.list = Objects.requireNonNull(list);
		}
		
		private static final URI versionBaseURI(String version) {
			return URI_BASE.resolve(version + '/');
		}
		
		public static final MessageListObtainer ofVersion(String version) throws Exception {
			MessageListObtainer obtainer;
			if((obtainer = cache.get(version)) != null)
				return obtainer;
			
			SSDCollection data = MessageList.emptyData();
			URI baseURI = versionBaseURI(version);
			URI uri = baseURI.resolve("list");
			try(StreamResponse response = Web.requestStream(new GetRequest(Utils.url(uri), Shared.USER_AGENT))) {
				data = SSDF.read(response.stream);
			}
			
			obtainer = new MessageListObtainer(new MessageList(baseURI, version, data));
			cache.put(version, obtainer);
			return obtainer;
		}
		
		public static final MessageListObtainer local() throws Exception {
			MessageListObtainer obtainer;
			if((obtainer = cache.get(null)) != null)
				return obtainer;
			
			SSDCollection data = MessageList.emptyData();
			if(NIO.exists(PATH_LOCAL)) {
				try(InputStream stream = Files.newInputStream(PATH_LOCAL, StandardOpenOption.READ)) {
					data = SSDF.read(stream);
				}
			}
			
			String version = MediaDownloader.version().string();
			URI baseURI = versionBaseURI(version);
			obtainer = new MessageListObtainer(new MessageList(baseURI, version, data));
			cache.put(null, obtainer);
			return obtainer;
		}
		
		public MessageList list() throws Exception {
			return list;
		}
	}
}