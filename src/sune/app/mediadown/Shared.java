package sune.app.mediadown;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import sune.app.mediadown.util.UserAgent;

public final class Shared {
	
	public static final String  USER_AGENT = UserAgent.CHROME;
	public static final Charset CHARSET    = StandardCharsets.UTF_8;
	
	// Forbid anyone to create an instance of this class
	private Shared() {
	}
}