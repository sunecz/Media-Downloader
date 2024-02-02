package sune.app.mediadown.exception;

/** @since 00.02.09 */
public class MissingCredentials extends TranslatableException {
	
	private static final long serialVersionUID = -5876895620697796286L;
	
	private static final String TRANSLATION_PATH = "errors.credentials.missing";
	
	public MissingCredentials() {
		super(TRANSLATION_PATH);
	}
	
	public MissingCredentials(Throwable cause) {
		super(TRANSLATION_PATH, cause);
	}
}