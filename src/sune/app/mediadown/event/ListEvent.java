package sune.app.mediadown.event;

import java.util.List;

import sune.app.mediadown.util.Utils;

/** @since 00.02.09 */
public final class ListEvent implements EventType {
	
	public static final Event<ListEvent, ListChange<?>> CHANGED = new Event<>();
	
	private static Event<ListEvent, ?>[] values;
	
	// Forbid anyone to create an instance of this class
	private ListEvent() {
	}
	
	public static final Event<ListEvent, ?>[] values() {
		if(values == null) {
			values = Utils.array(CHANGED);
		}
		
		return values;
	}
	
	public static final class ListChange<T> {
		
		private final List<T> itemsAdded;
		private final List<T> itemsRemoved;
		
		public ListChange(List<T> itemsAdded, List<T> itemsRemoved) {
			this.itemsAdded = itemsAdded == null ? List.of() : itemsAdded;
			this.itemsRemoved = itemsRemoved == null ? List.of() : itemsRemoved;
		}
		
		public static final <T> ListChange<T> ofAdded(List<T> itemsAdded) {
			return new ListChange<T>(itemsAdded, null);
		}
		
		public static final <T> ListChange<T> ofRemoved(List<T> itemsRemoved) {
			return new ListChange<T>(null, itemsRemoved);
		}
		
		public List<T> added() {
			return itemsAdded;
		}
		
		public List<T> removed() {
			return itemsRemoved;
		}
		
		public boolean hasAdded() {
			return !itemsAdded.isEmpty();
		}
		
		public boolean hasRemoved() {
			return !itemsRemoved.isEmpty();
		}
	}
}
