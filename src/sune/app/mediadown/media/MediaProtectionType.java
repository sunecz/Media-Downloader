package sune.app.mediadown.media;

/** @since 00.02.09 */
public enum MediaProtectionType {
	
	DRM_WIDEVINE, DRM_PLAYREADY, DRM_FAIRPLAY, UNKNOWN;
	
	public static final MediaProtectionType ofUUID(String uuid) {
		if(uuid == null || uuid.isEmpty()) {
			return MediaProtectionType.UNKNOWN;
		}
		
		final String prefix = "urn:uuid:";
		
		if(uuid.startsWith(prefix)) {
			uuid = uuid.substring(prefix.length());
		}
		
		// Reference: https://dashif.org/identifiers/content_protection/
		switch(uuid) {
			case "edef8ba9-79d6-4ace-a3c8-27dcd51d21ed": return MediaProtectionType.DRM_WIDEVINE;
			case "9a04f079-9840-4286-ab92-e65be0885f95": return MediaProtectionType.DRM_PLAYREADY;
			case "94ce86fb-07ff-4f43-adb8-93d2fa968ca2": return MediaProtectionType.DRM_FAIRPLAY;
			default: return MediaProtectionType.UNKNOWN;
		}
	}
}