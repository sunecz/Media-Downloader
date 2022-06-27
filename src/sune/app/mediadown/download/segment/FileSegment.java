package sune.app.mediadown.download.segment;

import java.net.URI;

/** @since 00.02.05 */
public interface FileSegment {
	
	void size(long size);
	URI uri();
	long size();
}