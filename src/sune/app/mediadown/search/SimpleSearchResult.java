package sune.app.mediadown.search;

import javafx.scene.image.Image;

public class SimpleSearchResult implements SearchResult {
	
	private final SearchEngine source;
	private final String url;
	private final String title;
	private final Image icon;
	
	public SimpleSearchResult(SearchEngine source, String url, String title, Image icon) {
		this.source = source;
		this.url    = url;
		this.title  = title;
		this.icon   = icon;
	}
	
	@Override
	public SearchEngine getSource() {
		return source;
	}
	
	@Override
	public String getURL() {
		return url;
	}
	
	@Override
	public String getTitle() {
		return title;
	}
	
	@Override
	public Image getIcon() {
		return icon;
	}
}