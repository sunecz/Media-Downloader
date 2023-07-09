package sune.app.mediadown.serialization;

/** @since 00.02.09 */
public final class SchemaField {
	
	private final int type;
	private final long offset;
	
	public SchemaField(int type, long offset) {
		this.type = type;
		this.offset = offset;
	}
	
	public int type() {
		return type;
	}
	
	public long offset() {
		return offset;
	}
}