package sune.app.mediadown.entity;

import java.nio.file.Path;

import sune.app.mediadown.download.DownloadInitialState;
import sune.app.mediadown.download.DownloadResult;
import sune.app.mediadown.download.MediaDownloadConfiguration;
import sune.app.mediadown.media.Media;

/** @since 00.02.05 */
public interface Downloader {
	
	default DownloadResult download(
		Media media,
		Path destination,
		MediaDownloadConfiguration configuration
	) throws Exception {
		return download(media, destination, configuration, null);
	}
	/** @since 00.02.09 */
	DownloadResult download(
		Media media,
		Path destination,
		MediaDownloadConfiguration configuration,
		DownloadInitialState state
	) throws Exception;
	boolean isDownloadable(Media media);
	
	String title();
	String version();
	String author();
}