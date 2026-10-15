package sune.app.mediadown.builtin;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import sune.app.mediadown.entity.ProxyProvider;
import sune.app.mediadown.net.Net;
import sune.app.mediadown.net.Web;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.09 */
public final class GlobalProxyProviderSettings implements ProxyProvider.Settings {
	
	private final Map<Web.Hint.Geolocation, ProxyGroup> groups;
	
	public GlobalProxyProviderSettings(Map<Web.Hint.Geolocation, ProxyGroup> groups) {
		this.groups = Objects.requireNonNull(groups);
	}
	
	@Override
	public SSDCollection save() {
		return Save.save(groups);
	}
	
	public Map<Web.Hint.Geolocation, ProxyGroup> groups() { return groups; }
	
	public static final class ProxyEndpoint {
		
		private final URI uri;
		private final String credentialsName;
		
		public ProxyEndpoint(URI uri, String credentialsName) {
			this.uri = Objects.requireNonNull(uri);
			this.credentialsName = credentialsName;
		}
		
		public URI uri() { return uri; }
		public String credentialsName() { return credentialsName; }
	}
	
	public static final class ProxyGroup {
		
		private final ProxyEndpoint forHttp;
		private final ProxyEndpoint forHttps;
		
		public ProxyGroup(ProxyEndpoint forHttp, ProxyEndpoint forHttps) {
			if((forHttp == null || forHttps == null) && forHttp == forHttps) {
				throw new IllegalArgumentException(
					"Both HTTP and HTTPS endpoints can't be null"
				);
			}
			
			this.forHttp = forHttp;
			this.forHttps = forHttps;
		}
		
		public ProxyEndpoint forHttp() { return forHttp; }
		public ProxyEndpoint forHttps() { return forHttps; }
	}
	
	public static final class Load {
		
		private Load() { throw new AssertionError("No instances"); }
		
		static final ProxyEndpoint loadEndpoint(SSDCollection data) {
			URI uri = Net.uri(data.getDirectString("uri"));
			String credentialsName = data.getDirectString("credentialsName");
			return new ProxyEndpoint(uri, credentialsName);
		}
		
		static final ProxyGroup loadGroup(SSDCollection data) {
			ProxyEndpoint forHttp = null, forHttps = null;
			SSDCollection item;
			if((item = data.getDirectCollection("forHttp")) != null)
				forHttp = loadEndpoint(item);
			if((item = data.getDirectCollection("forHttps")) != null)
				forHttps = loadEndpoint(item);
			return new ProxyGroup(forHttp, forHttps);
		}
		
		public static final GlobalProxyProviderSettings load(SSDCollection data) {
			Map<Web.Hint.Geolocation, ProxyGroup> groups = new HashMap<>();
			
			if(data != null) {
				for(SSDCollection item : data.collectionsIterable()) {
					groups.put(Web.Hint.Geolocation.of(item.getName()), loadGroup(item));
				}
			}
			
			return new GlobalProxyProviderSettings(groups);
		}
	}
	
	public static final class Save {
		
		private Save() { throw new AssertionError("No instances"); }
		
		static final SSDCollection saveEndpoint(ProxyEndpoint endpoint) {
			SSDCollection data = SSDCollection.empty();
			data.set("uri", endpoint.uri().toString());
			String credentialsName = endpoint.credentialsName();
			if(credentialsName != null)
				data.set("credentialsName", credentialsName);
			return data;
		}
		
		static final SSDCollection saveGroup(ProxyGroup group) {
			SSDCollection data = SSDCollection.empty();
			if(group.forHttp() != null)
				data.set("forHttp",  saveEndpoint(group.forHttp()));
			if(group.forHttps() != null)
				data.set("forHttps", saveEndpoint(group.forHttps()));
			return data;
		}
		
		public static final SSDCollection save(Map<Web.Hint.Geolocation, ProxyGroup> groups) {
			SSDCollection data = SSDCollection.empty();
			
			for(Entry<Web.Hint.Geolocation, ProxyGroup> entry : groups.entrySet()) {
				data.set(entry.getKey().isoCountryCode(), saveGroup(entry.getValue()));
			}
			
			return data;
		}
	}
}
