package sune.app.mediadown.util;

import java.util.HashMap;
import java.util.Map;

import sune.app.mediadown.language.Translation;
import sune.app.mediadown.media.MediaTitleFormat;

/** @since 00.02.05 */
public final class Previews {
	
	private static final String PREVIEW_PROGRAM_NAME = "Program name";
	private static final int    PREVIEW_SEASON       = 2;
	private static final int    PREVIEW_EPISODE      = 5;
	private static final String PREVIEW_EPISODE_NAME = "Episode name";
	
	// Forbid anyone to create an instance of this class
	private Previews() {
	}
	
	public static final String preview(MediaTitleFormat format, MediaTitleFormatPreviewMask mask,
			Translation translation) {
		Map<String, Object> args = new HashMap<>();
		args.put("translation", translation);
		
		if(mask.isProgramName()) args.put("program_name", PREVIEW_PROGRAM_NAME);
		if(mask.isSeason())      args.put("season", PREVIEW_SEASON);
		if(mask.isEpisode())     args.put("episode", PREVIEW_EPISODE);
		if(mask.isEpisodeName()) args.put("episode_name", PREVIEW_EPISODE_NAME);
		if(mask.isSplit())       args.put("split", true);
		
		return format.format(args);
	}
	
	public static final class MediaTitleFormatPreviewMask {
		
		private static final int POS_PROGRAM_NAME = 0;
		private static final int POS_SEASON       = 1;
		private static final int POS_EPISODE      = 2;
		private static final int POS_EPISODE_NAME = 3;
		private static final int POS_SPLIT        = 4;
		
		private final int mask;
		
		private MediaTitleFormatPreviewMask(int mask) {
			this.mask = mask;
		}
		
		public static final MediaTitleFormatPreviewMask of(int mask) {
			return new MediaTitleFormatPreviewMask(mask);
		}
		
		private final boolean is(int pos) {
			return ((mask & (0b1 << pos)) >> pos) == 1;
		}
		
		public boolean isProgramName() {
			return is(POS_PROGRAM_NAME);
		}
		
		public boolean isSeason() {
			return is(POS_SEASON);
		}
		
		public boolean isEpisode() {
			return is(POS_EPISODE);
		}
		
		public boolean isEpisodeName() {
			return is(POS_EPISODE_NAME);
		}
		
		public boolean isSplit() {
			return is(POS_SPLIT);
		}
	}
}