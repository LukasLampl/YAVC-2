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

package app.io.coder;

/**
 * The {@code ZigZagCoder8x8} is an extension of the {@code ZigZagOrder}
 * that is specialized on coding 8x8 matrices into a stream with zig
 * zag ordering. In addition to that it provides a decoding function as well.
 * 
 * @author Lukas Lampl
 * @since 1.2 [QT_COMP]
 * 
 * @see app.io.coder.ZigZagCoder ZigZagCoder
 */
public class ZigZagCoder8x8 implements ZigZagCoder {
	/**
	 * Size of the coder matrix.
	 */
	private final static int N = 8;
	
	/**
	 * Length of the 2D matrix.
	 */
	private final static int LENGTH = N * N;
	
	/**
	 * Order of the zig zag.
	 */
	private final static int[][] ORDER = {
		{ 0,  2,  3,  9, 10, 20, 21, 35},
		{ 1,  4,  8, 11, 19, 22, 34, 36},
		{ 5,  7, 12, 18, 23, 33, 37, 48},
		{ 6, 13, 17, 24, 32, 38, 47, 49},
		{14, 16, 25, 31, 39, 46, 50, 57},
		{15, 26, 30, 40, 45, 51, 56, 58},
		{27, 29, 41, 44, 52, 55, 59, 62},
		{28, 42, 43, 53, 54, 60, 61, 63}
	};
	
	@Override
	public double[] code(final double[][] matrix, final int offsetX, final int offsetY) {
		final double[] stream = new double[LENGTH];
		
		for (int x = 0; x < N; x++) {
			for (int y = 0; y < N; y++) {
				stream[ORDER[x][y]] = matrix[x + offsetX][y + offsetY];
			}
		}
		
		return stream;
	}

	@Override
	public double[][] decode(final double[] stream, final int streamOffset) {
		final double[][] mat = new double[N][N];
		
		for (int x = 0; x < N; x++) {
			for (int y = 0; y < N; y++) {
				mat[x][y] = stream[ORDER[x][y] + streamOffset];
			}
		}
		
		return mat;
	}
}
