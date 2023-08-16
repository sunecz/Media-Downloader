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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import sune.app.mediadown.concurrent.StateMutex;
import sune.app.mediadown.concurrent.VarLoader;
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
	
	private final int lastStreamId() {
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
	
	public TorCircuit circuit() throws IOException {
		List<TorCircuit> circuits = circuits();
		
		if(circuits == null) {
			return null;
		}
		
		TorCircuit current = null;
		int circuitId = circuitId();
		
		for(TorCircuit circuit : circuits) {
			if(!circuit.status().equals("BUILT") || circuit.id() != circuitId) {
				continue;
			}
			
			current = circuit;
			break;
		}
		
		return current;
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
		
		private static final Regex regexCanAccept = Regex.of("^\\d+ STREAM");
		private static final Regex regexConnect = Regex.of("^\\d+ STREAM (?<streamId>\\d+) SENTCONNECT (?<circuitId>\\d+)");
		
		private final Map<Integer, Integer> streamIdToCircuitId = new ConcurrentHashMap<>();
		private volatile int lastStreamId = ID_UNKNOWN;
		
		@Override
		public boolean canAccept(String line) {
			return regexCanAccept.matcher(line).find();
		}
		
		@Override
		public void accept(String line) {
			Matcher matcher;
			if(!(matcher = regexConnect.matcher(line)).find()) {
				return;
			}
			
			int streamId = Integer.valueOf(matcher.group("streamId"));
			int circuitId = Integer.valueOf(matcher.group("circuitId"));
			streamIdToCircuitId.put(streamId, circuitId);
			lastStreamId = streamId;
		}
		
		public int circuitId(int streamId) {
			return streamIdToCircuitId.getOrDefault(streamId, ID_UNKNOWN);
		}
		
		public int lastStreamId() {
			return lastStreamId;
		}
		
		public String name() {
			return NAME;
		}
	}
}