package sune.app.mediadown.serialization;

/** @since 00.02.09 */
public final class SchemaField {
	
	private final int type;
	private final long offset;
	private final String name;
	
	public SchemaField(int type, long offset, String name) {
		this.type = type;
		this.offset = offset;
		this.name = name;
	}
	
	public int type() {
		return type;
	}
	
	public long offset() {
		return offset;
	}
	
	public String name() {
		return name;
	}
}