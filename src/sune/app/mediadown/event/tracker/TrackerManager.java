package sune.app.mediadown.event.tracker;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.util.Pair;

public class TrackerManager implements EventBindable<TrackerEvent> {
	
	private final Listener<Tracker> onUpdate = this::update;
	private final Listener<Pair<Tracker, Exception>> onError = this::error;
	private final EventRegistry<TrackerEvent> eventRegistry = new EventRegistry<>();
	private Tracker tracker;
	
	public TrackerManager() {
	}
	
	/** @since 00.02.08 */
	public TrackerManager(Tracker tracker) {
		tracker(tracker);
	}
	
	private final void update(Tracker tracker) {
		eventRegistry.call(TrackerEvent.UPDATE, tracker);
	}
	
	private final void error(Pair<Tracker, Exception> pair) {
		eventRegistry.call(TrackerEvent.ERROR, pair);
	}
	
	private final void unbind() {
		if(tracker == null) {
			return;
		}
		
		tracker.removeEventListener(TrackerEvent.UPDATE, onUpdate);
		tracker.removeEventListener(TrackerEvent.ERROR, onError);
	}
	
	private final void bind() {
		if(tracker == null) {
			return;
		}
		
		tracker.addEventListener(TrackerEvent.UPDATE, onUpdate);
		tracker.addEventListener(TrackerEvent.ERROR, onError);
	}
	
	public void tracker(Tracker tracker) {
		unbind();
		this.tracker = tracker; // May be null
		bind();
		update(tracker);
	}
	
	public Tracker tracker() {
		return tracker;
	}
	
	/** @since 00.02.08 */
	@Override
	public <V> void addEventListener(Event<? extends TrackerEvent, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	/** @since 00.02.08 */
	@Override
	public <V> void removeEventListener(Event<? extends TrackerEvent, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
}