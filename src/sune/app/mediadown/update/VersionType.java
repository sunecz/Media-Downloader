package sune.app.mediadown.update;

public enum VersionType {
	
	// Do not change the order!
	// The order is used when comparing two versions.
	
	ALPHA      ("alpha"),
	BETA       ("beta"),
	RELEASE    (""),
	DEVELOPMENT("dev");
	
	private final String string;
	private VersionType(String string) {
		this.string = string;
	}
	
	public static final VersionType from(String string) {
		for(VersionType type : values()) {
			if((type == RELEASE))
				// skip the release version type, since it has an empty string
				continue;
			String strType = type.getString();
			if((string.indexOf(strType)) == 0) {
				return type;
			}
		}
		// if nothing is matched, return the release version type
		return RELEASE;
	}
	
	public static final String remove(String string, VersionType type) {
		return string.substring(type.getString().length());
	}
	
	public String getString() {
		return string;
	}
}