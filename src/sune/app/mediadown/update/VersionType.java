package sune.app.mediadown.update;

import java.util.stream.Stream;

public enum VersionType {
	
	// Do not change the order!
	// The order is used when comparing two versions.
	
	/** @since 00.02.07 */
	UNKNOWN          (null),
	DEVELOPMENT      ("dev"),
	ALPHA            ("alpha"),
	BETA             ("beta"),
	/** @since 00.02.07 */
	PRE_RELEASE      ("pre"),
	/** @since 00.02.07 */
	RELEASE_CANDIDATE("rc"),
	RELEASE          ("");
	
	private final String string;
	
	private VersionType(String string) {
		this.string = string;
	}
	
	public static final VersionType from(String string) {
		if(string == null || string.isEmpty())
			return UNKNOWN;
		
		return Stream.of(values())
					.filter((v) -> v.string != null && string.indexOf(v.string) == 0)
					.findFirst().orElse(UNKNOWN);
	}
	
	public String string() {
		return string;
	}
}