package sune.app.mediadown.tor;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;

import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.util.NIO;
import sune.app.mediadown.util.OSUtils;
import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class Tor {
	
	private static final VarLoader<Path> path = VarLoader.of(Tor::ensureBinary);
	
	// Forbid anyone to create an instance of this class
	private Tor() {
	}
	
	private static final Path ensureBinary() {
		Path path = NIO.localPath("resources/binary/tor", OSUtils.getExecutableName("tor"));
		
		if(!NIO.isRegularFile(path)) {
			throw new IllegalStateException("Tor was not found at " + path.toAbsolutePath().toString());
		}
		
		return path;
	}
	
	public static final Path path() {
		return path.value();
	}
	
	public static final TorProcess createProcess() {
		return TorProcess.create();
	}
	
	public static final String passwordHash(String password) throws Exception {
		try(TorProcess process = createProcess()) {
			return process.hashPassword(password);
		}
	}
	
	private static final String ip() throws Exception {
		return Web.request(Request.of(Net.uri("https://api.ipify.org/")).GET()).body();
	}
	
	private static final String ipTor(int port) throws Exception {
		URI uri = Net.uri("https://api.ipify.org/");
		
		HttpClient client = HttpClient.newBuilder()
			.proxy(ProxySelector.of(InetSocketAddress.createUnresolved("127.0.0.1", port)))
			.version(Version.HTTP_2)
			.build();
		
		// Use different authentication for the proxy to not reuse streams. For more information,
		// see the following links:
		// https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Proxy-Authorization (Basic username:password)
		// https://manpages.debian.org/stretch-backports/tor/torrc.5.en.html (SocksPort, IsolateSOCKSAuth)
		String auth = Utils.randomString(16) + ':' + Utils.randomString(16);
		HttpRequest request = HttpRequest.newBuilder(uri)
			.header("Proxy-Authorization", auth)
			.GET().build();
		
		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
		return response.body();
	}
	
	public static void main(String[] args) throws Exception {
		String password = Utils.randomString(32);
		String passwordHash = passwordHash(password);
		
		TorConfiguration configuration = TorConfiguration.builder()
			.set(TorConfiguration.Options.ClientOnly)
			.set(TorConfiguration.Options.GeoIPFile.ofValue(path().resolveSibling("data/geoip").toString()))
			.set(TorConfiguration.Options.GeoIPv6File.ofValue(path().resolveSibling("data/geoip6").toString()))
			.set(TorConfiguration.Options.ControlPort.ofValue("auto"))
			.set(TorConfiguration.Options.SocksPort.ofValue("0"))
			.set(TorConfiguration.Options.HTTPTunnelPort.ofValue("auto"))
			.set(TorConfiguration.Options.HardwareAccel)
			.set(TorConfiguration.Options.HashedControlPassword.ofValue(passwordHash))
			.build();
		
		try(TorProcess tor = Tor.createProcess()) {
			System.out.println("----- Tor initializing");
			tor.start(TorConfigurationFile.ofTemporary(configuration));
			tor.waitForInitialized();
			System.out.println("----- Tor initialized");
			TorControl control = tor.control();
			
			if(!control.authenticate(password)) {
				throw new IllegalStateException("Failed to authenticate to access Tor control");
			}
			
			System.out.println("----- Authenticated");
			
			if(!control.listenForStreamEvents()) {
				throw new IllegalStateException("Failed to listen for stream events");
			}
			
			System.out.println("----- Listening for stream events");
			
			System.out.println("Current IP (Tor): " + ipTor(tor.httpPort()));
			System.out.println("Current IP (Tor Control): " + control.ipAddress());
			System.out.println("Current country: " + control.country());
			
			/*System.out.println("----- Requesting a new identity");
			
			if(!control.newCircuit()) {
				throw new IllegalStateException("Failed to obtain a new identity");
			}
			
			System.out.println("----- New identity obtained");*/
			
			System.out.println("Current IP (Tor): " + ipTor(tor.httpPort()));
			System.out.println("Current IP (Tor Control): " + control.ipAddress());
			System.out.println("Current country: " + control.country());
		}
	}
}