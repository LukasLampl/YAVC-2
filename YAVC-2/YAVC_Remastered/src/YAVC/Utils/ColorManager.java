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

package YAVC.Utils;

import java.awt.Color;

public class ColorManager {
	/*
	 * Purpose: Convert a RGB color to an YUV color
	 * Return Type: YUVColor => Converted color
	 * Params: Color color => Color to be converted
	 */
	public double[] convertRGBToYUV(Color color) {
		int red = color.getRed(), green = color.getGreen(), blue = color.getBlue();
		double Y = 0.299 * red + 0.587 * green + 0.114 * blue;
		double U = 128 - 0.168736 * red - 0.331264 * green + 0.5 * blue;
		double V = 128 + 0.5 * red - 0.418688 * green - 0.081312 * blue;
		return new double[] {Y, U, V};
	}
	
	/*
	 * Purpose: Convert an int RGB color to an YUV color
	 * Return Type: YUVColor => Converted color
	 * Params: int color => Color to be converted
	 */
	public double[] convertRGBToYUV(int color) {
		int red = (color >> 16) & 0xFF, green = (color >> 8) & 0xFF, blue = color & 0xFF;
		double Y = 0.299 * red + 0.587 * green + 0.114 * blue;
		double U = 128 - 0.168736 * red - 0.331264 * green + 0.5 * blue;
		double V = 128 + 0.5 * red - 0.418688 * green - 0.081312 * blue;
		return new double[] {Y, U, V};
	}
	
	/*
	 * Purpose: Convert a YUV color to an RGB color
	 * Return Type: YUVColor => Converted color
	 * Params: YCbCrColor color => Color to be converted
	 */
	public int convertYUVToRGB(double[] YUV) {
		if (YUV == null) {
			throw new IllegalArgumentException("Can't convert NULL to RGB!");
		} else if (YUV.length != 3) {
			throw new IllegalArgumentException("YUV color contains " + YUV.length + " components instead of 3!");
		}
		
		double Y = YUV[0], U = YUV[1] - 128, V = YUV[2] - 128;
		int red = range((int)Math.round(Y + 1.402 * V), 0, 255);
		int green = range((int)Math.round(Y - 0.344136 * U - 0.714136 * V), 0, 255);
		int blue = range((int)Math.round(Y + 1.772 * U), 0, 255);
		return (0xFF000000 | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF));
	}
	
	public int[] convertYUVToRGB_intARR(double[] YUV, int[] rgbCache) {
		if (YUV == null) {
			throw new IllegalArgumentException("Can't convert NULL to RGB array!");
		} else if (YUV.length != 3) {
			throw new IllegalArgumentException("YUV color contains " + YUV.length + " components instead of 3!");
		}
		
		double Y = YUV[0], U = YUV[1] - 128, V = YUV[2] - 128;
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
	
	/*
	 * Purpose: Convert an int RGB color to grayscale
	 * Return Type: int => Grayscale value
	 * Params: int argb => ARGB value to convert
	 */
	public int convertRGBToGRAYSCALE(int argb) {
		int red = (argb >> 16) & 0xFF, green = (argb >> 8) & 0xFF, blue = argb & 0xFF;
		return (int)Math.round(red * 0.299 + green * 0.587 + blue * 0.114);
	}
	
	private int range(int x, int min, int max) {
		return x < min ? min : x > max ? max : x;
	}
}