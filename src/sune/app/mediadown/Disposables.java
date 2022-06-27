package sune.app.mediadown;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Disposables {
	
	private static final List<Disposable> disposables = new ArrayList<>();
	
	public static final void add(Disposable disposable) {
		if((disposable == null))
			throw new IllegalArgumentException("Disposable object cannot be null");
		disposables.add(disposable);
	}
	
	public static final void dispose() {
		Iterator<Disposable> it;
		for(it = disposables.iterator(); it.hasNext();) {
			Disposable disposable = it.next();
			try {
				disposable.dispose();
			} catch(Exception ex) {
				// Ignore
			}
			it.remove();
		}
	}
	
	// Forbid anyone to create an instance of this class
	private Disposables() {
	}
}