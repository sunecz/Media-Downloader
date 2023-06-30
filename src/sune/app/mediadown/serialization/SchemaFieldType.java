package sune.app.mediadown.serialization;

/** @since 00.02.09 */
public final class SchemaFieldType {
	
	// Each type is of format 0bcctttttt (bit notation 0b[8 bits])
	// where: c = type category (array or value)
	//        t = the actual type (byte, int, ...)
	
	public static final int VALUE = 0b01 << 6;
	public static final int ARRAY = 0b10 << 6;
	
	public static final int BOOLEAN = 0b0001;
	public static final int BYTE    = 0b0010;
	public static final int CHAR    = 0b0011;
	public static final int SHORT   = 0b0100;
	public static final int INT     = 0b0101;
	public static final int LONG    = 0b0110;
	public static final int FLOAT   = 0b0111;
	public static final int DOUBLE  = 0b1000;
	public static final int STRING  = 0b1001;
	public static final int OBJECT  = 0b1010;
	
	public static final int MASK_CATEGORY = 0b11000000;
	public static final int MASK_TYPE = 0b00111111;
	
	// Forbid anyone to create an instance of this class
	private SchemaFieldType() {
	}
}