package sune.app.mediadown.util;

import java.util.Locale;

public class MathUtils {
	
	public static final double ANGLE_360_RAD = 2.0 * Math.PI;
	public static final double ANGLE_360_DEG = 360.0;
	
	public static final double normalizeRad(double rad) {
		return (rad + ANGLE_360_RAD) % ANGLE_360_RAD;
	}
	
	public static final double normalizeDeg(double deg) {
		return (deg + ANGLE_360_DEG) % ANGLE_360_DEG;
	}
	
	public static final String round(double val, int decimals) {
		double tens  = Math.pow(10.0, decimals);
		double round = Math.floor(val * tens) / tens;
		return String.format(Locale.US, "%." + decimals + "f", round).replace(',', '.');
	}
	
	// find the nearest multiple of mult relative to the val
	public static final double near(double val, double mult) {
		double dif = val % mult;
		return val + (dif >= mult * 0.5 ? mult - dif : -dif);
	}
	
	public static final float near(float val, float mult) {
		float dif = val % mult;
		return val + (dif >= mult * 0.5f ? mult - dif : -dif);
	}
	
	public static final float min(float... vals) {
		float min = vals[0], val;
		for(int i = 1, l = vals.length; i < l; ++i) {
			val = vals[i];
			if((val < min))
				min = val;
		}
		return min;
	}
	
	public static final float max(float... vals) {
		float max = vals[0], val;
		for(int i = 1, l = vals.length; i < l; ++i) {
			val = vals[i];
			if((val > max))
				max = val;
		}
		return max;
	}
}