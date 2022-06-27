package sune.app.mediadown.resource;

import java.nio.file.Path;

public interface Extractable {
	
	void extract(Path dir, InputStreamResolver resolver) throws Exception;
}