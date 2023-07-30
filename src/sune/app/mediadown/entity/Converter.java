package sune.app.mediadown.entity;

import sune.app.mediadown.conversion.ConversionCommand;
import sune.app.mediadown.conversion.ConversionContext;

/** @since 00.01.26 */
public interface Converter extends AutoCloseable, ConversionContext {
	
	/** @since 00.02.08 */
	void start(ConversionCommand command) throws Exception;
	void stop() throws Exception;
	void pause() throws Exception;
	void resume() throws Exception;
}