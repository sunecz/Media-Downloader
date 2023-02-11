package sune.app.mediadown.engine;

import java.util.List;

import sune.app.mediadown.Episode;
import sune.app.mediadown.MediaGetter;
import sune.app.mediadown.Program;
import sune.app.mediadown.concurrent.ListTask;
import sune.app.mediadown.concurrent.WorkerProxy;
import sune.app.mediadown.concurrent.WorkerUpdatableTask;
import sune.app.mediadown.media.Media;
import sune.app.mediadown.util.CheckedBiFunction;

/** @since 00.02.05 */
public interface MediaEngine extends MediaGetter {
	
	@Deprecated
	List<Program> getPrograms() throws Exception;
	@Deprecated
	List<Episode> getEpisodes(Program program) throws Exception;
	@Deprecated
	List<Media>   getMedia(Episode episode) throws Exception;
	
	@Deprecated
	default WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Program, Boolean>, Void> getPrograms
			(CheckedBiFunction<WorkerProxy, Program, Boolean> function) {
		return WorkerUpdatableTask.listVoidTaskChecked(function, () -> getPrograms());
	}
	
	@Deprecated
	default WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Episode, Boolean>, Void> getEpisodes
			(Program program, CheckedBiFunction<WorkerProxy, Episode, Boolean> function) {
		return WorkerUpdatableTask.listVoidTaskChecked(function, () -> getEpisodes(program));
	}
	
	@Deprecated
	default WorkerUpdatableTask<CheckedBiFunction<WorkerProxy, Media, Boolean>, Void> getMedia
			(Episode episode, CheckedBiFunction<WorkerProxy, Media, Boolean> function) {
		return WorkerUpdatableTask.listVoidTaskChecked(function, () -> getMedia(episode));
	}
	
	// TODO: Make non-default
	default ListTask<Program> _getPrograms() throws Exception {
		return ListTask.empty();
	}
	
	// TODO: Make non-default
	default ListTask<Episode> _getEpisodes(Program program) throws Exception {
		return ListTask.empty();
	}
	
	// TODO: Make non-default
	default ListTask<Media> _getMedia(Episode episode) throws Exception {
		return ListTask.empty();
	}
}