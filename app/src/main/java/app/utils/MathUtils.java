package app.utils;

/**
 * originator: Hans Lampl
 */

public class MathUtils {

	public static double abs(double value) {
		return (value < 0.0) ? -value : value;
	}
	
	public static int round(double value) {
		return value >= 0.0 ? (int)(value + 0.5) : (int)(value - 0.5);
	}
}
