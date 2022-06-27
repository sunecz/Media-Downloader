package sune.app.mediadown.download.segment;

import java.util.List;

/** @since 00.02.05 */
public interface FileSegmentable<T extends FileSegment> {
	
	List<? extends FileSegmentsHolder<T>> segmentsHolders();
}