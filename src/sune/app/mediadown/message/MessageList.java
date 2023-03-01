package sune.app.mediadown.message;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sune.app.mediadown.net.Net;
import sune.app.mediadown.util.Utils;
import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;

/** @since 00.02.02 */
public final class MessageList {
	
	/** @since 00.02.05 */
	private static final String CURRENT_FORMAT = ParserV1.FORMAT;
	
	/** @since 00.02.05 */
	private static final Map<String, Parser> parsers = new HashMap<>();
	
	static {
		parsers.put(ParserV0.FORMAT, new ParserV0());
		parsers.put(ParserV1.FORMAT, new ParserV1());
	}
	
	/** @since 00.02.05 */
	private final URI baseURI;
	private final String version;
	private final SSDCollection data;
	
	private final Map<Message, Message> messages = new LinkedHashMap<>();
	private final Map<Message, Integer> indicies = new LinkedHashMap<>();
	
	// Constructor for the empty list
	protected MessageList(URI rootURI) throws Exception {
		this(Net.resolve(rootURI, "0000/"), "0000", emptyData());
	}
	
	protected MessageList(URI baseURI, String version, SSDCollection data) throws Exception {
		this.baseURI = Objects.requireNonNull(baseURI);
		this.version = Objects.requireNonNull(version);
		this.data = Objects.requireNonNull(data);
		this.parseData();
	}
	
	/** @since 00.02.05 */
	protected static final SSDCollection emptyData() {
		SSDCollection data = SSDCollection.empty();
		data.set("format", CURRENT_FORMAT);
		data.set("messages", SSDCollection.emptyArray());
		return data;
	}
	
	/** @since 00.02.05 */
	private static final Message newMessage(MessageList parent, String list, String file, MessageLanguage language,
			MessageOS os, int version) {
		URI uri = Net.resolve(parent.baseURI(), "../" + list + "/data/" + file);
		return new Message(list, uri, file, language, os, version);
	}
	
	/** @since 00.02.05 */
	private static final SSDNode arrayOf(MessageLanguage languages) {
		if(languages.isNone()) return SSDObject.ofRaw("NONE");
		if(languages.isAll())  return SSDObject.ofRaw("ALL");
		
		SSDCollection array = SSDCollection.emptyArray();
		languages.values().forEach(array::add);
		return array;
	}
	
	/** @since 00.02.05 */
	private static final SSDNode arrayOf(MessageOS os) {
		if(os.isNone()) return SSDObject.ofRaw("NONE");
		if(os.isAll())  return SSDObject.ofRaw("ALL");
		
		SSDCollection array = SSDCollection.emptyArray();
		os.values().forEach(array::add);
		return array;
	}
	
	/** @since 00.02.05 */
	// Helper method
	private static final void set(SSDCollection coll, String name, SSDNode node) {
		if(node.isCollection()) coll.set(name, (SSDCollection) node);
		else                    coll.set(name, (SSDObject)     node);
	}
	
	/** @since 00.02.05 */
	private static final SSDCollection itemize(SSDCollection item, MessageList parent, Message message) {
		item.set("file", message.file());
		set(item, "language", arrayOf(message.language()));
		set(item, "os", arrayOf(message.os()));
		item.set("list", message.list());
		item.set("version", message.version());
		return item;
	}
	
	/** @since 00.02.05 */
	private static final SSDCollection itemize(MessageList parent, Message message) {
		return itemize(SSDCollection.empty(), parent, message);
	}
	
	private static final List<Message> difference(Stream<Message> stream, MessageList other) {
		return stream.filter((message) -> {
			Message m = other.findMessage(message.list(), message.file());
			return m == null || m.version() < message.version();
		}).collect(Collectors.toList());
	}
	
	/** @since 00.02.05 */
	private static final Parser parser(String format) {
		return Optional.ofNullable(parsers.get(format)).orElseGet(MessageList::defaultParser);
	}
	
	/** @since 00.02.05 */
	private static final Parser defaultParser() {
		return parsers.get("v0");
	}
	
	/** @since 00.02.05 */
	private final SSDCollection dataMessages() {
		return data.getDirectCollection("messages");
	}
	
	/** @since 00.02.05 */
	private final void parseData() throws Exception {
		parser(data.getDirectString("format", ParserV0.FORMAT))
			.parseData(this)
			.forEach(this::addToCollections);
	}
	
	/** @since 00.02.05 */
	private final Message findMessage(String version, String file) {
		return messages.values().stream()
					.filter((m) -> m.list().equals(version) && m.file().equals(file))
					.findFirst().orElse(null);
	}
	
	/** @since 00.02.05 */
	private final Stream<Message> streamForLanguage(String language) {
		return messages.values().stream()
					.filter((m) -> m.language().has(language));
	}
	
	/** @since 00.02.05 */
	private final void reindex() {
		int index = 0;
		for(Message message : messages.values()) {
			indicies.put(message, index++);
		}
	}
	
	/** @since 00.02.05 */
	private final void addToCollections(Message message) {
		messages.put(message, message);
		indicies.put(message, indicies.size());
	}
	
	/** @since 00.02.05 */
	private final void remove(Message message, boolean reindex) {
		int index = Optional.ofNullable(indicies.get(message)).orElse(-1);
		if(index < 0) return; // Does not exist
		
		dataMessages().remove(index);
		messages.remove(message);
		indicies.remove(message);
		
		if(reindex) reindex();
	}
	
	public void add(Message message) {
		Message existing = findMessage(message.list(), message.file());
		int index = -1;
		if(existing != null) {
			if(existing.equals(message))
				return; // Already exists
			index = indicies.get(existing);
		}
		
		SSDCollection collMessages = dataMessages();
		if(index >= 0) { // Message updated
			SSDCollection item = collMessages.getCollection(index);
			itemize(item, this, message);
		} else { // New message
			collMessages.add(itemize(this, message));
		}
		
		addToCollections(message);
	}
	
	/** @since 00.02.05 */
	public void remove(Message message) {
		remove(message, true);
	}
	
	public void updateVersion(Message message, int newVersion) {
		message.version(newVersion);
		
		if(!messages.containsKey(message)) {
			add(message);
		} else {
			SSDCollection collMessages = dataMessages();
			SSDCollection item = collMessages.getCollection(indicies.get(message));
			item.set("version", newVersion);
		}
	}
	
	public List<Message> forLanguage(String language) {
		return streamForLanguage(language).collect(Collectors.toList());
	}
	
	public List<Message> difference(MessageList other) {
		return difference(Utils.stream(dataMessages().collectionsIterable())
							   .flatMap((item) -> forLanguage(item.getName()).stream()),
						  other);
	}
	
	public List<Message> difference(String language, MessageList other) {
		return difference(streamForLanguage(language), other);
	}
	
	public void reset() {
		data.setDirect("messages", SSDCollection.emptyArray());
		messages.clear();
		indicies.clear();
	}
	
	/** @since 00.02.05 */
	public URI baseURI() {
		return baseURI;
	}
	
	public String version() {
		return version;
	}
	
	public SSDCollection data() {
		return data;
	}
	
	/** @since 00.02.05 */
	public List<Message> messages() {
		return List.copyOf(messages.values());
	}
	
	/** @since 00.02.05 */
	private static final class ParserV1 implements Parser {
		
		private static final String FORMAT = "v1";
		
		private static final Message parse(MessageList parent, SSDCollection item) throws Exception {
			String file = item.getDirectString("file");
			
			MessageLanguage language = MessageLanguage.all(); // Default value
			SSDNode languageNode = item.getDirect("language", null); // Optional
			if(languageNode != null) {
				if(languageNode.isObject()) {
					String str = languageNode.toString();
					if(str.equals("ALL")) language = MessageLanguage.all();
					else                  language = MessageLanguage.none();
				} else if(languageNode.isCollection()) {
					language = MessageLanguage.of(Utils.stream(((SSDCollection) languageNode).objectsIterable())
													   .map(SSDObject::stringValue).toArray(String[]::new));
				}
			}
			
			MessageOS os = MessageOS.all(); // Default value
			SSDNode osNode = item.getDirect("os", null); // Optional
			if(osNode != null) {
				if(osNode.isObject()) {
					String str = osNode.toString();
					if(str.equals("ALL")) os = MessageOS.all();
					else                  os = MessageOS.none();
				} else if(osNode.isCollection()) {
					os = MessageOS.of(Utils.stream(((SSDCollection) osNode).objectsIterable())
										   .map(SSDObject::stringValue).toArray(String[]::new));
				}
			}
			
			String list = item.getDirectString("list", parent.version());
			int version = item.getDirectInt("version", 0);
			
			// If the message belongs to a different list, we have to obtain its actual version,
			// if requested (version = 0).
			if(!list.equals(parent.version()) && version == 0) {
				MessageList other = MessageManager.ofVersion(list);
				version = Optional.ofNullable(other.findMessage(list, file)).map(Message::version).orElse(0);
			}
			
			return newMessage(parent, list, file, language, os, version);
		}
		
		@Override
		public List<Message> parseData(MessageList list) throws Exception {
			List<Message> messages = new ArrayList<>();
			for(SSDCollection m : list.data.getDirectCollection("messages").collectionsIterable()) {
				messages.add(parse(list, m));
			}
			return messages;
		}
	}
	
	/** @since 00.02.05 */
	private static final class ParserV0 implements Parser {
		
		private static final String FORMAT = "v0";
		
		private static final Message parse(MessageList parent, String language, SSDCollection item) throws Exception {
			String file = item.getDirectString("file");
			MessageLanguage lang = MessageLanguage.of(language);
			
			MessageOS os = MessageOS.all(); // Default value
			SSDNode osNode = item.getDirect("os", null); // Optional
			if(osNode != null && osNode.isCollection()) {
				Set<String> set = new HashSet<>();
				for(SSDObject o : ((SSDCollection) osNode).objectsIterable()) {
					set.add(o.stringValue());
				}
				os = MessageOS.of(set.toArray(String[]::new));
			}
			
			String list = parent.version();
			int version = item.getDirectInt("version", 0);
			
			// If the message belongs to a different list, we have to obtain its actual version,
			// if requested (version = 0).
			if(!list.equals(parent.version()) && version == 0) {
				MessageList other = MessageManager.ofVersion(list);
				version = other.findMessage(list, file).version();
			}
			
			return newMessage(parent, list, file, lang, os, version);
		}
		
		@Override
		public List<Message> parseData(MessageList list) throws Exception {
			List<Message> messages = new ArrayList<>();
			for(SSDCollection l : list.data.collectionsIterable()) {
				String language = l.getName();
				for(SSDCollection m : l.collectionsIterable()) {
					messages.add(parse(list, language, m));
				}
			}
			return messages;
		}
	}
	
	/** @since 00.02.05 */
	private static interface Parser {
		
		List<Message> parseData(MessageList list) throws Exception;
	}
}