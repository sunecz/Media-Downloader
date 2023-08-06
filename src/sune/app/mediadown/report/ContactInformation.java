package sune.app.mediadown.report;

import java.util.Objects;

/** @since 00.02.09 */
public final class ContactInformation {
	
	private final String email;
	
	public ContactInformation(String email) {
		this.email = Objects.requireNonNull(email);
	}
	
	public String email() {
		return email;
	}
}