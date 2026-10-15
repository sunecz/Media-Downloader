package sune.app.mediadown.builtin;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sune.app.mediadown.Disposables;
import sune.app.mediadown.Shared;
import sune.app.mediadown.entity.ProxyProvider;
import sune.app.mediadown.net.SSL;
import sune.app.mediadown.net.Transport;
import sune.app.mediadown.net.Transport.ProxyConfiguration;
import sune.app.mediadown.net.Transport.ProxyRouter;
import sune.app.mediadown.net.Web;
import sune.app.mediadown.util.JSON;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.09 */
final class InternalProxyProvider implements ProxyProvider {
	
	private final ProxyRouter router;
	private InternalProxyProvider(ProxyRouter router) { this.router = Objects.requireNonNull(router); }
	@Override public ProxyRouter router() { return router; }
	
	static final class Settings implements ProxyProvider.Settings {
		
		@Override public SSDCollection save() { return SSDCollection.empty(); }
	}
	
	static final class Factory implements ProxyProvider.Factory {
		
		private static final ProxyConfiguration configurationOf(String countryCode) {
			URI proxyUri = URI.create(String.format("https://%s.prx01.sune.cc", countryCode));
			URI authUri  = URI.create(String.format("https://api.prx01.sune.cc/tokens/%s", countryCode));
			
			return new ProxyConfiguration(
				Transport.proxySelectorOf(proxyUri, proxyUri),
				new ProxyAuthenticator(authUri)
			);
		}
		
		@Override
		public ProxyProvider.Settings loadSettings(SSDCollection data) {
			return new Settings();
		}
		
		@Override
		public ProxyProvider create(ProxyProvider.Settings settings) {
			return new InternalProxyProvider(
				new GeolocationProxyRouter(Map.of(
					Web.Hint.Geolocation.CZ, configurationOf("cz"),
					Web.Hint.Geolocation.SK, configurationOf("sk")
				))
			);
		}
		
		private static final class ProxyAuthenticator extends Authenticator {
			
			private static final char[] PW = { 'x' };
			private final URI tokenEndpointUri;
			
			private ProxyAuthenticator(URI tokenEndpointUri) {
				this.tokenEndpointUri = Objects.requireNonNull(tokenEndpointUri);
			}
			
			private final String fetchToken() throws InterruptedException, IOException {
				HttpResponse<InputStream> response = HttpClientHolder.INSTANCE.send(
					HttpRequest
						.newBuilder(tokenEndpointUri)
						.header("User-Agent", Shared.USER_AGENT)
						.GET().build(),
					BodyHandlers.ofInputStream()
				);
				
				try(InputStream stream = response.body()) {
					int statusCode;
					if((statusCode = response.statusCode()) != 200) {
						throw new IOException("Token endpoint returned: " + statusCode);
					}
					
					return JSON.read(stream).getString("token");
				}
			}
			
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				if(getRequestorType() != RequestorType.PROXY) {
					return null; // Only handle 407 status code
				}
				
				try {
					String token = fetchToken();
					return new PasswordAuthentication(token, PW);
				} catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
					return null;
				} catch(IOException ex) {
					return null;
				}
			}
			
			private static final class HttpClientHolder {
				
				static final HttpClient INSTANCE = create();
				
				private static HttpClient create() {
					ExecutorService executor = Executors.newSingleThreadExecutor();
					Disposables.add(executor::shutdownNow);
					
					return (
						HttpClient.newBuilder()
							.connectTimeout(Web.defaultConnectTimeout())
							.executor(executor)
							.sslContext(SSL.Contexts.aiaFetching())
							.version(Web.defaultHttpVersion())
							.followRedirects(Redirect.NEVER)
							.build()
					);
				}
			}
		}
		
		@Override public String name() { return "internal"; }
	}
}
