package sune.app.mediadown.pipeline;

import java.util.concurrent.TimeUnit;

import sune.app.mediadown.event.tracker.ConversionTracker;
import sune.app.mediadown.event.tracker.DownloadTracker;
import sune.app.mediadown.event.tracker.PipelineProgress;
import sune.app.mediadown.event.tracker.Tracker;
import sune.app.mediadown.util.Utils;
import sune.app.mediadown.util.Utils.SizeUnit;

public interface PipelineInfoData {
	
	void update(PipelineInfo info);
	
	/** @since 00.02.09 */
	String state();
	
	public static final class OfText implements PipelineInfoData {
		
		private final String state;
		private final String text;
		
		public OfText(String state, String text) {
			this.state = state;
			this.text = text;
		}
		
		@Override
		public void update(PipelineInfo info) {
			info.progress(PipelineProgress.INDETERMINATE);
			info.state(state);
			info.current(null);
			info.total(null);
			info.speed(null);
			info.timeLeft(null);
			info.information(text);
		}
		
		@Override
		public String state() {
			return state;
		}
	}
	
	public static final class OfState implements PipelineInfoData {
		
		private final String state;
		private final String text;
		/** @since 00.02.09 */
		private final double progress;
		
		public OfState(String state, String text) {
			this(state, text, PipelineProgress.RESET);
		}
		
		/** @since 00.02.09 */
		public OfState(String state, String text, double progress) {
			this.state = state;
			this.text = text;
			this.progress = progress;
		}
		
		@Override
		public void update(PipelineInfo info) {
			info.progress(progress);
			info.state(state);
			info.information(text);
		}
		
		@Override
		public String state() {
			return state;
		}
	}
	
	public static final class OfEndText implements PipelineInfoData {
		
		private final String state;
		private final double progress;
		private final String text;
		
		public OfEndText(String state, double progress, String text) {
			this.state = state;
			this.progress = progress;
			this.text = text;
		}
		
		@Override
		public void update(PipelineInfo info) {
			info.progress(progress);
			info.state(state);
			info.current(null);
			info.total(null);
			info.speed(null);
			info.timeLeft(null);
			info.information(text);
		}
		
		@Override
		public String state() {
			return state;
		}
	}
	
	public static final class OfTracker implements PipelineInfoData {
		
		private final Tracker tracker;
		
		public OfTracker(Tracker tracker) {
			this.tracker = tracker;
		}
		
		@Override
		public void update(PipelineInfo info) {
			info.progress(tracker.progress());
			info.state(tracker.state());
			info.current(null);
			info.total(null);
			info.speed(null);
			info.timeLeft(null);
			info.information(tracker.textProgress());
			tracker.view(info);
		}
		
		@Override
		public String state() {
			return tracker.state();
		}
	}
	
	public static final class OfDownload implements PipelineInfoData {
		
		private final DownloadTracker tracker;
		private final String text;
		
		public OfDownload(DownloadTracker tracker, String text) {
			this.tracker = tracker;
			this.text = text;
		}
		
		@Override
		public void update(PipelineInfo info) {
			info.progress(tracker.progress());
			info.state(tracker.state());
			info.current(Utils.OfFormat.size(tracker.current(), SizeUnit.BYTES, 2));
			info.total(Utils.OfFormat.size(tracker.total(), SizeUnit.BYTES, 2));
			info.speed(Utils.OfFormat.size(tracker.speed(), SizeUnit.BYTES, 2) + "/s");
			info.timeLeft(Utils.OfFormat.time(tracker.secondsLeft(), TimeUnit.SECONDS, false));
			info.information(text);
			tracker.view(info);
		}
		
		@Override
		public String state() {
			return tracker.state();
		}
	}
	
	public static final class OfConversion implements PipelineInfoData {
		
		private final ConversionTracker tracker;
		private final String text;
		
		public OfConversion(ConversionTracker tracker, String text) {
			this.tracker = tracker;
			this.text = text;
		}
		
		@Override
		public void update(PipelineInfo info) {
			info.progress(tracker.progress());
			info.state(tracker.state());
			info.current(Utils.OfFormat.time(tracker.currentTime(), TimeUnit.SECONDS, false));
			info.total(Utils.OfFormat.time(tracker.totalTime(), TimeUnit.SECONDS, false));
			info.speed(null);
			info.timeLeft(null);
			info.information(text);
			tracker.view(info);
		}
		
		@Override
		public String state() {
			return tracker.state();
		}
	}
}