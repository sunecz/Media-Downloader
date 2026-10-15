package sune.app.mediadown.gui.configuration;

import javafx.scene.layout.Pane;
import sune.app.mediadown.entity.ProxyProvider;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.09 */
public interface ProxyProviderGUI {
	
	Pane createPane();
	SSDCollection save();
	
	static interface Factory {
		
		ProxyProviderGUI create(ProxyProvider.Settings settings);
		String name();
	}
}
