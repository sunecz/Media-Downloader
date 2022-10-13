package sune.app.mediadown;

import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventBindable;

public interface Download extends EventBindable<DownloadEvent>, HasTaskState {
	
	void start() throws Exception;
	void stop() throws Exception;
	void pause() throws Exception;
	void resume() throws Exception;
}