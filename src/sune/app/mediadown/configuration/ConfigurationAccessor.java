package sune.app.mediadown.configuration;

import java.util.Map;

import sune.app.mediadown.configuration.Configuration.ConfigurationProperty;
import sune.util.ssdf2.SSDCollection;

/** @since 00.02.04 */
public interface ConfigurationAccessor {
	
	boolean booleanValue(String name);
	long longValue(String name);
	int intValue(String name);
	short shortValue(String name);
	char charValue(String name);
	byte byteValue(String name);
	double doubleValue(String name);
	float floatValue(String name);
	String stringValue(String name);
	<T> T typeValue(String name);
	Map<String, ConfigurationProperty<?>> arrayValue(String name);
	Map<String, ConfigurationProperty<?>> objectValue(String name);
	Object nullValue(String name);
	SSDCollection data();
}