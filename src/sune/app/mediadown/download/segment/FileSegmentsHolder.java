package sune.app.mediadown.download.segment;

import java.util.List;

/** @since 00.02.05 */
public interface FileSegmentsHolder {
	
	List<? extends FileSegment> segments();
	int count();
	double duration();
}