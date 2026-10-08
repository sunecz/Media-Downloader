package sune.app.mediadown;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import sune.app.mediadown.util.Utils.Ignore;

public final class Disposables {
	
	private static final AtomicReference<Node> queue = new AtomicReference<>();
	private static final Node SHUTDOWN = new Node(null, null);
	
	private Disposables() { throw new AssertionError("No instances"); }
	
	public static final void add(Disposable disposable) {
		Objects.requireNonNull(disposable);
		
		Node head;
		do {
			head = queue.get();
			
			if(head == SHUTDOWN) {
				Ignore.callVoid(disposable::dispose);
				return;
			}
		} while(!queue.compareAndSet(head, new Node(disposable, head)));
	}
	
	public static final void dispose() {
		for(Node node = queue.getAndSet(SHUTDOWN);
				node != null && node != SHUTDOWN;
				node = node.next) {
			Ignore.callVoid(node.disposable::dispose);
		}
	}
	
	private static final class Node {
		
		final Disposable disposable;
		final Node next;
		Node(Disposable d, Node n) { disposable = d; next = n; }
	}
}