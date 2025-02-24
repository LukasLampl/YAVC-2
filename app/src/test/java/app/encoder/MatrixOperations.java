/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2025  Lukas Nian En Lampl, Hans Lampl

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

package app.encoder;

import java.awt.Color;
import java.util.Random;

import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.MathUtils;

public abstract class MatrixOperations {
	private static Random r = new Random(42L);
	
	public static double[][][] generateRandom3DMatrix(final int size, final double scale) {
		double[][][] matrix = ArrayUtils.get3DArray(size, true);
		
		for (int i = 0; i < ColorManager.CHANNELS; i++) {
			for (int x = 0; x < matrix[i].length; x++) {
				for (int y = 0; y < matrix[i][x].length; y++) {
					matrix[i][x][y] = r.nextDouble() * scale;
				}
			}
		}
		
		return matrix;
	}
	
	public static double[][] generateRandom2DMatrix(final int size, final double scale) {
		double[][] matrix = new double[size][size];
		
		for (int x = 0; x < matrix.length; x++) {
			for (int y = 0; y < matrix[x].length; y++) {
				matrix[x][y] = r.nextDouble() * scale;
			}
		}
		
		return matrix;
	}
	
	public static double[][] generateRoundedRandom2DMatrix(final int size, final double scale) {
		double[][] matrix = new double[size][size];
		
		for (int x = 0; x < matrix.length; x++) {
			for (int y = 0; y < matrix[x].length; y++) {
				matrix[x][y] = MathUtils.round(r.nextDouble() * scale);
			}
		}
		
		return matrix;
	}
	
	public static double[][] generateColorBased2DMatrix(final int size) {
		double[][] matrix = new double[size][ColorManager.CHANNELS];
		
		for (int i = 0; i < size; i++) {
			final int red = MathUtils.round(r.nextDouble() * 255);
			final int green = MathUtils.round(r.nextDouble() * 255);
			final int blue = MathUtils.round(r.nextDouble() * 255);
			matrix[i] = ColorManager.convertRGBToYUV(new Color(red, green, blue));
		}
		
		return matrix;
	}
	
	public static byte[] generateRandomByteMatrix(final int length) {
		byte[] data = new byte[length];
		
		for (int i = 0; i < length; i++) {
			data[i] = (byte)MathUtils.round(r.nextDouble() * 255);
		}
		
		return data;
	}
	
	public static byte[] generateIncreasingByteMatrix(final int length, final int min, final int max) {
		int range = max - min;
		double stepSize = (double)range / (double)length;
		
		byte[] data = new byte[length];
		
		for (int i = 0; i < length; i++) {
			data[i] = (byte)MathUtils.round(i * stepSize);
		}
		
		return data;
	}
	
	public static byte[] generateSteadyByteMatrix(final int length, final byte b) {
		byte[] data = new byte[length];
		
		for (int i = 0; i < length; i++) {
			data[i] = b;
		}
		
		return data;
	}
	
	public static double[][] generateRGBColor(final int length) {
		double[][] cols = new double[length][ColorManager.CHANNELS];
		
		for (int i = 0; i < length; i++) {
			final int r = MathUtils.round(Math.random() * 255);
			final int g = MathUtils.round(Math.random() * 255);
			final int b = MathUtils.round(Math.random() * 255);
			cols[i] = ColorManager.convertRGBToYUV(new Color(r, g, b));
		}
		
		return cols;
	}
}
