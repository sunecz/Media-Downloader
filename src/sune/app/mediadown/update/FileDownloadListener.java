package sune.app.mediadown.update;

import java.nio.file.Path;

@Deprecated(forRemoval=true)
public interface FileDownloadListener {
	
	void begin (String url, Path file);
	void update(String url, Path file, long current, long total);
	void end   (String url, Path file);
	void error (Exception ex);
}