package sune.app.mediadown.serialization;

import java.io.IOException;

/** @since 00.02.09 */
public interface SerializationReader extends AutoCloseable {
	
	void open() throws IOException;
	void close() throws IOException;
	
	int read(byte[] buf, int off, int len) throws IOException;
	
	boolean readBoolean() throws IOException;
	byte readByte() throws IOException;
	char readChar() throws IOException;
	short readShort() throws IOException;
	int readInt() throws IOException;
	long readLong() throws IOException;
	float readFloat() throws IOException;
	double readDouble() throws IOException;
	String readString() throws IOException;
	Object readObject() throws IOException;
	
	boolean[] readBooleanArray() throws IOException;
	byte[] readByteArray() throws IOException;
	char[] readCharArray() throws IOException;
	short[] readShortArray() throws IOException;
	int[] readIntArray() throws IOException;
	long[] readLongArray() throws IOException;
	float[] readFloatArray() throws IOException;
	double[] readDoubleArray() throws IOException;
	String[] readStringArray() throws IOException;
	Object[] readObjectArray() throws IOException;
	
	long skip(long n) throws IOException;
	int available() throws IOException;
	String readLine() throws IOException;
}