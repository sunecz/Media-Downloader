package sune.app.mediadown.media;

import java.util.Objects;

/** @since 00.02.09 */
public final class MediaProtection {
	
	private final MediaProtectionType type;
	private final String scheme;
	private final String contentType;
	private final String content;
	
	private MediaProtection(MediaProtectionType type, String scheme, String contentType, String content) {
		this.type = Objects.requireNonNull(type);
		this.scheme = Objects.requireNonNull(scheme);
		this.contentType = Objects.requireNonNull(contentType);
		this.content = Objects.requireNonNull(content);
	}
	
	public static final Builder of(MediaProtectionType type) {
		return new Builder(type);
	}
	
	public static final Builder ofWidevine() {
		return of(MediaProtectionType.DRM_WIDEVINE);
	}
	
	public static final Builder ofPlayReady() {
		return of(MediaProtectionType.DRM_PLAYREADY);
	}
	
	public static final Builder ofFairPlay() {
		return of(MediaProtectionType.DRM_FAIRPLAY);
	}
	
	public static final Builder ofUnknown() {
		return of(MediaProtectionType.UNKNOWN);
	}
	
	public MediaProtectionType type() {
		return type;
	}
	
	public String scheme() {
		return scheme;
	}
	
	public String contentType() {
		return contentType;
	}
	
	public String content() {
		return content;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(content, contentType, scheme, type);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		MediaProtection other = (MediaProtection) obj;
		return Objects.equals(content, other.content)
		        && Objects.equals(contentType, other.contentType)
		        && Objects.equals(scheme, other.scheme)
		        && type == other.type;
	}
	
	@Override
	public String toString() {
		return "MediaProtection["
					+ "type=" + type + ", "
					+ "scheme=" + scheme + ", "
					+ "contentType=" + contentType + ", "
					+ "content=" + content
		        + "]";
	}
	
	public static final class Builder {
		
		private final MediaProtectionType type;
		private String scheme;
		private String contentType;
		private String content;
		
		public Builder(MediaProtectionType type) {
			this.type = Objects.requireNonNull(type);
		}
		
		public MediaProtection build() {
			return new MediaProtection(type, scheme, contentType, content);
		}
		
		public Builder scheme(String scheme) {
			this.scheme = scheme;
			return this;
		}
		
		public Builder contentType(String contentType) {
			this.contentType = contentType;
			return this;
		}
		
		public Builder content(String content) {
			this.content = content;
			return this;
		}
		
		public MediaProtectionType type() {
			return type;
		}
		
		public String scheme() {
			return scheme;
		}
		
		public String contentType() {
			return contentType;
		}
		
		public String content() {
			return content;
		}
	}
}