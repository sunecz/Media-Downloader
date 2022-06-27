package sune.app.mediadown.util;

@FunctionalInterface
public interface CheckedRunnable {
	
	void run() throws Exception;
}