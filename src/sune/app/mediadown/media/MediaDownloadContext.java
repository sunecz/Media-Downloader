package sune.app.mediadown.media;

import java.nio.file.Path;

import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.download.MediaDownloadConfiguration;

/** @since 00.02.09 */
public interface MediaDownloadContext {
	
	Media media();
	Path destination();
	MediaDownloadConfiguration mediaConfiguration();
	DownloadConfiguration configuration();
}