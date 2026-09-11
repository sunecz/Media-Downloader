package sune.app.mediadown.exception;

/** @since 00.02.02 */
public class UncheckedException extends RuntimeException {
	
	private static final long serialVersionUID = 6443649903230895135L;
	
	public UncheckedException(Exception exception) {
		super(exception);
	}
	
	Exception exception() {
		return (Exception) getCause();
	}
}