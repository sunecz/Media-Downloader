package sune.app.mediadown.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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
		
		call(ListTaskEvent.ADD, new Pair<>(this, item));
		
		return true;
	}
	
	public boolean addAll(List<T> list) throws Exception {
		for(T item : list) {
			if(!add(item)) {
				return false;
			}
		}
		
		return true;
	}
	
	public <W extends T> void forwardAdd(ListTask<W> other) {
		forward(other, ListTaskEvent.ADD, ListTask::add, (p) -> Utils.<W>cast(p.b));
	}
	
	public <W extends T, V> void forwardAdd(ListTask<V> other, Function<W, V> transform) {
		forward(other, ListTaskEvent.ADD, ListTask::add, (p) -> transform.apply(Utils.<W>cast(p.b)));
	}
	
	// Convenience method
	public List<T> startAndGet() throws Exception {
		startAndWait();
		return list();
	}
	
	public List<T> list() {
		return list;
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public static final class ListTaskEvent implements EventType {
		
		public static final Event<ListTaskEvent, Pair<Task, Object>> ADD = new Event<>();
		
		private static Event<ListTaskEvent, ?>[] values;
		
		// Forbid anyone to create an instance of this class
		private ListTaskEvent() {
		}
		
		public static final Event<ListTaskEvent, ?>[] values() {
			if(values == null) {
				values = Utils.array(ADD);
			}
			
			return values;
		}
	}
}