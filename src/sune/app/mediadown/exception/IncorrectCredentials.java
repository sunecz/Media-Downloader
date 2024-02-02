package sune.app.mediadown.exception;

/** @since 00.02.09 */
public class IncorrectCredentials extends TranslatableException {
	
	private static final long serialVersionUID = -7462971936865661126L;
	
	private static final String TRANSLATION_PATH = "errors.credentials.incorrect";
	
	public IncorrectCredentials() {
		super(TRANSLATION_PATH);
	}
	
	public IncorrectCredentials(Throwable cause) {
		super(TRANSLATION_PATH, cause);
	}
}