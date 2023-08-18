package sune.app.mediadown.tor;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import sune.app.mediadown.net.Web.Proxy;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class TorConnection implements AutoCloseable {
	
	private final TorConfiguration configuration;
	private TorProcess process;
	private TorControl control;
	
	private final AtomicBoolean opening = new AtomicBoolean();
	private volatile boolean open;
	
	private InetSocketAddress address;
	private String requestAuth;
	private Proxy proxy;
	
	private TorConnection(TorConfiguration configuration) {
		this.configuration = Objects.requireNonNull(configuration);
	}
	
	public static final TorConfiguration.Builder defaultConfiguration() {
		return TorConfiguration.builder()
			.set(TorConfiguration.Options.ClientOnly)
			.set(TorConfiguration.Options.GeoIPFile.ofValue(Tor.path(TorResource.GEO_IPV4).toString()))
			.set(TorConfiguration.Options.GeoIPv6File.ofValue(Tor.path(TorResource.GEO_IPV6).toString()))
			.set(TorConfiguration.Options.ControlPort.ofValue("auto"))
			.set(TorConfiguration.Options.SocksPort.ofValue("0"))
			.set(TorConfiguration.Options.HTTPTunnelPort.ofValue("auto"))
			.set(TorConfiguration.Options.HardwareAccel);
	}
	
	public static final TorConnection of(TorConfiguration configuration) {
		return new TorConnection(configuration);
	}
	
	public static final TorConnection ofDefault() {
		return builder().build();
	}
	
	public static final Builder builder() {
		return builder(defaultConfiguration());
	}
	
	public static final Builder builder(TorConfiguration.Builder configuration) {
		return new Builder(configuration);
	}
	
	private final String newRequestAuth() {
		return Utils.randomString(16) + ':' + Utils.randomString(16);
	}
	
	public void open() throws Exception {
		open(null);
	}
	
	public void open(String password) throws Exception {
		if(!opening.compareAndSet(false, true)) {
			return;
		}
		
		process = Tor.createProcess();
		process.start(TorConfigurationFile.ofTemporary(configuration));
		process.waitForInitialized();
		control = process.control();
		
		if(password != null && !control.authenticate(password)) {
			throw new IllegalStateException("Failed to authenticate the access to Tor control");
		}
		
		if(!control.listenForStreamEvents()) {
			throw new IllegalStateException("Failed to listen for stream events");
		}
		
		address = InetSocketAddress.createUnresolved("127.0.0.1", process.httpPort());
		requestAuth = newRequestAuth();
		proxy = null;
		
		open = true;
		opening.set(false);
	}
	
	public void newCircuit() throws Exception {
		if(!control.newCircuit()) {
			throw new IllegalStateException("Failed to obtain a new circuit");
		}
		
		// Use different authentication for the proxy to not reuse streams. For more information,
		// see the following links:
		// https://manpages.debian.org/stretch-backports/tor/torrc.5.en.html (SocksPort, IsolateSOCKSAuth)
		requestAuth = newRequestAuth();
		proxy = null;
	}
	
	public TorConfiguration configuration() {
		return configuration;
	}
	
	public TorProcess process() {
		return process;
	}
	
	public TorControl control() {
		return control;
	}
	
	public Proxy proxy() {
		if(proxy == null) {
			proxy = Proxy.of(address, requestAuth);
		}
		
		return proxy;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	@Override
	public void close() throws Exception {
		if(process != null) {
			process.close();
		}
	}
	
	public static final class Builder {
		
		private final TorConfiguration.Builder builder;
		
		private Builder(TorConfiguration.Builder builder) {
			this.builder = Objects.requireNonNull(builder);
		}
		
		public TorConnection build() {
			return new TorConnection(builder.build());
		}
		
		public Builder password(String password) throws Exception {
			return passwordHash(Tor.passwordHash(password));
		}
		
		public Builder passwordHash(String passwordHash) {
			builder.set(TorConfiguration.Options.HashedControlPassword.ofValue(passwordHash));
			return this;
		}
		
		public Builder noPassword() {
			builder.remove(TorConfiguration.Options.HashedControlPassword.name());
			return this;
		}
	}
}