package sune.app.mediadown.search;

import javafx.scene.image.Image;

/** @since 00.01.17 */
public interface SearchResult {
	
	SearchEngine getSource();
	String       getURL();
	String       getTitle();
	Image        getIcon();
}