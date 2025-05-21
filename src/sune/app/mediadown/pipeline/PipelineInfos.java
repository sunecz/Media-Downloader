package sune.app.mediadown.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import sune.app.mediadown.concurrent.Threads;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.ListEvent;
import sune.app.mediadown.event.ListEvent.ListChange;
import sune.app.mediadown.event.Listener;

/** @since 00.02.09 */
public class PipelineInfos implements EventBindable<ListEvent> {
	
	private final List<PipelineInfo> items = new ArrayList<>();
	private final EventRegistry<ListEvent> eventRegistry = new EventRegistry<>();
	
	public PipelineInfos() {
	}
	
	public static final boolean anyNonPaused(List<PipelineInfo> infos) {
		return infos.stream().anyMatch((i) -> {
			Pipeline p = i.pipeline();
			return i.isPausing() || (p.isStarted() && p.isRunning());
		});
	}
	
	public static final boolean anyTerminable(List<PipelineInfo> infos) {
		return infos.stream().anyMatch((i) -> {
			Pipeline p = i.pipeline();
			return i.isStopping() || (p.isStarted() && (p.isRunning() || p.isPaused()));
		});
	}
	
	public static final boolean anyRetryable(List<PipelineInfo> infos) {
		return infos.stream().anyMatch((i) -> {
			Pipeline p = i.pipeline();
			return i.isRetrying() || (p.isDone() || p.isStopped() || p.isError());
		});
	}
	
	private final void emitListChange(ListChange<PipelineInfo> change) {
		eventRegistry.call(ListEvent.CHANGED, change);
	}
	
	private final boolean isActivePipeline(PipelineInfo info) {
		Pipeline pipeline = info.pipeline();
		return pipeline.isRunning() || pipeline.isPaused();
	}
	
	public void add(PipelineInfo info) {
		items.add(info);
		emitListChange(ListChange.ofAdded(List.of(info)));
	}
	
	public void add(List<PipelineInfo> infos) {
		items.addAll(infos);
		emitListChange(ListChange.ofAdded(infos));
	}
	
	public void remove(PipelineInfo info) {
		items.remove(info);
		emitListChange(ListChange.ofRemoved(List.of(info)));
	}
	
	public void remove(List<PipelineInfo> infos) {
		items.removeAll(infos);
		emitListChange(ListChange.ofRemoved(infos));
	}
	
	public void start(List<PipelineInfo> infos) {
		List<PipelineInfo> notEnqueued = infos.stream()
			.filter(Predicate.not(PipelineInfo::isQueued))
			.collect(Collectors.toList());
		
		// Enqueue all the items, so that they can be sequentually added
		notEnqueued.stream().forEachOrdered((i) -> i.isQueued(true));
		
		// Start all items in a thread with sequential ordering
		Threads.executeEnsured(() -> {
			notEnqueued.stream().forEachOrdered(PipelineInfo::start);
		});
	}
	
	public void startAll() {
		start(pipelines());
	}
	
	public void stop(List<PipelineInfo> infos) {
		Threads.executeEnsured(() -> {
			infos.stream().forEachOrdered(PipelineInfo::stop);
		});
	}
	
	public void stopAll() {
		stop(pipelines());
	}
	
	public void pause(List<PipelineInfo> infos) {
		Threads.executeEnsured(() -> {
			infos.stream().forEachOrdered(PipelineInfo::pause);
		});
	}
	
	public void pauseAll() {
		pause(pipelines());
	}
	
	public void resume(List<PipelineInfo> infos) {
		Threads.executeEnsured(() -> {
			infos.stream().forEachOrdered(PipelineInfo::resume);
		});
	}
	
	public void resumeAll() {
		resume(pipelines());
	}
	
	public void retry(List<PipelineInfo> infos) {
		Threads.executeEnsured(() -> {
			infos.stream().forEachOrdered(PipelineInfo::retry);
		});
	}
	
	public List<PipelineInfo> pipelines() {
		return List.copyOf(items);
	}
	
	public List<PipelineInfo> activePipelines() {
		return pipelines().stream().filter(this::isActivePipeline).collect(Collectors.toList());
	}
	
	public boolean hasActivePipelines() {
		return pipelines().stream().anyMatch(this::isActivePipeline);
	}
	
	@Override
	public <V> void addEventListener(Event<? extends ListEvent, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends ListEvent, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
	
	public static class Stats {
		
		private final List<PipelineInfo> infos;
		
		private final int count;
		private final int started;
		private final int done;
		private final int stopped;
		private final int error;
		
		protected Stats(List<PipelineInfo> infos, int count, int started, int done, int stopped, int error) {
			this.infos = infos;
			this.count = count;
			this.started = started;
			this.done = done;
			this.stopped = stopped;
			this.error = error;
		}
		
		public static final Stats from(List<PipelineInfo> infos) {
			int count = infos.size();
			int started = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isStarted).count();
			int done = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isDone).count();
			int stopped = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isStopped).count();
			int error = (int) infos.stream().map(PipelineInfo::pipeline).filter(Pipeline::isError).count();
			
			return new Stats(infos, count, started, done, stopped, error);
		}
		
		public boolean anyNonPaused() {
			return PipelineInfos.anyNonPaused(infos);
		}
		
		public boolean anyTerminable() {
			return PipelineInfos.anyTerminable(infos);
		}
		
		public boolean anyRetryable() {
			return PipelineInfos.anyRetryable(infos);
		}
		
		public int count() {
			return count;
		}
		
		public int started() {
			return started;
		}
		
		public int done() {
			return done;
		}
		
		public int stopped() {
			return stopped;
		}
		
		public int error() {
			return error;
		}
	}
}
