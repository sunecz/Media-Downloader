package sune.app.mediadown.tor;

import java.net.URI;
import java.nio.file.Path;

import sune.app.mediadown.concurrent.VarLoader;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.net.Web.Request;
import sune.app.mediadown.net.Web.Response;
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
	
	public static final Path path(TorResource resource) {
		return path().resolveSibling(resource.relativePath()).toAbsolutePath();
	}
	
	public static final TorProcess createProcess() {
		return TorProcess.create();
	}
	
	public static final String passwordHash(String password) throws Exception {
		try(TorProcess process = createProcess()) {
			return process.hashPassword(password);
		}
	}
	
	private static final String ip(TorConnection connection) throws Exception {
		URI uri = Net.uri("https://api.ipify.org/");
		
		try(Response.OfString response = Web.request(Request.of(uri).proxy(connection.proxy()).GET())) {
			return response.body();
		}
	}
	
	public static void main(String[] args) throws Exception {
		String password = Utils.randomString(32);
		
		try(TorConnection connection = TorConnection.builder().password(password).build()) {
			connection.open(password);
			
			TorControl control = connection.control();
			
			System.out.println("Current IP (Tor): " + ip(connection));
			System.out.println("Current IP (Tor Control): " + control.ipAddress());
			System.out.println("Current country: " + control.country());
			
			connection.newCircuit();
			
			System.out.println("Current IP (Tor): " + ip(connection));
			System.out.println("Current IP (Tor Control): " + control.ipAddress());
			System.out.println("Current country: " + control.country());
		}
	}
}