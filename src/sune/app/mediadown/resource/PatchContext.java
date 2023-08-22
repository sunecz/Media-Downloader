package sune.app.mediadown.resource;

/** @since 00.02.09 */
public interface PatchContext {
	
	Patcher.ResourceList.Entry remoteEntry();
	Patcher.ResourceList.Entry localEntry();
	Exception exception();
}