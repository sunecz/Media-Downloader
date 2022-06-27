package sune.app.mediadown.update;

import java.nio.file.Path;

public interface FileCheckListener {
	
	void begin (Path dir);
	void update(Path path, String hash);
	void end   (Path dir);
	void error (Exception ex);
}