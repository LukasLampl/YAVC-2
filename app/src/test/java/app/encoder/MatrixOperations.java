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
	private static Random r = new Random();
	
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
}
