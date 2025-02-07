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

/**
 * The interface used for defining FCT (Fast Cosine Transform) using approximation.
 * 
 * @author Lukas Lampl
 * @since 1.0.0 [optimized_prototype_2]
 */
public interface FCT {
	/**
	 * Holds the offset to apply to the coefficients for scaling the values.
	 */
	static final int OFFSET = 128;
	
	/**
	 * Computes the 2D Fast Cosine Transform of the given matrix at the specified offset.
	 * 
	 * <p>
	 * The computed coefficients are put back into the given matrix array.
	 * 
	 * @param matrix	Matrix on which apply the FCT.
	 * @param offsetX	Offset to the x direction in the matrix.
	 * @param offsetY	Offset to the y direction in the matrix.
	 */
	public void fct2D(final double[][] matrix, final int offsetX, final int offsetY);
	
	/**
	 * Computes the 2D Inverse Fast Cosine Transform of the given matrix at the specified offset.
	 * 
	 * <p>
	 * The computed coefficients are put back into the given matrix array.
	 * 
	 * @param matrix	Matrix on which apply the FCT.
	 * @param offsetX	Offset to the x direction in the matrix.
	 * @param offsetY	Offset to the y direction in the matrix.
	 */
	public void ifct2D(final double[][] matrix, final int offsetX, final int offsetY);
}
