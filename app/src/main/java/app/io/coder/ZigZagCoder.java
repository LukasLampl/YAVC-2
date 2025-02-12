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
 * The {@code ZigZagCoder} is the base of all ZigZagCoders for
 * setting DCT coefficients in a more compressing format.
 * 
 * @author Lukas Lampl
 * @since 1.2 [QT_COMP]
 */
public interface ZigZagCoder {
	/**
	 * Encodes the given matrix into a zig zag order stream.
	 * 
	 * @param matrix	Matrix to encode.
	 * @return An array with the zig zag ordering.
	 */
	public double[] code(double[][] matrix);
	
	/**
	 * Decodes a given zig zag coded stream into the original matrix.
	 * 
	 * @param stream	Stream to decode.
	 * @return The decoded matrix.
	 */
	public double[][] decode(double[] stream);
}
