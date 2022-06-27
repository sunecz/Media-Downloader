package sune.app.mediadown.convert;

import java.io.Closeable;

import sune.app.mediadown.event.ConversionEvent;
import sune.app.mediadown.event.EventBindable;

/** @since 00.01.26 */
public interface Converter extends Closeable, EventBindable<ConversionEvent> {
	
	void start() throws Exception;
	void stop() throws Exception;
	void pause() throws Exception;
	void resume() throws Exception;
	
	boolean isRunning();
	boolean isStarted();
	boolean isDone();
	boolean isPaused();
	boolean isStopped();
}