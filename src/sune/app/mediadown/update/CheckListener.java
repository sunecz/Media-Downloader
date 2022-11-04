package sune.app.mediadown.update;

@Deprecated
public interface CheckListener {
	
	// Methods
	void begin();
	void compare(String name);
	void end();
	// Listeners
	FileCheckListener fileCheckListener();
}