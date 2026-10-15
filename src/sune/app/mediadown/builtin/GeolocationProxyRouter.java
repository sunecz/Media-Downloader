package sune.app.mediadown.builtin;

import java.util.Map;
import java.util.Objects;

import sune.app.mediadown.net.Transport.ProxyConfiguration;
import sune.app.mediadown.net.Transport.ProxyRouter;
import sune.app.mediadown.net.Web;

/** @since 00.02.09 */
final class GeolocationProxyRouter implements ProxyRouter {
	
	private final Map<Web.Hint.Geolocation, ProxyConfiguration> configurations;
	
	GeolocationProxyRouter(Map<Web.Hint.Geolocation, ProxyConfiguration> configurations) {
		this.configurations = Objects.requireNonNull(configurations);
	}
	
	private final ProxyConfiguration defaultRoute() {
		return configurations.get(Web.Hint.Geolocation.ANY);
	}
	
	@Override
	public ProxyConfiguration route(Web.Request request) {
		return request.hint(Web.Hint.GEOLOCATION).map(configurations::get).orElseGet(this::defaultRoute);
	}
}
