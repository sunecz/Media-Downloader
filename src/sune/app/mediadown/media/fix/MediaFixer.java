package sune.app.mediadown.media.fix;

import java.util.List;

import sune.app.mediadown.conversion.ConversionMedia;
import sune.app.mediadown.gui.table.ResolvedMedia;
import sune.app.mediadown.util.Metadata;

/** @since 00.02.09 */
public interface MediaFixer extends AutoCloseable, MediaFixerContext {
	
	void start(ResolvedMedia output, List<ConversionMedia> inputs, Metadata metadata) throws Exception;
	void stop() throws Exception;
	void pause() throws Exception;
	void resume() throws Exception;
	
	static final class Steps {
		
		public static final String STEP_VIDEO_FIX_TIMESTAMPS = "video.fix_timestamps";
		public static final String STEP_AUDIO_FIX_TIMESTAMPS = "audio.fix_timestamps";
		
		// Forbid anyone to create an instance of this class
		private Steps() {
		}
	}
}