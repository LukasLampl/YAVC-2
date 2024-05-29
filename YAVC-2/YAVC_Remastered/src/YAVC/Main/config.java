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

package YAVC.Main;

/**
 * <p>The class {@code YAVC.Main.config} contains all universally used
 * constants throughout the YAVC video compressor.</p>
 * 
 * @author Lukas Lampl
 * @since 1.0
 */

public class config {
	public static final int CODING_OFFSET = 15;
	public static final int UTF_8_CODING_SIZE = 8;
	public static final int MAX_REFERENCES = 4;
	
	public static final byte VECTOR_START = (char)1;
	public static final byte VECTOR_DCT_START = (char)2;
	public static final byte VECTOR_U_START = (char)3;
	public static final byte VECTOR_V_START = (char)4;
	public static final byte VECTOR_END = (char)5;

	public static final int[][] QUANTIZATION_MATRIX_8x8_Luma = {
		{16, 11, 10, 16, 24, 40, 51, 61},
		{12, 12, 14, 19, 26, 58, 60, 55},
		{14, 13, 16, 24, 40, 57, 69, 56},
		{14, 17, 22, 29, 51, 87, 80, 62},
		{18, 22, 37, 56, 68, 109, 103, 77},
		{24, 35, 55, 64, 81, 104, 113, 92},
		{49, 64, 78, 87, 103, 121, 120, 101},
		{72, 92, 95, 98, 112, 100, 103, 99}
	};

	public static final int[][] QUANTIZATION_MATRIX_4x4_Luma = {
		{8, 12, 24, 32},
		{5, 14, 25, 34},
		{16, 23, 24, 43},
		{21, 24, 26, 48}
	};
	
	public static final int[][] QUANTIZATION_MATRIX_4x4_Chroma = {
		{8, 12, 26, 48},
		{13, 15, 27, 48},
		{24, 23, 22, 48},
		{48, 48, 48, 48}
	};
	
	public static final int[][] QUANTIZATION_MATRIX_2x2_Chroma = {
		{4, 15},
		{17, 42}
	};
}
