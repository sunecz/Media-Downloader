package sune.app.mediadown.builtin;

import sune.app.mediadown.entity.ProxyProvider;
import sune.app.mediadown.net.Transport.ProxyConfiguration;
import sune.app.mediadown.net.Transport.ProxyRouter;
import sune.app.mediadown.net.Web;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.09 */
final class NoneProxyProvider implements ProxyProvider {
	
	private final ProxyRouter router;
	private NoneProxyProvider() { this.router = new Router(); }
	@Override public ProxyRouter router() { return router; }
	
	static final class Factory implements ProxyProvider.Factory {
		
		@Override public ProxyProvider.Settings loadSettings(SSDCollection data) { return null; }
		@Override public ProxyProvider create(ProxyProvider.Settings settings) { return new NoneProxyProvider(); }
		@Override public String name() { return "none"; }
	}
	
	static final class Router implements ProxyRouter {
		
		@Override public ProxyConfiguration route(Web.Request request) { return null; }
	}
}
