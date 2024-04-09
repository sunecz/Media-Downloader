package sune.app.mediadown.tor;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;

import sune.api.process.Processes;
import sune.api.process.ReadOnlyProcess;
import sune.app.mediadown.InternalState;
import sune.app.mediadown.TaskStates;
import sune.app.mediadown.concurrent.StateMutex;
import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.util.Regex;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class TorProcess implements AutoCloseable {
	
	private static final int PORT_UNKNOWN = -1;
	
	private final ReadOnlyProcess process;
	private final InternalState state = new InternalState(TaskStates.INITIAL);
	private final StateMutex mtxInitialized = new StateMutex();
	
	private TorConfigurationFile configFile;
	private volatile int portControl = PORT_UNKNOWN;
	private volatile int portHttp = PORT_UNKNOWN;
	private volatile int portSocks = PORT_UNKNOWN;
	
	private volatile boolean collectOutput = false;
	private final VarLoader<StringBuilder> output = VarLoader.of(StringBuilder::new);
	
	private final VarLoader<TorControl> control = VarLoader.of(this::createControl);
	
	private TorProcess() {
		process = Processes.createAsynchronous(Tor.path(), this::readLine);
	}
	
	private static final int extractPort(String line) {
		Matcher matcher = Regex.of("on .*?:(\\d+)").matcher(line);
		
		if(matcher.find()) {
			return Integer.valueOf(matcher.group(1));
		}
		
		return PORT_UNKNOWN;
	}
	
	public static final TorProcess create() {
		return new TorProcess();
	}
	
	private final void readLine(String line) {
		if(line == null) {
			return;
		}
		
		if(line.contains("Bootstrapped 100% (done)")) {
			mtxInitialized.unlock();
		} else if(line.contains("Opened Control listener connection (ready)")) {
			portControl = extractPort(line);
		} else if(line.contains("Opened HTTP tunnel listener connection (ready)")) {
			portHttp = extractPort(line);
		} else if(line.contains("Opened Socks listener connection (ready)")) {
			portSocks = extractPort(line);
		}
		
		if(collectOutput) {
			output.value().append(line);
		}
	}
	
	private final TorControl createControl() {
		return new TorControl(portControl);
	}
	
	public String hashPassword(String password) throws Exception {
		if(state.is(TaskStates.STARTED)) {
			return null;
		}
		
		state.clear(TaskStates.STARTED);
		collectOutput = true;
		
		try {
			String command = Utils.format(
				"--quiet --hash-password \"%{password}s\"",
				"password", password
			);
			
			process.execute(command);
			
			if(process.waitFor() != 0) {
				return null;
			}
			
			return output.value().toString();
		} catch(Exception ex) {
			state.set(TaskStates.ERROR);
			throw ex; // Rethrow
		}
	}
	
	public void start(TorConfigurationFile configurationFile) throws Exception {
		if(state.is(TaskStates.STARTED)) {
			return;
		}
		
		configFile = Objects.requireNonNull(configurationFile);
		state.clear(TaskStates.STARTED);
		
		try {
			// First, ensure that the configuration file is written to disk
			configFile.write();
			
			String command = Utils.format(
				"--torrc-file \"%{torrc_path}s\"",
				"torrc_path", configFile.path().toAbsolutePath().toString()
			);
			
			process.execute(command);
		} catch(Exception ex) {
			state.set(TaskStates.ERROR);
			throw ex; // Rethrow
		}
	}
	
	public void waitForInitialized() throws Exception {
		mtxInitialized.await();
	}
	
	public int waitFor() throws Exception {
		try {
			int code = process.waitFor();
			state.set(TaskStates.DONE);
			return code;
		} catch(Exception ex) {
			state.set(TaskStates.ERROR);
			throw ex; // Rethrow
		}
	}
	
	public TorControl control() throws IOException {
		TorControl tc = control.value();
		
		if(!tc.isOpen()) {
			tc.open();
		}
		
		return tc;
	}
	
	public void collectOutput(boolean collectOutput) {
		this.collectOutput = collectOutput;
	}
	
	public String output() {
		if(!collectOutput || output.isUnset()) {
			return null;
		}
		
		return output.value().toString();
	}
	
	@Override
	public void close() throws Exception {
		try {
			process.close();
		} finally {
			if(!state.is(TaskStates.DONE)) {
				state.set(TaskStates.STOPPED);
			}
		}
	}
	
	public int controlPort() {
		return portControl;
	}
	
	public int httpPort() {
		return portHttp;
	}
	
	public int socksPort() {
		return portSocks;
	}
}