package sune.app.mediadown.download;

import sune.app.mediadown.event.DownloadEvent;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.event.IEventMapper;
import sune.app.mediadown.event.Listener;

@Deprecated(forRemoval=true)
public final class InternalListenerEventMapper implements IEventMapper<DownloadEvent> {
	
	private final IInternalListener listener;

	public InternalListenerEventMapper(IInternalListener listener) {
		this.listener = listener;
	}
	
	@Override
	public Listener<?> map(EventType<DownloadEvent, ?> type) {
		if((type == DownloadEvent.BEGIN))  return listener.beginListener();
		if((type == DownloadEvent.UPDATE)) return listener.updateListener();
		if((type == DownloadEvent.END))    return listener.endListener();
		if((type == DownloadEvent.ERROR))  return listener.errorListener();
		if((type == DownloadEvent.PAUSE))  return listener.pauseListener();
		if((type == DownloadEvent.RESUME)) return listener.resumeListener();
		return null;
	}
}