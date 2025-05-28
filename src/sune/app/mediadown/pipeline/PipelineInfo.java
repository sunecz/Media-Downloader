package sune.app.mediadown.pipeline;

import java.util.Objects;

import sune.app.mediadown.MediaDownloader;
import sune.app.mediadown.download.DownloadConfiguration;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.event.PipelineInfoEvent;
import sune.app.mediadown.event.tracker.PipelineProgress;
import sune.app.mediadown.event.tracker.PipelineStates;
import sune.app.mediadown.event.tracker.TrackerView;
import sune.app.mediadown.media.ResolvedMedia;
import sune.app.mediadown.util.Property;
import sune.app.mediadown.util.SimpleProperty;

public class PipelineInfo implements TrackerView, EventBindable<PipelineInfoEvent> {
	
	public static final String TEXT_NONE = null;
	
	protected final Pipeline pipeline;
	protected final ResolvedMedia resolvedMedia;
	protected PipelineMedia media;
	/** @since 00.02.09 */
	protected final EventRegistry<PipelineInfoEvent> eventRegistry = new EventRegistry<>();
	
	protected Property<String> sourceProperty;
	protected Property<String> titleProperty;
	protected Property<String> destinationProperty;
	protected Property<Double> progressProperty;
	protected Property<String> stateProperty;
	protected Property<String> currentProperty;
	protected Property<String> totalProperty;
	protected Property<String> speedProperty;
	protected Property<String> timeLeftProperty;
	protected Property<String> informationProperty;
	
	protected boolean isQueued;
	
	/** @since 00.02.09 */
	protected volatile boolean isPausing;
	/** @since 00.02.09 */
	protected volatile boolean isResuming;
	/** @since 00.02.09 */
	protected volatile boolean isStopping;
	/** @since 00.02.09 */
	protected volatile boolean isRetrying;
	
	public PipelineInfo(Pipeline pipeline, ResolvedMedia resolvedMedia) {
		this.pipeline = Objects.requireNonNull(pipeline);
		this.resolvedMedia = Objects.requireNonNull(resolvedMedia);
		initialize();
	}
	
	private final void initialize() {
		PipelineMedia pipelineMedia = PipelineMedia.of(
			resolvedMedia.media(),
			resolvedMedia.path(),
			resolvedMedia.configuration(),
			DownloadConfiguration.ofDefault()
		);
		
		pipeline.setInput(MediaPipelineResult.of(pipelineMedia));
		media = pipelineMedia;
	}
	
	public void update(PipelineInfoData data) {
		eventRegistry.call(PipelineInfoEvent.UPDATE, data);
	}
	
	public void start() {
		if(pipeline.isStarted()) {
			return;
		}
		
		try {
			pipeline.start();
			media.awaitSubmitted();
		} catch(Exception ex) {
			MediaDownloader.error(ex);
		}
	}
	
	public void stop() {
		if(pipeline.isStopped() || !pipeline.isStarted() || pipeline.isDone()) {
			return;
		}
		
		isStopping = true;
		update(new PipelineInfoData.OfState(
			PipelineStates.STOPPING, PipelineInfo.TEXT_NONE, PipelineProgress.INDETERMINATE
		));
		
		try {
			pipeline.stop();
			pipeline.waitFor();
		} catch(Exception ex) {
			MediaDownloader.error(ex);
		} finally {
			isStopping = false;
		}
	}
	
	public void pause() {
		if(pipeline.isPaused() || !pipeline.isStarted() || pipeline.isDone() || pipeline.isStopped()) {
			return;
		}
		
		isPausing = true;
		update(new PipelineInfoData.OfState(
			PipelineStates.PAUSING, PipelineInfo.TEXT_NONE, PipelineProgress.INDETERMINATE
		));
		
		try {
			pipeline.pause();
		} catch(Exception ex) {
			MediaDownloader.error(ex);
		} finally {
			isPausing = false;
		}
	}
	
	public void resume() {
		if(!pipeline.isPaused() || !pipeline.isStarted() || pipeline.isDone() || pipeline.isStopped()) {
			return;
		}
		
		isResuming = true;
		update(new PipelineInfoData.OfState(
			PipelineStates.RESUMING, PipelineInfo.TEXT_NONE, PipelineProgress.INDETERMINATE
		));
		
		try {
			pipeline.resume();
		} catch(Exception ex) {
			MediaDownloader.error(ex);
		} finally {
			isResuming = false;
		}
	}
	
	/** @since 00.02.09 */
	public void retry() {
		if(!pipeline.isDone() && !pipeline.isStopped() && !pipeline.isError()) {
			return;
		}
		
		isRetrying = true;
		update(new PipelineInfoData.OfState(
			PipelineStates.RETRYING, PipelineInfo.TEXT_NONE, PipelineProgress.INDETERMINATE
		));
		
		try {
			pipeline.waitFor();
			pipeline.reset();
			isQueued(false);
			initialize();
			start();
		} catch(Exception ex) {
			MediaDownloader.error(ex);
		} finally {
			isRetrying = false;
		}
	}
	
	public void isQueued(boolean isQueued) {
		this.isQueued = isQueued;
	}
	
	public Property<String> sourceProperty() {
		return sourceProperty == null
					? sourceProperty = new SimpleProperty<>(source())
					: sourceProperty;
	}
	
	public Property<String> titleProperty() {
		return titleProperty == null
					? titleProperty = new SimpleProperty<>(title())
					: titleProperty;
	}
	
	public Property<String> destinationProperty() {
		return destinationProperty == null
					? destinationProperty = new SimpleProperty<>(destination())
					: destinationProperty;
	}
	
	public Property<Double> progressProperty() {
		return progressProperty == null
					? progressProperty = new SimpleProperty<>()
					: progressProperty;
	}
	
	public Property<String> stateProperty() {
		return stateProperty == null
					? stateProperty = new SimpleProperty<>()
					: stateProperty;
	}
	
	public Property<String> currentProperty() {
		return currentProperty == null
					? currentProperty = new SimpleProperty<>()
					: currentProperty;
	}
	
	public Property<String> totalProperty() {
		return totalProperty == null
					? totalProperty = new SimpleProperty<>()
					: totalProperty;
	}
	
	public Property<String> speedProperty() {
		return speedProperty == null
					? speedProperty = new SimpleProperty<>()
					: speedProperty;
	}
	
	public Property<String> timeLeftProperty() {
		return timeLeftProperty == null
					? timeLeftProperty = new SimpleProperty<>()
					: timeLeftProperty;
	}
	
	public Property<String> informationProperty() {
		return informationProperty == null
					? informationProperty = new SimpleProperty<>()
					: informationProperty;
	}
	
	@Override
	public void progress(double progress) {
		progressProperty().set(progress);
	}
	
	@Override
	public void state(String state) {
		stateProperty().set(state);
	}
	
	@Override
	public void current(String current) {
		currentProperty().set(current);
	}
	
	@Override
	public void total(String total) {
		totalProperty().set(total);
	}
	
	@Override
	public void speed(String speed) {
		speedProperty().set(speed);
	}
	
	@Override
	public void timeLeft(String timeLeft) {
		timeLeftProperty().set(timeLeft);
	}
	
	@Override
	public void information(String information) {
		informationProperty().set(information);
	}
	
	public String source() {
		return resolvedMedia.media().source().toString();
	}
	
	public String title() {
		return resolvedMedia.media().metadata().title();
	}
	
	public String destination() {
		return resolvedMedia.path().toString();
	}
	
	@Override
	public double progress() {
		return progressProperty().get();
	}
	
	@Override
	public String state() {
		return stateProperty().get();
	}
	
	@Override
	public String current() {
		return currentProperty().get();
	}
	
	@Override
	public String total() {
		return totalProperty().get();
	}
	
	@Override
	public String speed() {
		return speedProperty().get();
	}
	
	@Override
	public String timeLeft() {
		return timeLeftProperty().get();
	}
	
	@Override
	public String information() {
		return informationProperty().get();
	}
	
	public Pipeline pipeline() {
		return pipeline;
	}
	
	public ResolvedMedia resolvedMedia() {
		return resolvedMedia;
	}
	
	public boolean isQueued() {
		return isQueued;
	}
	
	public PipelineMedia media() {
		return media;
	}
	
	/** @since 00.02.09 */
	public boolean isPausing() {
		return isPausing;
	}
	
	/** @since 00.02.09 */
	public boolean isResuming() {
		return isResuming;
	}
	
	/** @since 00.02.09 */
	public boolean isStopping() {
		return isStopping;
	}
	
	/** @since 00.02.09 */
	public boolean isRetrying() {
		return isRetrying;
	}
	
	/** @since 00.02.09 */
	@Override
	public <V> void addEventListener(Event<? extends PipelineInfoEvent, V> event,
			Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	/** @since 00.02.09 */
	@Override
	public <V> void removeEventListener(Event<? extends PipelineInfoEvent, V> event,
			Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
}