package sune.app.mediadown.util;

public class NoNullCache extends Cache {
	
	@Override
	protected <T> boolean canAddValue(T instance) {
		// add the value only if it is not null
		return instance != null;
	}
}