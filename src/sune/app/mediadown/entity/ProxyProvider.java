package sune.app.mediadown.entity;

import sune.app.mediadown.net.Transport;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.09 */
public interface ProxyProvider {
	
	Transport.ProxyRouter router();
	default void dispose() { /* Do nothing by default */ }
	
	static interface Settings {
		
		SSDCollection save();
	}
	
	static interface Factory {
		
		Settings loadSettings(SSDCollection data);
		ProxyProvider create(Settings settings);
		String name();
	}
}
