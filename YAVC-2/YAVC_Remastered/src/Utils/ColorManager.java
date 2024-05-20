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

package Utils;

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
	public Color convertYUVToRGB(double Y, double U, double V) {		
		int red = (int)Math.round(Y + 1.402 * (V - 128));
		int green = (int)Math.round(Y - 0.344136 * (U - 128) - 0.714136 * (V - 128));
		int blue = (int)Math.round(Y + 1.772 * (U - 128));
		return new Color(Math.min(Math.max(red, 0), 255), Math.min(Math.max(green, 0), 255), Math.min(Math.max(blue, 0), 255));
	}
	
	public Color convertYUVToRGB(double[] YUV) {		
		return convertYUVToRGB(YUV[0], YUV[1], YUV[2]);
	}
	
	public int[] convertYUVToRGB_intARR(double[] YUV) {		
		int red = (int)Math.round(YUV[0] + 1.402 * (YUV[2] - 128));
		int green = (int)Math.round(YUV[0] - 0.344136 * (YUV[1] - 128) - 0.714136 * (YUV[2] - 128));
		int blue = (int)Math.round(YUV[0] + 1.772 * (YUV[1] - 128));
		return new int[] {Math.min(Math.max(red, 0), 255), Math.min(Math.max(green, 0), 255), Math.min(Math.max(blue, 0), 255)};
	}
	
	
	/*
	 * Purpose: Convert an int RGB color to grayscale
	 * Return Type: int => Grayscale value
	 * Params: int argb => ARGB value to convert
	 */
	public int convertRGBToGRAYSCALE(int argb) {
		Color col = new Color(argb);
		return (int)Math.round(col.getRed() * 0.299 + col.getGreen() * 0.587 + col.getBlue() * 0.114);
	}
}