package sune.app.mediadown.download;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sune.app.mediadown.event.DownloadStateEvent;
import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventBindable;
import sune.app.mediadown.event.EventRegistry;
import sune.app.mediadown.event.Listener;
import sune.app.mediadown.util.Bitmap;

/** @since 00.02.09 */
public abstract class DownloadState implements EventBindable<DownloadStateEvent> {
	
	protected final EventRegistry<DownloadStateEvent> eventRegistry = new EventRegistry<>();
	
	protected DownloadState() {
	}
	
	public int current() {
		return 0;
	}
	
	public int total() {
		return 0;
	}
	
	public List<DownloadedRange> downloaded() {
		return null;
	}
	
	protected final void update() {
		eventRegistry.call(DownloadStateEvent.UPDATE, this);
	}
	
	@Override
	public <V> void addEventListener(Event<? extends DownloadStateEvent, V> event, Listener<V> listener) {
		eventRegistry.add(event, listener);
	}
	
	@Override
	public <V> void removeEventListener(Event<? extends DownloadStateEvent, V> event, Listener<V> listener) {
		eventRegistry.remove(event, listener);
	}
	
	public static final class DownloadedRange {
		
		private int from; // Including
		private int to; // Excluding
		
		public DownloadedRange(int from, int to) {
			this.from = from;
			this.to = to;
		}
		
		public void from(int from) { this.from = from; }
		public void to(int to) { this.to = to; }
		
		public int from() { return from; }
		public int to() { return to; }
		
		@Override
		public String toString() {
			return "<" + from + ", " + to + ")";
		}
	}
	
	public static final class OfSegments extends DownloadState {
		
		private final Bitmap bitmap;
		private final int total;
		private int current;
		
		private final List<DownloadedRange> ranges = new LinkedList<>();
		
		public OfSegments(int count) {
			this.bitmap = new Bitmap(count);
			this.total = count;
		}
		
		private final void updateRanges(int index) {
			int insertIndex = ranges.size();
			int idx = 0;
			
			// Try to merge to an existing range
			for(DownloadedRange range : ranges) {
				if(range.from() - index == 1) { // Merge from left
					range.from(index);
					insertIndex = -1;
					break;
				} else if(index - range.to() == 0) { // Merge from right
					range.to(index + 1);
					insertIndex = -1;
					break;
				} else if(range.from() > index) {
					insertIndex = idx;
					break;
				}
				
				++idx;
			}
			
			if(insertIndex >= 0) {
				ranges.add(insertIndex, new DownloadedRange(index, index + 1));
			}
			
			// Combine touching ranges
			Iterator<DownloadedRange> it = ranges.iterator();
			for(DownloadedRange prev = null, range; it.hasNext(); prev = range) {
				range = it.next();
				
				if(prev != null && prev.to() == range.from()) {
					prev.to(range.to());
					it.remove();
				}
			}
		}
		
		public void set(int index) {
			bitmap.set(index);
			++current;
			synchronized(ranges) {
				updateRanges(index);
			}
			update();
		}
		
		public boolean get(int index) {
			return bitmap.get(index);
		}
		
		@Override
		public int current() {
			return current;
		}
		
		@Override
		public int total() {
			return total;
		}
		
		@Override
		public List<DownloadedRange> downloaded() {
			synchronized(ranges) {
				return List.copyOf(ranges);
			}
		}
	}
	
	public static final class OfBlocks extends DownloadState {

		private final long blockSize;
		
		public OfBlocks(long blockSize) {
			this.blockSize = blockSize;
		}
		
		public void set(long start, long end) {
			
		}
		
		public void unset(long start, long end) {
			
		}
	}
}