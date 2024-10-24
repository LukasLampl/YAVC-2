/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2024  Lukas Nian En Lampl

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package utils;

import java.awt.Color;

/**
 * <p>The class {@code ColorManager} contains basic functions 
 * for converting RGB-colorspace to the YUV-colorspace and back.</p>
 * The formulas are based on the <u>Rec. 601 (ITU-T T.871)</u> Y'CbCr and
 * have the full 8 bit range from 0 to 255. Due to the
 * YUV-colorspace, colors that exceed the range of 8 bits
 * get reduced to the 8 bit range.
 * Minimum is 0 and maximum is 255.
 * 
 * <p><strong>Performance warning:</strong><br>
 * The conversion from RGB to
 * YUV and back involve floatingpoint-arithmetic, which might impact
 * performance, if used frequently.</p>
 * 
 * @author Lukas Lampl
 * @since 17.0
 * @version 1.0 29 May 2024
 */

public class ColorManager {
	/**
	 * Convert a RGB color, based on a Color object
	 * to YUV using the Rec. 601 (ITU-T T.871) conversion.
	 * 
	 * @return <p>Returns a double[], that contains the
	 * Y, U and V component at the following indexes:
	 * </p><p>
	 * <ul><li>double[0] = Y
	 * <li>double[1] = U
	 * <li>double[2] = V
	 * </ul></p>
	 * 
	 * @param color	RGB color that should be
	 * converted to an YUV color.
	 */
	public static double[] convertRGBToYUV(Color color) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		double Y = 0.299 * r + 0.587 * g + 0.114 * b;
		double U = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b;
		double V = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b;
		return new double[] {Y, U, V};
	}
	
	/**
	 * <p>Convert a RGB color, based on an integer
	 * to YUV using the Rec. 601 (ITU-T T.871) conversion. 
	 * If a color component is bigger than 8 bits, it'll get
	 * cut off by masking.</p>
	 * 
	 * @return <p>Returns a double[], that contains the
	 * Y, U and V component at the following indexes:
	 * </p>
	 * <ul><li>double[0] = Y
	 * <li>double[1] = U
	 * <li>double[2] = V
	 * </ul>
	 * 
	 * @param color	RGB color that should be
	 * converted to an YUV color.
	 */
	public static double[] convertRGBToYUV(int color) {
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		double Y = 0.299 * r + 0.587 * g + 0.114 * b;
		double U = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b;
		double V = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b;
		return new double[] {Y, U, V};
	}
	
	/**
	 * <p>Convert a YUV color to RGB using a double[]
	 * as input, where Y is at [0], U at [1] and V at [2].</p>
	 * <p>The converted color is stored in an integer
	 * with the following order of the components:</p>
	 * <ul><li>First 8 bits:	Alpha
	 * <li>Bits from 8 to 16:	Red
	 * <li>Bits from 16 to 8:	Green
	 * <li>Last 8 bits:			Blue
	 * </ul> 
	 * <p>The order is the same as in the {@code java.awt.Color}
	 * Object.</p>
	 * 
	 * @return Integer with the described order above
	 * 
	 * @param YUV	The YUV color to be converted
	 * to a RGB color
	 * 
	 * @throws IllegalArgumentException	if the provided YUV is
	 * null or the length is not 3
	 */
	public static int convertYUVToRGB(double[] YUV) {
		if (YUV == null) {
			throw new IllegalArgumentException("Can't convert NULL to RGB!");
		} else if (YUV.length != 3) {
			throw new IllegalArgumentException("YUV color contains " + YUV.length + " components instead of 3!");
		}
		
		double Y = YUV[0];
		double U = YUV[1] - 128;
		double V = YUV[2] - 128;
		int red = range((int)Math.round(Y + 1.402 * V), 0, 255);
		int green = range((int)Math.round(Y - 0.344136 * U - 0.714136 * V), 0, 255);
		int blue = range((int)Math.round(Y + 1.772 * U), 0, 255);
		return (0xFF000000 | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF));
	}
	
	/**
	 * <p>Convert a YUV color to RGB using a double[]
	 * as input, where Y is at [0], U at [1] and V at [2].</p>
	 * <p>The converted color is stored in an integer array
	 * with the following order of the components:</p>
	 * <ul><li>int[0]: Red
	 * <li>int[1]: Green
	 * <li>int[2]: Blue
	 * </ul>
	 * 
	 * @return int[] with the red, green and blue values
	 * in the order described above.
	 * 
	 * @param YUV	The YUV color to be converted
	 * to a RGB color
	 * @param rgbCache	An optional int array, that should
	 * be used to store the data. (Faster than initializing each array)
	 * 
	 * @throws IllegalArgumentException	When the YUV array is null,
	 * when the length of the YUV color exceeds 3 or is shorter than 3
	 * and if the rgbCache is not null, but does not have a length of 3.
	 */
	public static int[] convertYUVToRGB_intARR(double[] YUV, int[] rgbCache) {
		if (YUV == null) {
			throw new IllegalArgumentException("Can't convert NULL to RGB array!");
		} else if (YUV.length != 3) {
			throw new IllegalArgumentException("YUV color contains " + YUV.length + " components instead of 3!");
		} else if (rgbCache != null && rgbCache.length != 3) {
			throw new IllegalArgumentException("RGB cache has length of " + rgbCache.length + "instead of 3");
		}
		
		double Y = YUV[0];
		double U = YUV[1] - 128;
		double V = YUV[2] - 128;
		int red = (int)Math.round(Y + 1.402 * V);
		int green = (int)Math.round(Y - 0.344136 * U - 0.714136 * V);
		int blue = (int)Math.round(Y + 1.772 * U);
		
		if (rgbCache != null) {
			rgbCache[0] = red;
			rgbCache[1] = green;
			rgbCache[2] = blue;
			return null;
		}
		
		return new int[] {range(red, 0, 255), range(green, 0, 255), range(blue, 0, 255)};
	}

	/**
	 * <p>Checks if the value of x is bigger than the max
	 * or smaller than the min and returns based on that.</p>
	 * 
	 * @return int with the ranged value.
	 * <ul><li>If x is smaller than min, min will be returned.
	 * <li>If x is bigger than max, max will be returned
	 * <li>If x is in between or equal to min or max, x is returned.
	 * </ul>
	 * 
	 * @param x	Number to clamp
	 * @param min	Minimum value that x is allowed to reach
	 * @param max	Maximum value that x is allowed to reach
	 */
	private static int range(int x, int min, int max) {
		return x < min ? min : x > max ? max : x;
	}
}