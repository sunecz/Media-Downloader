package sune.app.mediadown.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import sune.app.mediadown.event.Event;
import sune.app.mediadown.event.EventType;
import sune.app.mediadown.util.CheckedConsumer;
import sune.app.mediadown.util.Pair;
import sune.app.mediadown.util.Utils;

public abstract class ListTask<T> extends Task {
	
	private static ListTask<?> EMPTY;
	
	private final List<T> list;
	
	public ListTask() {
		this(new ArrayList<>());
	}
	
	public ListTask(List<T> list) {
		this.list = Objects.requireNonNull(list);
	}
	
	public static final <T> ListTask<T> of(CheckedConsumer<ListTask<T>> runnable) {
		return new ListTask<T>() {
			
			@Override
			protected void run() throws Exception {
				runnable.accept(this);
			}
		};
	}
	
	public static final <T> ListTask<T> empty() {
		if(EMPTY == null) {
			EMPTY = new ListTask<>(List.of()) {
				
				@Override
				protected void run() throws Exception {
					// Do nothing
				}
			};
		}
		
		return Utils.cast(EMPTY);
	}
	
	public boolean add(T item) throws Exception {
		awaitPaused();
		
		if(isStopped()) {
			return false;
		}
		
		list.add(item);
		
		call(ListTaskEvent.ITEM_ADDED, new Pair<>(this, item));
		
		return true;
	}
	
	public List<T> list() {
		return list;
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public static final class ListTaskEvent implements EventType {
		
		public static final Event<ListTaskEvent, Pair<Task, Object>> ITEM_ADDED = new Event<>();
		
		// TODO: Add missing methods
	}
}