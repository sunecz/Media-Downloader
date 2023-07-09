package sune.app.mediadown.serialization;

import java.io.IOException;

/** @since 00.02.09 */
public interface SerializationWriter extends AutoCloseable {
	
	void open() throws IOException;
	void close() throws IOException;
	void flush() throws IOException;
	
	int write(byte[] buf, int off, int len) throws IOException;
	
	void write(boolean value) throws IOException;
	void write(byte value) throws IOException;
	void write(char value) throws IOException;
	void write(short value) throws IOException;
	void write(int value) throws IOException;
	void write(long value) throws IOException;
	void write(float value) throws IOException;
	void write(double value) throws IOException;
	void write(String value) throws IOException;
	void write(Object value) throws IOException;
	
	void write(boolean[] array) throws IOException;
	void write(byte[] array) throws IOException;
	void write(char[] array) throws IOException;
	void write(short[] array) throws IOException;
	void write(int[] array) throws IOException;
	void write(long[] array) throws IOException;
	void write(float[] array) throws IOException;
	void write(double[] array) throws IOException;
	void write(String[] array) throws IOException;
	void write(Object[] array) throws IOException;
	
	void beginObjectArray(Class<?> clazz) throws IOException;
	void writeObjectArrayItem(Object item) throws IOException;
	void endObjectArray() throws IOException;
}