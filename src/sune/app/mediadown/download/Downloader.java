package sune.app.mediadown.download;

import java.nio.file.Path;

import sune.app.mediadown.media.Media;

/** @since 00.02.05 */
public interface Downloader {
	
	DownloadResult download(Media media, Path destination, MediaDownloadConfiguration configuration) throws Exception;
	boolean isDownloadable(Media media);
	
	String title();
	String version();
	String author();
}