package sune.app.mediadown.search;

import java.util.function.BiFunction;

import javafx.scene.image.Image;
import sune.app.mediadown.Program;
import sune.app.mediadown.engine.MediaEngine;
import sune.app.mediadown.util.WorkerProxy;
import sune.app.mediadown.util.WorkerUpdatableTask;

/** @since 00.01.17 */
public interface SearchEngine {
	
	// functional methods
	SearchResult[] search(String text, SearchOptions options);
	
	// asynchronous functional methods
	default WorkerUpdatableTask<BiFunction<WorkerProxy, SearchResult, Boolean>, Void> search
		(String text, SearchOptions options,
		 BiFunction<WorkerProxy, SearchResult, Boolean> function) {
		// just return the value of getEpisodes method
		return WorkerUpdatableTask.arrayVoidTask(function, () -> search(text, options));
	}
	
	// additional methods
	Program getProgram(SearchResult resultItem);
	/** @since 00.01.18 */
	MediaEngine getMediaEngine(Program program);
	
	// informative methods
	String getTitle();
	String getURL();
	String getVersion();
	String getAuthor();
	Image  getIcon();
}