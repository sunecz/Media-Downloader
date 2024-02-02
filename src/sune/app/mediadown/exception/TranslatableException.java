package sune.app.mediadown.exception;

import java.util.Objects;

/** @since 00.02.09 */
public class TranslatableException extends Exception {
	
	private static final long serialVersionUID = -6595819806387567981L;
	
	protected final String translationPath;
	
	protected TranslatableException(String translationPath) {
		super();
		this.translationPath = Objects.requireNonNull(translationPath);
	}
	
	protected TranslatableException(String translationPath, Throwable cause) {
		super(cause);
		this.translationPath = Objects.requireNonNull(translationPath);
	}
	
	public String translationPath() {
		return translationPath;
	}
}