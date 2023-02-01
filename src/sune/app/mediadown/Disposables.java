package sune.app.mediadown;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import sune.app.mediadown.util.Utils.Ignore;

public final class Disposables {
	
	private static final Queue<Disposable> disposables = new ConcurrentLinkedQueue<>();
	
	// Forbid anyone to create an instance of this class
	private Disposables() {
	}
	
	public static final void add(Disposable disposable) {
		if(disposable == null) {
			throw new IllegalArgumentException("Disposable object cannot be null");
		}
		
		disposables.add(disposable);
	}
	
	public static final void dispose() {
		for(Disposable disposable; (disposable = disposables.poll()) != null;) {
			Ignore.callVoid(disposable::dispose);
		}
	}
}