package sune.app.mediadown.update;

public interface UpdateListener {
	
	// Methods
	void beforeUpdate();
	void beforeDownload();
	// Listeners
	FileDownloadListener fileDownloadListener();
}