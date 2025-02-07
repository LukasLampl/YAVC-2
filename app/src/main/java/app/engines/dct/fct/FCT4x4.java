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

package app.engines.dct.fct;

import app.utils.ArrayUtils;

/**
 * The {@code FCT4x4} class is responsible for approximating the DCT
 * and IDCT for 4x4 matrices only.
 * 
 * @author Lukas Lampl
 * @since 1.0.0 [optimized_prototype_2]
 */
public class FCT4x4 implements FCT {
	/**
	 * Holds the size of the transform unit.
	 */
	private static final int N = 4;

	private static final double cos_1 = Math.cos(1.0 * Math.PI / (2.0 * N));
	private static final double cos_2 = Math.cos(2.0 * Math.PI / (2.0 * N));
	private static final double cos_3 = Math.cos(3.0 * Math.PI / (2.0 * N));
	
	private static final double i_S_0 =  Math.sqrt(2.0);
	private static final double i_S_1 =  cos_1 + cos_3;
	private static final double i_S_2 =  1.0 / Math.sqrt(2.0);
	private static final double i_S_3 =  cos_1 - cos_3;
	private static final double i_S_5 =  0.5;
	private static final double i_S_6 =  cos_2 / 2.0;
	
	private static final double S_0 = (cos_1 - cos_3) / 2.0;
	private static final double S_1 = (cos_1 + cos_3) / 2.0;
	private static final double S_2 = 0.5;

	@Override
	public void fct2D(final double[][] matrix, final int offsetX, final int offsetY) {
		double[][] temp = new double[N][N];

		for (int x = 0; x < N; x++) {
			fct1D(matrix[x + offsetX], offsetY, true);
		}

		for (int x = 0; x < N; x++) {
			for (int y = 0; y < N; y++) {
				temp[y][x] = matrix[x + offsetX][y + offsetY];
			}
		}

		for (int y = 0; y < N; y++) {
			fct1D(temp[y], 0, false);
		}

		ArrayUtils.copy2DArray(temp, 0, 0, matrix, offsetX, offsetY, N, N);
	}

	@Override
	public void ifct2D(final double[][] matrix, final int offsetX, final int offsetY) {
		double[][] temp = new double[N][N];

		for (int x = 0; x < N; x++) {
			ifct1D(matrix[x + offsetX], offsetY, false);
		}

		for (int x = 0; x < N; x++) {
			for (int y = 0; y < N; y++) {
				temp[y][x] = matrix[x + offsetX][y + offsetY];
			}
		}

		for (int y = 0; y < N; y++) {
			ifct1D(temp[y], 0, true);
		}

		ArrayUtils.copy2DArray(temp, 0, 0, matrix, offsetX, offsetY, N, N);
	}

	/**
	 * Calculates the 1D version of the FCT and applies the results to the
	 * given array.
	 * 
	 * @param vector	Array from which to get the 1D FCT.
	 * @param offsetY	Offset in the array.
	 * @param rowDCT	Whether the coefficients are converted the first time.
	 */
	private void fct1D(final double[] vector, final int offsetY,
			final boolean rowDCT) {
		double s_0 = vector[0 + offsetY];
		double s_1 = vector[1 + offsetY];
		double s_2 = vector[2 + offsetY];
		double s_3 = vector[3 + offsetY];

		if (rowDCT) {
			s_0 -= OFFSET;
			s_1 -= OFFSET;
			s_2 -= OFFSET;
			s_3 -= OFFSET;
		}
		
		final double s1_0 = s_0 + s_3;
		final double s1_1 = s_1 + s_2;
		final double s1_2 = s_0 - s_3;
		final double s1_3 = s_1 - s_2;

		vector[0 + offsetY] = S_2 * (s1_0 + s1_1);
		vector[1 + offsetY] = S_1 * s1_2 + S_0 * s1_3;
		vector[2 + offsetY] = S_2 * (s1_0 - s1_1);
		vector[3 + offsetY] = S_0 * s1_2 - S_1 * s1_3;
	}

	/**
	 * Calculates the 1D version of the IFCT and applies the results to the
	 * given array.
	 * 
	 * @param vector	Array from which to get the 1D IFCT.
	 * @param offsetY	Offset in the array.
	 * @param rowDCT	Whether the coefficients are converted the first time.
	 */
	private void ifct1D(final double[] vector, final int offsetY,
			final boolean rowIDCT) {
		final double s_0 = vector[0 + offsetY];
		final double s_1 = vector[1 + offsetY];
		final double s_2 = vector[2 + offsetY];
		final double s_3 = vector[3 + offsetY];

		final double s1_0 = i_S_0 * s_0;
		final double s1_1 = i_S_1 * s_1 + i_S_3 * s_3;
		final double s1_2 = i_S_0 * s_2;
		final double s1_3 = -i_S_3 * s_1 + i_S_1 * s_3;
		
		final double s2_4 = i_S_5 * (s1_0 - s1_2);
		final double s2_5 = i_S_2 * s1_3;

		vector[0 + offsetY] = i_S_6 * (s1_0 + s1_2) + i_S_5 * s1_1;
		vector[1 + offsetY] = i_S_2 * (s2_4 - s2_5);
		vector[2 + offsetY] = i_S_2 * (s2_4 + s2_5);
		vector[3 + offsetY] = i_S_6 * (s1_0 + s1_2) - i_S_5 * s1_1;
		
		if (rowIDCT) {
			vector[0 + offsetY] += OFFSET;
			vector[1 + offsetY] += OFFSET;
			vector[2 + offsetY] += OFFSET;
			vector[3 + offsetY] += OFFSET;
		}
	}
}
