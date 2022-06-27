package sune.app.mediadown.update;

public interface CheckListener {
	
	// Methods
	void begin();
	void compare(String name);
	void end();
	// Listeners
	FileCheckListener    fileCheckListener();
	FileDownloadListener fileDownloadListener();
}