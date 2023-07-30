package sune.app.mediadown.download;

public interface Download extends DownloadContext {
	
	void start() throws Exception;
	void stop() throws Exception;
	void pause() throws Exception;
	void resume() throws Exception;
}