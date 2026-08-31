package sune.app.mediadown.update;

import sune.app.mediadown.net.Net;
import sune.app.mediadown.util.JSON.JSONCollection;

/** @since 00.02.09 */
public class ArtifactParser {
	
	protected static final byte[] HEX = {
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, -1, -1, -1, -1, -1, -1,
		-1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
	};
	
	protected byte[] decodeHex(String value) {
		int l = value.length();
		if((l & 1) != 0) {
			throw new IllegalArgumentException("Odd length");
		}
		byte[] b = new byte[l >> 1];
		for(int i = 0; i < l; i += 2) {
			int u = HEX[value.charAt(i)];
			int v = HEX[value.charAt(i + 1)];
			int r = (u << 4) + v;
			b[i >> 1] = (byte)r;
		}
		return b;
	}
	
	protected Digest digestOf(String digest) {
		int index = digest.indexOf(':');
		if(index < 0) return null;
		String strType = digest.substring(0, index).toUpperCase();
		DigestType type = DigestType.valueOf(strType);
		if(type == null) return null;
		String strValue = digest.substring(index + 1);
		if(strValue.isEmpty()) return null;
		byte[] value = decodeHex(strValue);
		return new Digest(type, value);
	}
	
	protected Encoding encodingOf(String encoding) {
		switch(encoding) {
			case "gzip": return Encoding.GZIP;
			default:     return null;
		}
	}
	
	public Artifact parse(JSONCollection data) {
		Digest digest = digestOf(data.getString("digest"));
		
		if(digest == null) {
			throw new IllegalArgumentException("Invalid digest: " + data.getString("digest"));
		}
		
		Encoding encoding = encodingOf(data.getString("encoding"));
		
		if(encoding == null) {
			throw new IllegalArgumentException("Invalid encoding: " + data.getString("encoding"));
		}
		
		return new Artifact(
			data.getString("component"),
			data.getString("version"),
			Net.uri(data.getString("url")),
			data.getString("install_path"),
			digest,
			encoding,
			data.getLong("size"),
			data.getLong("encodedSize"),
			data.getBoolean("executable")
		);
	}
}
