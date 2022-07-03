package sune.app.mediadown.language;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import sune.util.ssdf2.SSDCollection;
import sune.util.ssdf2.SSDF;
import sune.util.ssdf2.SSDNode;
import sune.util.ssdf2.SSDObject;
import sune.util.ssdf2.SSDType;

/**
 * New version of the Translation API's class for translation's data encapsulation.
 * This class provides better methods for obtaining a text from translation than
 * the previous now-inexistent Translation class, it also supports Unicode characters.
 * @since 00.01.18
 * @author Sune*/
public final class Translation {
	
	private final SSDCollection data;
	
	public Translation(InputStream stream) {
		this(SSDF.read(stream));
	}
	
	public Translation(Path path) {
		this(SSDF.read(path.toFile()));
	}
	
	public Translation(String data) {
		this(SSDF.read(data));
	}
	
	public Translation(SSDCollection data) {
		this.data = data;
	}
	
	public final boolean has(String name) {
		return data.has(name);
	}
	
	public final boolean hasSingle(String name) {
		return data.hasObject(name);
	}
	
	public final boolean hasCollection(String name) {
		return data.hasCollection(name);
	}
	
	public final Translation getTranslation(String name) {
		return new Translation(data.getCollection(name));
	}
	
	public final String getSingle(String name) {
		SSDObject object = data.getObject(name, null);
		if(object == null) return name;
		
		// Also handle concatenated strings
		if(object.getType() == SSDType.STRING_VAR) {
			return SSDObject.ofRaw("", object.toString(false, true)).stringValue();
		}
		
		return object.stringValue();
	}
	
	private static final char CHAR_SIGN = '%';
	private static final char CHAR_OB   = '{';
	private static final char CHAR_CB   = '}';
	public final String getSingle(String name, Map<String, Object> params) {
		String value = getSingle(name);
		StringBuilder sbstr = new StringBuilder();
		StringBuilder sbtmp = new StringBuilder();
		boolean wsign = false;
		boolean fsign = false;
		boolean finbr = false;
		boolean bcadd = true;
		for(int i = 0, l = value.length(), c; i < l; ++i) {
			c     = value.codePointAt(i);
			bcadd = true;
			if((finbr)) {
				if((c == CHAR_CB)) {
					String paramName  = sbtmp.toString();
					Object paramValue = params.get(paramName);
					// Optimize the adding of a parameter's value
					if((paramValue != null)) sbstr.append(paramValue.toString());
					else                     sbstr.append(CHAR_SIGN)
												  .append(CHAR_OB)
												  .append(paramName)
												  .append(CHAR_CB);
					sbtmp.setLength(0);
					finbr = false;
					bcadd = false;
				}
			} else if((fsign)) {
				if((c == CHAR_OB)) {
					sbstr.append(sbtmp);
					sbtmp.setLength(0);
					finbr = true;
					bcadd = false;
				} else {
					// '%%...%' -> '%'
					if(!wsign)
						sbtmp.append(CHAR_SIGN);
				}
				fsign = false;
			} else if((c == CHAR_SIGN)) {
				wsign = true;
				fsign = true;
				bcadd = false;
			} else {
				wsign = false;
			}
			if((bcadd)) {
				sbtmp.appendCodePoint(c);
			}
		}
		// Add the rest of the string
		if((sbtmp.length() > 0)) {
			if((finbr)) {
				sbstr.append(CHAR_SIGN)
					 .append(CHAR_OB);
			}
			sbstr.append(sbtmp);
		}
		return sbstr.toString();
	}
	
	private static final Map<String, Object> paramsToMap(Object... params) {
		if((params.length & 1) != 0)
			throw new IllegalArgumentException("Number of parameters has to be even");
		Map<String, Object> map = new HashMap<>();
		boolean isName = true;
		for(int i = 0, l = params.length; i < l; ++i) {
			if(!isName) {
				Object name  = params[i - 1];
				Object value = params[i];
				map.put(name != null ? name.toString() : null,
				        value);
			}
			isName = !isName;
		}
		return map;
	}
	
	public final String getSingle(String name, Object... params) {
		return getSingle(name, paramsToMap(params));
	}
	
	private static final void merge(SSDCollection data, SSDCollection dataToMerge) {
		for(SSDNode node : dataToMerge) {
			String name = node.getName();
			if((node.isCollection())) {
				SSDCollection coll = (SSDCollection) node;
				if((data.hasCollection(name))) {
					merge(data.getCollection(name), coll);
				} else {
					data.set(name, coll);
				}
			} else {
				if(!data.has(name)) {
					data.set(name, (SSDObject) node);
				}
			}
		}
	}
	
	public final void combine(Translation translation) {
		merge(data, translation.data);
	}
	
	public final SSDCollection getData() {
		return data;
	}
}