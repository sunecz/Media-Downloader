package sune.app.mediadown.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import sune.app.mediadown.media.ResolvedMedia;
import sune.app.mediadown.pipeline.AbstractPipelineTask;
import sune.app.mediadown.pipeline.Pipeline;
import sune.app.mediadown.pipeline.PipelineResult;
import sune.app.mediadown.pipeline.PipelineTask;
import sune.app.mediadown.pipeline.PipelineTransformer;

/** @since 00.02.09 */
// Package-private
class CombinedTransformer implements Transformer {
	
	private final List<Transformer> transformers;
	
	// Package-private
	CombinedTransformer(List<Transformer> transformers) {
		this.transformers = List.copyOf(transformers); // Always make copy
	}
	
	private static final <T> List<T> nonNullItems(List<T> list) {
		if(list == null) {
			return null;
		}
		
		List<T> filtered = list.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		
		if(filtered.isEmpty()) {
			return null;
		}
		
		return filtered;
	}
	
	private static final <T> List<T> replaceNullItems(List<T> list, Supplier<? extends T> supplier) {
		Objects.requireNonNull(supplier);
		
		if(list == null || list.isEmpty()) {
			return null;
		}
		
		List<T> filtered = list.stream()
			.map((v) -> v == null ? supplier.get() : v)
			.collect(Collectors.toList());
		
		return filtered;
	}
	
	@Override
	public boolean isUsable(ResolvedMedia media) {
		return transformers.stream().allMatch((t) -> t.isUsable(media));
	}
	
	@Override
	public PipelineTransformer pipelineTransformer() {
		return new CombinedPipelineTransformer(
			transformers.stream().map(Transformer::pipelineTransformer).collect(Collectors.toList())
		);
	}
	
	private static final class CombinedPipelineTransformer implements PipelineTransformer {
		
		private final List<PipelineTransformer> transformers;
		
		public CombinedPipelineTransformer(List<PipelineTransformer> transformers) {
			this.transformers = Objects.requireNonNull(nonNullItems(transformers));
		}
		
		@Override
		public PipelineResult transform(PipelineResult result) {
			return new CombinedPipelineResult(
				transformers.stream().map((t) -> t.transform(result)).collect(Collectors.toList())
			);
		}
		
		@Override
		public PipelineTask transform(PipelineTask task) {
			return new CombinedPipelineTask(
				transformers.stream().map((t) -> t.transform(task)).collect(Collectors.toList())
			);
		}
	}
	
	private static final class CombinedPipelineResult implements PipelineResult {
		
		private final List<PipelineResult> results;
		
		public CombinedPipelineResult(List<PipelineResult> results) {
			this.results = Objects.requireNonNull(replaceNullItems(results, DoNothingPipelineResult::new));
		}
		
		@Override
		public PipelineTask process(Pipeline pipeline) throws Exception {
			List<PipelineTask> tasks = new ArrayList<>(results.size());
			
			for(PipelineResult result : results) {
				tasks.add(result.process(pipeline));
			}
			
			return new CombinedPipelineTask(tasks);
		}
		
		@Override
		public boolean isTerminating() {
			return results.stream().allMatch(PipelineResult::isTerminating);
		}
	}
	
	private static final class DoNothingPipelineResult implements PipelineResult {
		
		@Override
		public DoNothingPipelineTask process(Pipeline pipeline) throws Exception {
			return new DoNothingPipelineTask();
		}
		
		@Override
		public boolean isTerminating() {
			return true;
		}
	}
	
	private static final class DoNothingPipelineTask extends AbstractPipelineTask {
		
		@Override
		protected PipelineResult doRun(Pipeline pipeline) throws Exception {
			return new DoNothingPipelineResult();
		}
	}
	
	private static final class CombinedPipelineTask extends AbstractPipelineTask {
		
		private final List<PipelineTask> tasks;
		
		public CombinedPipelineTask(List<PipelineTask> tasks) {
			this.tasks = Objects.requireNonNull(replaceNullItems(tasks, DoNothingPipelineTask::new));
		}
		
		@Override
		protected PipelineResult doRun(Pipeline pipeline) throws Exception {
			List<PipelineResult> results = new ArrayList<>(tasks.size());
			
			for(PipelineTask task : tasks) {
				if(!checkState()) {
					return null;
				}
				
				results.add(task.run(pipeline));
			}
			
			return new CombinedPipelineResult(results);
		}
		
		@Override
		protected void doStop() throws Exception {
			for(PipelineTask task : tasks) {
				task.stop();
			}
		}
		
		@Override
		protected void doPause() throws Exception {
			for(PipelineTask task : tasks) {
				task.pause();
			}
		}
		
		@Override
		protected void doResume() throws Exception {
			for(PipelineTask task : tasks) {
				task.resume();
			}
		}
	}
}