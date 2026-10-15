package sune.app.mediadown.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.entity.ProxyProvider;
import sune.app.mediadown.util.unsafe.Reflection;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.09 */
public final class Transport {
	
	private Transport() { throw new AssertionError("No instances"); }
	
	public static final int portOf(URI uri) {
		Objects.requireNonNull(uri);
		
		int port;
		if((port = uri.getPort()) == -1) {
			String scheme;
			if((scheme = uri.getScheme()) == null) {
				port = 80;
			} else {
				switch(scheme.toLowerCase(Locale.ROOT)) {
					case "http":  port = 80;  break;
					case "https": port = 443; break;
					default:      port = 80;  break;
				}
			}
		}
		
		return port;
	}
	
	public static final InetSocketAddress socketAddressOf(URI uri) {
		Objects.requireNonNull(uri);
		return InetSocketAddress.createUnresolved(uri.getHost(), portOf(uri));
	}
	
	public static final ProxySelector proxySelectorOf(URI uriForHttp, URI uriForHttps) {
		return new CommonProxySelector(uriForHttp, uriForHttps);
	}
	
	public static class ProxyConfiguration {
		
		protected final ProxySelector selector;
		protected final Authenticator authenticator;
		
		public ProxyConfiguration(ProxySelector selector, Authenticator authenticator) {
			this.selector = selector;
			this.authenticator = authenticator;
		}
		
		public ProxySelector selector() { return selector; }
		public Authenticator authenticator() { return authenticator; }
	}
	
	public static interface ProxyRouter {
		
		ProxyConfiguration route(Web.Request request);
	}
	
	public static final class ProxyManager {
		
		private static final ProxyProvider DEFAULT_PROVIDER = DefaultProxyProvider.INSTANCE;
		private static final Map<String, ProxyProvider.Factory> factories = new ConcurrentHashMap<>();
		private static volatile ProxyProvider currentProvider = DEFAULT_PROVIDER;
		
		private ProxyManager() { throw new AssertionError("No instances"); }
		
		public static void register(String className) {
			register(
				ProxyProvider.Factory.class.cast(
					Reflection.newInstance(Reflection.getConstructor(Reflection.getClass(className)))
				)
			);
		}
		
		public static void register(ProxyProvider.Factory factory) {
			Objects.requireNonNull(factory);
			factories.put(factory.name(), factory);
		}
		
		public static void initialize(SSDCollection data) {
			String name = data.getDirectString("name");
			ProxyProvider.Factory factory = factory(name);
			ProxyProvider next = DEFAULT_PROVIDER;
			
			if(factory != null) {
				try {
					SSDCollection settings = data.getDirectCollection("settings", null);
					if(settings == null) settings = SSDCollection.empty();
					next = factory.create(factory.loadSettings(settings));
				} catch(Exception ex) {
					// Ignore for now
				}
			}
			
			ProxyProvider prev = currentProvider;
			currentProvider = next;
			if(prev != next && prev != DEFAULT_PROVIDER) prev.dispose();
		}
		
		public static ProxyProvider.Factory factory(String name) {
			return name != null ? factories.get(name) : null;
		}
		
		public static ProxyProvider provider() { return currentProvider; }
		public static List<ProxyProvider.Factory> factories() { return List.copyOf(factories.values()); }
	}
	
	private static final class Holder {
		
		static final class OfTLSProxyBridgePool {
			
			static final TLSProxyBridgePool INSTANCE = create();
			
			private static final TLSProxyBridgePool create() {
				SSLContext sslContext = SSL.Contexts.aiaFetching();
				TLSProxyBridgePool instance = new TLSProxyBridgePool(sslContext);
				Disposables.add(instance::close);
				return instance;
			}
		}
	}
	
	private static final class CommonProxySelector extends ProxySelector {
		
		private static final List<Proxy> NO_PROXY_LIST = List.of(Proxy.NO_PROXY);
		
		private final List<Proxy> listForHttp;
		private final List<Proxy> listForHttps;
		
		CommonProxySelector(URI uriForHttp, URI uriForHttps) {
			this.listForHttp  = resolveList(Objects.requireNonNull(uriForHttp));
			this.listForHttps = resolveList(Objects.requireNonNull(uriForHttps));
		}
		
		private static final List<Proxy> resolveList(URI uri) {
			InetSocketAddress address;
			switch(uri.getScheme().toLowerCase(Locale.ROOT)) {
				case "https":
					// Java's HttpClient doesn't support TLS proxy. The workaround is to route
					// traffic through an additional small forwarding proxy. This proxy accepts
					// any connection, does handshake with the actual configured proxy and then
					// forwards all following traffic.
					address = new InetSocketAddress(
						InetAddress.getLoopbackAddress(),
						Holder.OfTLSProxyBridgePool.INSTANCE.getLocalPort(uri)
					);
					break;
				default:
					address = socketAddressOf(uri);
					break;
			}
			
			return List.of(new Proxy(Proxy.Type.HTTP, address));
		}
		
		@Override
		public void connectFailed(URI uri, SocketAddress sa, IOException e) {
			// Ignore
		}
		
		@Override
		public List<Proxy> select(URI uri) {
			switch(uri.getScheme().toLowerCase(Locale.ROOT)) {
				case "http":  return listForHttp;
				case "https": return listForHttps;
				default:      return NO_PROXY_LIST;
			}
		}
	}
	
	private static final class TLSProxyBridge implements AutoCloseable {
		
		private final ServerSocket server;
		private final String upstreamHost;
		private final int upstreamPort;
		private final SSLSocketFactory sslSocketFactory;
		
		private final ExecutorService executor;
		private volatile boolean running = true;
		private Thread serverThread;
		
		private TLSProxyBridge(
			String upstreamHost,
			int upstreamPort,
			SSLSocketFactory sslSocketFactory
		) throws IOException {
			this.upstreamHost = Objects.requireNonNull(upstreamHost);
			this.upstreamPort = upstreamPort;
			this.sslSocketFactory = Objects.requireNonNull(sslSocketFactory);
			this.server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
			this.executor = Executors.newCachedThreadPool(new BridgeThreadFactory());
		}
		
		public static final TLSProxyBridge of(URI uri, SSLContext sslContext) throws IOException {
			Objects.requireNonNull(uri);
			Objects.requireNonNull(sslContext);
			
			TLSProxyBridge bridge = new TLSProxyBridge(
				uri.getHost(),
				portOf(uri),
				sslContext.getSocketFactory()
			);
			
			bridge.start();
			return bridge;
		}
		
		private final void start() {
			serverThread = new Thread(this::serverLoop, "Web.TLSProxyBridge.ServerThread-");
			serverThread.setDaemon(true);
			serverThread.start();
		}
		
		private final void serverLoop() {
			while(running) {
				try {
					Socket client = server.accept();
					try {
						executor.submit(() -> handleConnection(client));
					} catch(RejectedExecutionException ex) {
						close(client); // Shutting down
					}
				} catch(IOException ex) {
					// Ignore
				}
			}
		}
		
		private final void handleConnection(Socket downstream) {
			SSLSocket upstream = null;
			try {
				downstream.setTcpNoDelay(true);
				upstream = (SSLSocket) sslSocketFactory.createSocket(upstreamHost, upstreamPort);
				
				// Enforce hostname verification against the proxy's certificate - raw SSLSocket
				// doesn't do this by default.
				SSLParameters params = upstream.getSSLParameters();
				params.setEndpointIdentificationAlgorithm("HTTPS");
				upstream.setSSLParameters(params);
				
				upstream.startHandshake();
			} catch(IOException ex) {
				close(downstream);
				close(upstream);
				return; // Setup failed, exit.
			}
			
			submitPump(downstream, upstream);
			submitPump(upstream, downstream);
		}
		
		private final void submitPump(Socket src, Socket dst) {
			try {
				executor.submit(() -> {
					try(Socket s = src; Socket d = dst) {
						s.getInputStream().transferTo(d.getOutputStream());
					} catch(IOException ex) {
						// One of the sides closed, ignore.
					}
				});
			} catch(RejectedExecutionException ex) {
				close(src);
				close(dst);
			}
		}
		
		private final void close(Closeable closeable) {
			if(closeable == null) return;
			try { closeable.close(); } catch(IOException ex) {}
		}
		
		@Override
		public void close() {
			running = false;
			close(server);
			executor.shutdownNow();
			
			if(serverThread != null) {
				try {
					serverThread.join(Duration.ofSeconds(5).toMillis());
				} catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		}
		
		public int getLocalPort() {
			return server.getLocalPort();
		}
		
		private static final class BridgeThreadFactory implements ThreadFactory {
			
			private final String namePrefix = "Web.TLSProxyBridge.Thread-";
			private final AtomicInteger nextId = new AtomicInteger();
			
			@Override
			public Thread newThread(Runnable r) {
				String name = namePrefix + nextId.getAndIncrement();
				Thread thread = new Thread(null, r, name, 0, false);
				thread.setDaemon(true);
				return thread;
			}
		}
	}
	
	private static final class TLSProxyBridgePool implements AutoCloseable {
		
		private final SSLContext sslContext;
		private final Map<Key, TLSProxyBridge> bridges = new ConcurrentHashMap<>();
		
		public TLSProxyBridgePool(SSLContext sslContext) {
			this.sslContext = Objects.requireNonNull(sslContext);
		}
		
		private final TLSProxyBridge createBridge(Key key) {
			return key.createBridge(sslContext);
		}
		
		@Override
		public void close() {
			bridges.values().forEach(TLSProxyBridge::close);
			bridges.clear();
		}
		
		public int getLocalPort(URI uri) {
			return bridges.computeIfAbsent(Key.of(uri), this::createBridge).getLocalPort();
		}
		
		private static final class Key {
			
			private final String key;
			private final URI uri;
			
			private Key(String key, URI uri) {
				this.key = key;
				this.uri = uri;
			}
			
			static final Key of(URI uri) {
				return new Key(uri.getHost() + ":" + portOf(uri), uri);
			}
			
			final TLSProxyBridge createBridge(SSLContext sslContext) {
				try {
					return TLSProxyBridge.of(uri, sslContext);
				} catch(IOException ex) {
					throw new UncheckedIOException(ex);
				}
			}
			
			@Override public int hashCode() { return key.hashCode(); }
			@Override public boolean equals(Object o) { return o instanceof Key && key.equals(((Key) o).key); }
		}
	}
	
	private static final class DefaultProxyProvider implements ProxyProvider {
		
		static final DefaultProxyProvider INSTANCE = new DefaultProxyProvider();
		
		private final ProxyRouter router;
		private DefaultProxyProvider() { router = new Router(); }
		@Override public ProxyRouter router() { return router; }
		
		static final class Router implements ProxyRouter {
			
			@Override public ProxyConfiguration route(Web.Request request) { return null; }
		}
	}
}
