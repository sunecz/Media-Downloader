package sune.app.mediadown.download;

import sune.app.mediadown.HasTaskState;
import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventBindable;

public interface Download extends EventBindable<DownloadEvent>, HasTaskState {
	
	void start() throws Exception;
	void stop() throws Exception;
	void pause() throws Exception;
	void resume() throws Exception;
}