package sune.app.mediadown.resource;

import java.io.InputStream;

public interface InputStreamResolver {
	
	InputStream resolve(String path);
}