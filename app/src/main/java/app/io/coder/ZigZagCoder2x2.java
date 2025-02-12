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
 * The {@code ZigZagCoder2x2} is an extension of the {@code ZigZagOrder}
 * that is specialized on coding 2x2 matrices into a stream with zig
 * zag ordering. In addition to that it provides a decoding function as well.
 * 
 * @author Lukas Lampl
 * @since 1.2 [QT_COMP]
 * 
 * @see app.io.coder.ZigZagCoder ZigZagCoder
 */
public class ZigZagCoder2x2 implements ZigZagCoder {
	/**
	 * Size of the coder matrix.
	 */
	private final static int N = 2;
	
	/**
	 * Length of the 2D matrix.
	 */
	private final static int LENGTH = N * N;
	
	/**
	 * Order of the zig zag.
	 */
	private final static int[][] ORDER = {
		{0, 2},
		{1, 3}
	};
	
	@Override
	public double[] code(double[][] matrix) {
		final double[] stream = new double[LENGTH];
		
		for (int x = 0; x < N; x++) {
			for (int y = 0; y < N; y++) {
				stream[ORDER[x][y]] = matrix[x][y];
			}
		}
		
		return stream;
	}

	@Override
	public double[][] decode(double[] stream) {
		final double[][] mat = new double[N][N];
		
		for (int x = 0; x < N; x++) {
			for (int y = 0; y < N; y++) {
				mat[x][y] = stream[ORDER[x][y]];
			}
		}
		
		return mat;
	}
}
