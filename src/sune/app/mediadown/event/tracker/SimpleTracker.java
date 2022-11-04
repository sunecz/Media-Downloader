package sune.app.mediadown.event.tracker;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;

public abstract class SimpleTracker implements Tracker {
	
	/** @since 00.02.08 */
	protected final EventRegistry<TrackerEvent> eventRegistry = new EventRegistry<>();
	
	/** @since 00.02.08 */
	protected final void update() {
		call(TrackerEvent.UPDATE, this);
	}
	
	/** @since 00.02.08 */
	// Do not expose EventCallable methods, they should only be used internally
	protected final <V> void call(Event<? extends TrackerEvent, V> event) {
		eventRegistry.call(event);
	}
	
	/** @since 00.02.08 */
	// Do not expose EventCallable methods, they should only be used internally
	protected final <V> void call(Event<? extends TrackerEvent, V> event, V value) {
		eventRegistry.call(event, value);
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