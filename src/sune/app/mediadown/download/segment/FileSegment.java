package sune.app.mediadown.download.segment;

import java.net.URI;

/** @since 00.02.05 */
public interface FileSegment {
	
	URI uri();
	long size();
	/** @since 00.02.09 */
	double duration();
}