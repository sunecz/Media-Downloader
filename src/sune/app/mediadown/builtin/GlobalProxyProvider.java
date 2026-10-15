package sune.app.mediadown.builtin;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import sune.app.mediadown.authentication.Credentials;
import sune.app.mediadown.authentication.CredentialsManager;
import sune.app.mediadown.authentication.UsernameCredentials;
import sune.app.mediadown.entity.ProxyProvider;
import sune.app.mediadown.net.Transport;
import sune.app.mediadown.net.Transport.ProxyConfiguration;
import sune.app.mediadown.net.Transport.ProxyRouter;
import sune.app.mediadown.net.Web;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.09 */
final class GlobalProxyProvider implements ProxyProvider {
	
	private final ProxyRouter router;
	private GlobalProxyProvider(ProxyRouter router) { this.router = Objects.requireNonNull(router); }
	@Override public ProxyRouter router() { return router; }
	
	static final class Factory implements ProxyProvider.Factory {
		
		@Override
		public ProxyProvider.Settings loadSettings(SSDCollection data) {
			return GlobalProxyProviderSettings.Load.load(data);
		}
		
		@Override
		public ProxyProvider create(ProxyProvider.Settings settings) {
			if(!(settings instanceof GlobalProxyProviderSettings)) return null;
			GlobalProxyProviderSettings proxySettings = (GlobalProxyProviderSettings) settings;
			Map<Web.Hint.Geolocation, GlobalProxyProviderSettings.ProxyGroup> groups = proxySettings.groups();
			Map<Web.Hint.Geolocation, ProxyConfiguration> configurations = new HashMap<>();
			
			for(Entry<Web.Hint.Geolocation, GlobalProxyProviderSettings.ProxyGroup> entry : groups.entrySet()) {
				GlobalProxyProviderSettings.ProxyEndpoint forHttp  = entry.getValue().forHttp();
				GlobalProxyProviderSettings.ProxyEndpoint forHttps = entry.getValue().forHttps();
				
				if(forHttps == null) forHttps = forHttp; else
				if(forHttp  == null) forHttp  = forHttps;
				
				ProxyConfiguration configuration = new ProxyConfiguration(
					Transport.proxySelectorOf(forHttp.uri(), forHttps.uri()),
					new ProxyAuthenticator(forHttp.credentialsName(), forHttps.credentialsName())
				);
				
				configurations.put(entry.getKey(), configuration);
			}
			
			return new GlobalProxyProvider(new GeolocationProxyRouter(configurations));
		}
		
		private static final class ProxyAuthenticator extends Authenticator {
			
			private final String credentialsNameForHttp;
			private final String credentialsNameForHttps;
			
			ProxyAuthenticator(String credentialsNameForHttp, String credentialsNameForHttps) {
				this.credentialsNameForHttp  = credentialsNameForHttp;
				this.credentialsNameForHttps = credentialsNameForHttps;
			}
			
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				if(getRequestorType() != RequestorType.PROXY) {
					return null; // Only handle 407 status code
				}
				
				String credentialsName;
				switch(getRequestingURL().getProtocol().toLowerCase(Locale.ROOT)) {
					case "http":  case "ws":  credentialsName = credentialsNameForHttp; break;
					case "https": case "wss": credentialsName = credentialsNameForHttps; break;
					default:                  credentialsName = null; break;
				}
				
				if(credentialsName == null) {
					return null;
				}
				
				try {
					Credentials credentials = CredentialsManager.instance().get(credentialsName);
					
					if(!(credentials instanceof UsernameCredentials)) {
						return null; // Unsupported
					}
					
					try(UsernameCredentials uc = (UsernameCredentials) credentials) {
						return new PasswordAuthentication(
							uc.username(),
							uc.password().toCharArray()
						);
					}
				} catch(IOException ex) {
					return null;
				}
			}
		}
		
		@Override public String name() { return "global"; }
	}
}
