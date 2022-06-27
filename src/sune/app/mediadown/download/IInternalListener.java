package sune.app.mediadown.download;

import sune.app.mediadown.event.Listener;

public interface IInternalListener {
	
	<E> Listener<E> beginListener();
	<E> Listener<E> updateListener();
	<E> Listener<E> endListener();
	<E> Listener<E> errorListener();
	
	/** @since 00.01.18 */
	default <E> Listener<E> pauseListener() {
		return null;
	}
	/** @since 00.01.18 */
	default <E> Listener<E> resumeListener() {
		return null;
	}
	
	default InternalListenerEventMapper toEventMapper() {
		// wrap it in the default internal listener event mapper
		return new InternalListenerEventMapper(this);
	}
}