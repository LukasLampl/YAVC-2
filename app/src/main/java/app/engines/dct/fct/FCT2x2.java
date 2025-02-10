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
 * The {@code FCT2x2} class is responsible for approximating the DCT
 * and IDCT for 2x2 matrices only.
 * 
 * @author Lukas Lampl
 * @since 1.0.0 [optimized_prototype_2]
 */
public class FCT2x2 implements FCT {
	/**
	 * Holds the size of the transform unit.
	 */
	private static int N = 2;

	private static final double i_S_0 = 1.0 / Math.sqrt(2.0);
	
	private static final double S_0 = 1.0 / Math.sqrt(2.0);

	@Override
	public void fct2D(final double[][] matrix, final int offsetX, final int offsetY) {
		for (int x = 0; x < N; x++) {
			fct1D(matrix[x + offsetX], offsetY, true);
		}

		ArrayUtils.transpose(matrix, N, offsetX, offsetY);

		for (int y = 0; y < N; y++) {
			fct1D(matrix[y], 0, false);
		}
	}

	@Override
	public void ifct2D(final double[][] matrix, final int offsetX, final int offsetY) {
		for (int x = 0; x < N; x++) {
			ifct1D(matrix[x + offsetX], offsetY, false);
		}

		ArrayUtils.transpose(matrix, N, offsetX, offsetY);

		for (int y = 0; y < N; y++) {
			ifct1D(matrix[y], 0, true);
		}
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
		
		if (rowDCT) {
			s_0 -= OFFSET;
			s_1 -= OFFSET;
		}

		vector[0 + offsetY] = S_0 * (s_0 + s_1);
		vector[1 + offsetY] = S_0 * (s_0 - s_1);
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

		vector[0 + offsetY] = i_S_0 * (s_0 + s_1);
		vector[1 + offsetY] = i_S_0 * (s_0 - s_1);
		
		if (rowIDCT) {
			vector[0 + offsetY] += OFFSET;
			vector[1 + offsetY] += OFFSET;
		}
	}
}
