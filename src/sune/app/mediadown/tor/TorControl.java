package sune.app.mediadown.tor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import sune.app.mediadown.concurrent.StateMutex;
import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.util.Ref;
import sune.app.mediadown.util.Regex;

/** @since 00.02.09 */
public final class TorControl implements AutoCloseable {
	
	// Reference: https://github.com/torproject/torspec/blob/main/control-spec.txt
	
	private static final Charset CHARSET = StandardCharsets.UTF_8;
	private static final int PORT_MIN = 1;
	private static final int PORT_MAX = 65535;
	private static final char[] CRLF = { '\r', '\n' };
	private static final int ID_UNKNOWN = -1;
	
	private static final Regex regexNodeIp = Regex.of("^r [^ ]+ [^ ]+ [^ ]+ [^ ]+ [^ ]+ (?<ip>[^ ]+)");
	
	private final int port;
	private volatile Thread threadInput;
	private volatile Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private final AtomicBoolean opening = new AtomicBoolean();
	private volatile boolean open;
	
	private final StateMutex mtxRead = new StateMutex();
	private final VarLoader<BlockingQueue<String>> input = VarLoader.of(LinkedBlockingQueue::new);
	private volatile boolean readClosed = false;
	private volatile IOException readException = null;
	
	private final Map<String, EventListener> listeners = new ConcurrentHashMap<>();
	private volatile boolean listenForStreamEvents;
	
	public TorControl(int port) {
		this.port = checkPort(port);
	}
	
	private static final int checkPort(int port) {
		if(port < PORT_MIN || port > PORT_MAX) {
			throw new IllegalArgumentException("Invalid port");
		}
		
		return port;
	}
	
	private final void readLoop() {
		while(!Thread.currentThread().isInterrupted()) {
			try {
				String line = reader.readLine();
				
				if(line == null) {
					break; // EOF
				}
				
				for(EventListener listener : listeners.values()) {
					if(!listener.canAccept(line)) {
						continue;
					}
					
					listener.accept(line);
				}
				
				input.value().add(line);
			} catch(IOException ex) {
				readException = ex;
				break; // Do not continue
			} finally {
				mtxRead.unlock();
			}
		}
		
		readClosed = true;
	}
	
	private final String readLine() throws IOException {
		if(readClosed || input.isUnset()) {
			// Check for exception that may have had happened during the reading
			if(readException != null) {
				throw readException;
			}
			
			return null;
		}
		
		BlockingQueue<String> lines = input.value();
		
		try {
			return lines.poll(200L, TimeUnit.MILLISECONDS);
		} catch(InterruptedException ex) {
			throw new IOException(ex);
		}
	}
	
	private final List<String> readInputFully() throws IOException {
		// Wait for the first data, so that we don't exit immediately
		mtxRead.awaitAndReset();
		
		List<String> lines = new ArrayList<>();
		boolean statusLine = false;
		
		for(String line; (line = readLine()) != null;) {
			// Since lines can mix together, we must filter the stream event lines
			if(listenForStreamEvents
					&& StreamEventListener.isStreamEventLine(line)) {
				continue;
			}
			
			lines.add(line);
			
			if(statusLine) {
				break;
			} else if(line.equals(".")) {
				// Next line is the status line
				statusLine = true;
			}
		}
		
		return lines;
	}
	
	private final List<String> command(String command) throws IOException {
		writer.write(command);
		writer.write(CRLF);
		writer.flush();
		return readInputFully();
	}
	
	private final String commandFirstResponse(String command) throws IOException {
		return command(command).get(0);
	}
	
	private final int responseCode(String response) {
		String value = response;
		
		int index;
		if((index = value.indexOf(' ')) > 0) {
			value = value.substring(0, index);
		}
		
		return Integer.valueOf(value);
	}
	
	private final int responseCode(List<String> response) {
		return responseCode(response.get(response.size() - 1));
	}
	
	private final String responseValue(String response, String command) {
		Regex regex = Regex.of("^(?<code>\\d+)-" + Regex.quote(command) + "=(?<value>.*?)$");
		
		Matcher matcher;
		if(!(matcher = regex.matcher(response)).matches()) {
			return null;
		}
		
		return matcher.group("value");
	}
	
	private final void addEventListener(EventListener listener) {
		Objects.requireNonNull(listener);
		listeners.put(listener.name(), listener);
	}
	
	private final <T extends EventListener> T eventListenerOf(String name) {
		@SuppressWarnings("unchecked")
		T listener = (T) listeners.get(name);
		return listener;
	}
	
	private final int circuitId(int streamId) {
		StreamEventListener listener = eventListenerOf(StreamEventListener.NAME);
		
		if(listener == null) {
			return ID_UNKNOWN;
		}
		
		return listener.circuitId(streamId);
	}
	
	public final int lastStreamId() {
		StreamEventListener listener = eventListenerOf(StreamEventListener.NAME);
		
		if(listener == null) {
			return ID_UNKNOWN;
		}
		
		return listener.lastStreamId();
	}
	
	public void open() throws IOException {
		if(!opening.compareAndSet(false, true)) {
			return;
		}
		
		socket = new Socket(InetAddress.getByName(null), port);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), CHARSET));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), CHARSET));
		
		threadInput = new Thread(this::readLoop);
		threadInput.setDaemon(true);
		threadInput.start();
		
		open = true;
		opening.set(false);
	}
	
	public boolean authenticate(String password) throws IOException {
		String response = commandFirstResponse("AUTHENTICATE \"" + password + "\"");
		return responseCode(response) == 250;
	}
	
	public boolean listenForStreamEvents() throws IOException {
		String response = commandFirstResponse("SETEVENTS STREAM");
		if(responseCode(response) != 250) {
			return false;
		}
		
		addEventListener(new StreamEventListener());
		listenForStreamEvents = true;
		return true;
	}
	
	public int circuitId() {
		return circuitId(lastStreamId());
	}
	
	public List<TorCircuit> circuits() throws IOException {
		List<String> response = command("GETINFO circuit-status");
		
		if(responseCode(response) != 250) {
			return null;
		}
		
		List<TorCircuit> circuits = new ArrayList<>(response.size());
		
		for(String line : response) {
			TorCircuit circuit = TorCircuit.parse(line);
			
			if(circuit != null) {
				circuits.add(circuit);
			}
		}
		
		return circuits;
	}
	
	public TorCircuit circuit(int circuitId) throws IOException {
		List<String> response = command("GETINFO circuit-status");
		
		if(responseCode(response) != 250) {
			return null;
		}
		
		for(String line : response) {
			TorCircuit circuit = TorCircuit.parse(line);
			
			if(circuit != null && circuit.id() == circuitId) {
				return circuit;
			}
		}
		
		return null;
	}
	
	public TorCircuit circuit() throws IOException {
		TorCircuit circuit = circuit(circuitId());
		
		if(circuit == null || !circuit.status().equals("BUILT")) {
			return null;
		}
		
		return circuit;
	}
	
	public String nodeIp(TorCircuit.Node node) throws IOException {
		List<String> response = command("GETINFO ns/id/" + node.identity());
		
		for(String line : response) {
			Matcher matcher;
			if((matcher = regexNodeIp.matcher(line)).find()) {
				return matcher.group("ip");
			}
		}
		
		return null;
	}
	
	public String nodeCountry(TorCircuit.Node node) throws IOException {
		String ip = nodeIp(node);
		
		if(ip == null) {
			return null;
		}
		
		String command = "ip-to-country/" + ip;
		List<String> response = command("GETINFO " + command);
		
		if(responseCode(response) != 250) {
			return null;
		}
		
		String country = responseValue(response.get(0), command);
		return country == null ? null : country.toUpperCase();
	}
	
	public TorCircuit.Node nodeAt(int index) throws IOException {
		TorCircuit circuit = circuit();
		
		if(circuit == null) {
			return null;
		}
		
		List<TorCircuit.Node> nodes = circuit.nodes();
		
		if(index < 0) {
			index = nodes.size() + index;
		}
		
		return nodes.get(index);
	}
	
	public TorCircuit.Node entryNode() throws IOException {
		return nodeAt(0);
	}
	
	public TorCircuit.Node exitNode() throws IOException {
		return nodeAt(-1);
	}
	
	public String ipAddress() throws IOException {
		TorCircuit.Node exitNode = exitNode();
		
		if(exitNode == null) {
			return null;
		}
		
		return nodeIp(exitNode);
	}
	
	public String country() throws IOException {
		TorCircuit.Node exitNode = exitNode();
		
		if(exitNode == null) {
			return null;
		}
		
		return nodeCountry(exitNode);
	}
	
	public boolean newCircuit() throws IOException {
		String response = commandFirstResponse("SIGNAL NEWNYM");
		return responseCode(response) == 250;
	}
	
	private static final int REASON_DESTROY = 5;
	
	public boolean closeStream(int streamId) throws IOException {
		return closeStream(streamId, REASON_DESTROY);
	}
	
	public boolean closeStream(int streamId, int reason) throws IOException {
		String response = commandFirstResponse(String.format("CLOSESTREAM %d %d", streamId, reason));
		return responseCode(response) == 250;
	}
	
	public List<TorStream> streams() {
		StreamEventListener listener = eventListenerOf(StreamEventListener.NAME);
		
		if(listener == null) {
			return null;
		}
		
		return listener.streams();
	}
	
	public TorStream stream(int streamId) {
		StreamEventListener listener = eventListenerOf(StreamEventListener.NAME);
		
		if(listener == null) {
			return null;
		}
		
		return listener.stream(streamId);
	}
	
	public boolean closeCircuit() throws IOException {
		return closeCircuit(circuitId());
	}
	
	public boolean closeCircuit(int circuitId) throws IOException {
		String response = commandFirstResponse(String.format("CLOSECIRCUIT %d", circuitId));
		return responseCode(response) == 250;
	}
	
	private final StateMutex mtxCircuitConnection = new StateMutex(true);
	private final VarLoader<ConnectionWaiter> connectionWaiter = VarLoader.of(this::newConnectionWaiter);
	
	public TorCircuitConnection newCircuitConnection() throws IOException {
		TorCircuitConnection cc = new TorCircuitConnection(connectionWaiter.value());
		
		if(!cc.acquire()) {
			throw new IOException("Unable to acquire circuit connection");
		}
		
		return cc;
	}
	
	private final ConnectionWaiter newConnectionWaiter() {
		return new ConnectionWaiter(this);
	}
	
	private static final class ConnectionWaiter {
		
		private final TorControl control;
		private final Ref.Mutable<Integer> circuitId = new Ref.Mutable<>(ID_UNKNOWN);
		private final StateMutex mtxConnection = new StateMutex();
		private final Consumer<Integer> ncl = this::listener;
		
		public ConnectionWaiter(TorControl control) {
			this.control = control;
		}
		
		private final StreamEventListener streamListener() {
			return control.eventListenerOf(StreamEventListener.NAME);
		}
		
		private final void listener(int cid) {
			circuitId.set(cid);
			mtxConnection.unlock();
		}
		
		public int await() {
			StreamEventListener listener;
			if((listener = streamListener()) == null) {
				return ID_UNKNOWN;
			}
			
			mtxConnection.awaitAndReset();
			listener.removeNewCircuitListener(ncl);
			return circuitId.get();
		}
		
		public boolean acquire() {
			control.mtxCircuitConnection.awaitAndReset();
			
			StreamEventListener listener;
			if((listener = streamListener()) == null) {
				return false;
			}
			
			circuitId.set(ID_UNKNOWN);
			listener.addNewCircuitListener(ncl);
			return true;
		}
		
		public boolean release() throws IOException {
			StreamEventListener listener;
			if((listener = streamListener()) == null) {
				return false;
			}
			
			listener.forgetCircuit(circuitId.get());
			control.mtxCircuitConnection.unlockOne();
			return true;
		}
	}
	
	public static final class TorCircuitConnection implements AutoCloseable {
		
		private final ConnectionWaiter waiter;
		
		private TorCircuitConnection(ConnectionWaiter waiter) {
			this.waiter = waiter;
		}
		
		public final int await() throws IOException {
			return waiter.await();
		}
		
		public boolean acquire() throws IOException {
			return waiter.acquire();
		}
		
		public boolean release() throws IOException {
			return waiter.release();
		}
		
		@Override
		public void close() throws Exception {
			release();
		}
	}
	
	@Override
	public void close() throws Exception {
		if(threadInput != null) {
			threadInput.interrupt();
			threadInput.join();
		}
		
		if(socket != null) {
			socket.close();
		}
	}
	
	public boolean isOpen() {
		return open;
	}
	
	private static interface EventListener {
		
		boolean canAccept(String line);
		void accept(String line);
		String name();
	}
	
	private static final class StreamEventListener implements EventListener {
		
		private static final String NAME = "STREAM";
		
		// Tor control-spec.txt, section 4.1.2.
		private static final Regex regexStatusChanged = Regex.of(
			"^650 STREAM (?<streamId>\\d+) (?<state>[^\\s]+) (?<circuitId>\\d+) (?<target>[^\\s]+)(?: (?<attributes>.*?))?$"
		);
		
		private final Map<Integer, TorStream> streams = new HashMap<>();
		private volatile int lastStreamId = ID_UNKNOWN;
		
		private final Set<Integer> circuitIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
		private final Deque<Consumer<Integer>> listenersNewCircuit = new ConcurrentLinkedDeque<>();
		
		public static final boolean isStreamEventLine(String line) {
			return line.startsWith("650 STREAM");
		}
		
		@Override
		public boolean canAccept(String line) {
			return isStreamEventLine(line);
		}
		
		@Override
		public void accept(String line) {
			Matcher matcher;
			if(!(matcher = regexStatusChanged.matcher(line)).find()) {
				return;
			}
			
			int streamId = Integer.valueOf(matcher.group("streamId"));
			TorStream.Status status = TorStream.Status.of(matcher.group("state"));
			int circuitId = Integer.valueOf(matcher.group("circuitId"));
			String target = matcher.group("target");
			String strAttributes = matcher.group("attributes");
			Map<String, String> attributes = null;
			
			if(strAttributes != null && !strAttributes.isEmpty()) {
				attributes = new HashMap<>();
				
				for(int i = 0, l = strAttributes.length(); i < l;) {
					int idx = strAttributes.indexOf(' ', i);
					if(idx < 0) idx = strAttributes.length();
					String strAttribute = strAttributes.substring(i, idx);
					String[] parts = strAttribute.split("=", 2);
					String name = parts[0];
					String value = parts.length < 2 ? null : parts[1];
					attributes.put(name, value);
					i = idx + 1;
				}
			}
			
			TorStream stream = streams.get(streamId);
			
			if(stream == null) {
				stream = TorStream.create(streamId, status, circuitId, target);
				stream.attributes(attributes);
				streams.put(streamId, stream);
			} else {
				stream.status(status);
				stream.circuitId(circuitId);
				stream.addTarget(target);
				
				if(attributes != null) {
					stream.addAttributes(attributes);
				}
			}
			
			if(circuitIds.add(circuitId)) {
				for(Consumer<Integer> listener : listenersNewCircuit) {
					listener.accept(circuitId);
				}
			}
			
			lastStreamId = streamId;
		}
		
		public void addNewCircuitListener(Consumer<Integer> listener) {
			listenersNewCircuit.add(Objects.requireNonNull(listener));
		}
		
		public void removeNewCircuitListener(Consumer<Integer> listener) {
			listenersNewCircuit.remove(listener);
		}
		
		public void forgetCircuit(int circuitId) {
			circuitIds.remove(circuitId);
		}
		
		public List<TorStream> streams() {
			return List.copyOf(streams.values());
		}
		
		public TorStream stream(int streamId) {
			return streams.get(streamId);
		}
		
		public int circuitId(int streamId) {
			TorStream stream = stream(streamId);
			return stream == null ? ID_UNKNOWN : stream.circuitId();
		}
		
		public int lastStreamId() {
			return lastStreamId;
		}
		
		public String name() {
			return NAME;
		}
	}
}