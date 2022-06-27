package sune.app.mediadown.util;

import java.util.concurrent.Callable;

public final class FXContext {
	
	private static boolean fxInitializedBySelf;
	private static boolean implicitExit = true;
	
	// Forbid anyone to create an instance of this class
	private FXContext() {
	}
	
	private static final void ensureFX() {
		if(!FXUtils.isInitialized()) {
			FXUtils.init();
			fxInitializedBySelf = true;
		}
	}
	
	private static final void disposeFX() {
		if((implicitExit
				&& fxInitializedBySelf)) {
			FXUtils.exit();
		}
	}
	
	public static final void setImplicitExit(boolean value) {
		implicitExit = value;
	}
	
	public static final <T> T doTaskThrow(Callable<T> task) throws Exception {
		if((task == null))
			throw new NullPointerException();
		ensureFX();
		T result = task.call();
		disposeFX();
		return result;
	}
	
	public static final <T> T doTask(Callable<T> task) {
		// Do this check outside try-catch
		if((task == null))
			throw new NullPointerException();
		try {
			return doTaskThrow(task);
		} catch(Exception ex) {
			// Ignore
		}
		return null;
	}
	
	public static final void tryExit() {
		setImplicitExit(true);
		disposeFX();
	}
	
	public static final boolean isImplicitExit() {
		return implicitExit;
	}
}