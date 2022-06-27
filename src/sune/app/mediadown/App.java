package sune.app.mediadown;

public final class App {
	
	// Forbid anyone to create an instance of this class
	private App() {
	}
	
	public static final void main(String[] args) {
		MediaDownloader.initialize(args);
	}
}