package sune.app.mediadown.convert;

import java.nio.file.Path;

/** @since 00.01.26 */
public class ConversionConfiguration {
	
	private final Path destination;
	private final double duration;
	
	public ConversionConfiguration(Path destination, double duration) {
		this.destination = destination;
		this.duration = duration;
	}
	
	public Path getDestination() {
		return destination;
	}
	
	public double getDuration() {
		return duration;
	}
}