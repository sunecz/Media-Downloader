package sune.app.mediadown.resource.cache;

/** @since 00.02.07 */
public class NoNullCache extends Cache {
	
	public NoNullCache() {
		super();
	}
	
	public NoNullCache(boolean supportConcurrency) {
		super(supportConcurrency);
	}
	
	@Override
	protected <T> boolean canAddValue(T instance) {
		// add the value only if it is not null
		return instance != null;
	}
}