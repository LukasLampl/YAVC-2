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
 * The {@code FCT8x8} class is responsible for approximating the DCT
 * and IDCT for 8x8 matrices only.
 * 
 * @author Lukas Lampl
 * @since 1.0.0 [optimized_prototype_2]
 */
public class FCT8x8 implements FCT {
	/**
	 * Holds the size of the transform unit.
	 */
	private static int N = 8;

	private static final double cos_1 = Math.cos(1.0 * Math.PI / (2.0 * N));
	private static final double cos_2 = Math.cos(2.0 * Math.PI / (2.0 * N));
	private static final double cos_3 = Math.cos(3.0 * Math.PI / (2.0 * N));
	private static final double cos_4 = Math.cos(4.0 * Math.PI / (2.0 * N));
	private static final double cos_5 = Math.cos(5.0 * Math.PI / (2.0 * N));
	private static final double cos_6 = Math.cos(6.0 * Math.PI / (2.0 * N));
	private static final double cos_7 = Math.cos(7.0 * Math.PI / (2.0 * N));

	private static final double i_S_0 = -(cos_1 - cos_7);
	private static final double i_S_1 = cos_3 - cos_5;
	private static final double i_S_2 = Math.sqrt(2.0);
	private static final double i_S_3 = cos_2 + cos_6;
	private static final double i_S_4 = cos_1 - cos_7;
	private static final double i_S_6 = 1.0 / Math.sqrt(2.0);
	private static final double i_S_7 = cos_2 - cos_6;
	private static final double i_S_8 = cos_1 + cos_7;
	private static final double i_S_9 = 0.5;
	private static final double i_S_A = 0.25;
	private static final double i_S_B = cos_4 / 2.0;
	private static final double i_S_C = cos_3 + cos_5;
	
	private static final double S_0 = -(cos_1 - cos_7);
	private static final double S_1 = cos_3 - cos_5;
	private static final double S_2 = cos_2 / 2.0;
	private static final double S_3 = cos_1 - cos_7;
	private static final double S_4 = cos_6 / 2.0;
	private static final double S_5 = 1.0 / Math.sqrt(2.0);
	private static final double S_6 = cos_1 + cos_7;
	private static final double S_7 = cos_4 / 2.0;
	private static final double S_8 = cos_3 + cos_5;
	
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
		double s_4 = vector[4 + offsetY];
		double s_5 = vector[5 + offsetY];
		double s_6 = vector[6 + offsetY];
		double s_7 = vector[7 + offsetY];
		
		if (rowDCT) {
			s_0 -= OFFSET;
			s_1 -= OFFSET;
			s_2 -= OFFSET;
			s_3 -= OFFSET;
			s_4 -= OFFSET;
			s_5 -= OFFSET;
			s_6 -= OFFSET;
			s_7 -= OFFSET;
		}

		final double s1_0 = s_0 + s_7;
		final double s1_1 = s_1 + s_6;
		final double s1_2 = s_2 + s_5;
		final double s1_3 = s_3 + s_4;
		final double s1_4 = s_0 - s_7;
		final double s1_5 = s_1 - s_6;
		final double s1_6 = s_2 - s_5;
		final double s1_7 = s_3 - s_4;
		
		final double s2_0 = s1_0 + s1_3;
		final double s2_1 = s1_1 + s1_2;
		final double s2_2 = s1_0 - s1_3;
		final double s2_3 = s1_1 - s1_2;
		final double s2_4 = S_8 * s1_4 + S_1 * s1_7;
		final double s2_5 = S_6 * s1_5 + S_3 * s1_6;
		final double s2_6 = S_0 * s1_5 + S_6 * s1_6;
		final double s2_7 = S_1 * s1_4 - S_8 * s1_7;
		
		final double s3_0 = S_7 * (s2_4 - s2_5);
		final double s3_1 = S_7 * (s2_6 - s2_7);

		vector[0 + offsetY] = S_7 * (s2_0 + s2_1);
		vector[1 + offsetY] = S_7 * (s2_4 + s2_5);
		vector[2 + offsetY] = S_2 * s2_2 + S_4 * s2_3;
		vector[3 + offsetY] = S_5 * (s3_0 - s3_1);
		vector[4 + offsetY] = S_7 * (s2_0 - s2_1);
		vector[5 + offsetY] = S_5 * (s3_0 + s3_1);
		vector[6 + offsetY] = S_4 * s2_2 - S_2 * s2_3;
		vector[7 + offsetY] = S_7 * (s2_6 + s2_7);
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
		final double v0 = vector[0 + offsetY];
		final double v1 = vector[1 + offsetY];
		final double v2 = vector[2 + offsetY];
		final double v3 = vector[3 + offsetY];
		final double v4 = vector[4 + offsetY];
		final double v5 = vector[5 + offsetY];
		final double v6 = vector[6 + offsetY];
		final double v7 = vector[7 + offsetY];
		
		final double s1_0 = i_S_2 * v0;
		final double s1_1 = i_S_C * v1 + i_S_1 * v7;
		final double s1_2 = i_S_3 * v2 + i_S_7 * v6;
		final double s1_3 = i_S_8 * v3 + i_S_4 * v5;
		final double s1_4 = i_S_2 * v4;
		final double s1_5 = i_S_0 * v3 + i_S_8 * v5;
		final double s1_6 = i_S_7 * v2 - i_S_3 * v6;
		final double s1_7 = -i_S_1 * v1 + i_S_C * v7;
		
		final double s2_0 = s1_0 + s1_4;
		final double s2_1 = s1_1 + s1_3;
		final double s2_2 = i_S_2 * s1_2;
		final double s2_3 = s1_0 - s1_4;
		final double s2_4 = s1_1 - s1_3;
		final double s2_5 = i_S_B * (s2_0 - s2_2);
		final double s2_6 = i_S_B * (s2_3 + s2_4);
		final double s2_7 = i_S_B * (s2_3 - s2_4);
		
		final double s3_0 = i_S_2 * s1_6;
		final double s3_1 = s1_5 + s1_7;
		final double s3_2 = s1_5 - s1_7;
		final double s3_3 = i_S_B * (s3_0 + s3_1);
		final double s3_4 = i_S_B * (s3_0 - s3_1);
		final double s3_5 = i_S_9 * s3_2;
		final double s3_6 = -s3_4;
			
		vector[0 + offsetY] = i_S_A * (s2_0 + s2_2) + i_S_B * s2_1;
		vector[1 + offsetY] = i_S_6 * (s2_6 - s3_6);
		vector[2 + offsetY] = i_S_6 * (s2_6 + s3_6);
		vector[3 + offsetY] = i_S_6 * (s2_5 + s3_5);
		vector[4 + offsetY] = i_S_6 * (s2_5 - s3_5);
		vector[5 + offsetY] = i_S_6 * (s2_7 - s3_3);
		vector[6 + offsetY] = i_S_6 * (s2_7 + s3_3);
		vector[7 + offsetY] = i_S_A * (s2_0 + s2_2) - i_S_B * s2_1;
		
		if (rowIDCT) {
			vector[0 + offsetY] += OFFSET;
			vector[1 + offsetY] += OFFSET;
			vector[2 + offsetY] += OFFSET;
			vector[3 + offsetY] += OFFSET;
			vector[4 + offsetY] += OFFSET;
			vector[5 + offsetY] += OFFSET;
			vector[6 + offsetY] += OFFSET;
			vector[7 + offsetY] += OFFSET;
		}
	}
}
