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

package app;

/**
 * <p>The class {@code config} contains all universally used
 * constants throughout the YAVC video compressor.</p>
 * 
 * @author Lukas Lampl
 * @since 1.0
 */

public class config {
	public static final int CODING_OFFSET = 15;
	public static final int UTF_8_CODING_SIZE = 8;
	public static final int MAX_REFERENCES = 4;
	
	public static final byte VECTOR_START = (byte)0x01;
	public static final byte VECTOR_DCT_START = (byte)0x02;
	public static final byte VECTOR_U_START = (byte)0x03;
	public static final byte VECTOR_V_START = (byte)0x04;
	public static final byte VECTOR_END = (byte)0x05;

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
		{8, 12, 20, 44},
		{12, 14, 23, 46},
		{27, 25, 39, 48},
		{44, 44, 48, 48}
	};
	
	public static final int[][] QUANTIZATION_MATRIX_2x2_Chroma = {
		{4, 48},
		{48, 48}
	};

	public static final int DEBLOCKER_ALPHAS[] = {
		0, 3, 5, 8, 10, 13, 15, 18, 21, 23, 26, 28, 31, 33,
		36, 39, 41, 44, 46, 49, 52, 54, 57, 59, 62, 64, 67,
		70, 72, 75, 77, 80, 82, 85, 88, 90, 93, 95, 98, 100,
		103, 106, 108, 111, 113, 116, 118, 121, 124, 126, 129,
		131, 134, 137, 139, 142, 144, 147, 149, 152, 155, 157,
		160, 162, 165, 167, 170, 173, 175, 178, 180, 183, 185, 188,
		191, 193, 196, 198, 201, 203, 206, 209, 211, 214, 216, 219,
		222, 224, 227, 229, 232, 234, 237, 240, 242, 245, 247, 250,
		252, 255
	};

	public static final int DEBLOCKER_BETAS[] = {
		0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9,
		10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17,
		17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 23, 24, 24,
		25, 25, 26, 26, 27, 27, 28, 28, 29, 29, 30, 30, 31, 31, 32,
		32, 33, 33, 34, 34, 35, 35, 36, 36, 37, 37, 38, 38, 39, 39,
		40, 40, 41, 41, 42, 42, 43, 43, 44, 44, 45, 45, 46, 46, 47,
		47, 48, 48, 49, 49, 50
	};

	public static final int DEBLOCKER_CS[] = {
		0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9,
		10, 10, 11, 11, 12, 12, 13, 13, 14, 14, 15, 15, 16, 16, 17,
		17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 23, 24, 24,
		25, 25, 26, 26, 27, 27, 28, 28, 29, 29, 30, 30, 31, 31, 32,
		32, 33, 33, 34, 34, 35, 35, 36, 36, 37, 37, 38, 38, 39, 39,
		40, 40, 41, 41, 42, 42, 43, 43, 44, 44, 45, 45, 46, 46, 47,
		47, 48, 48, 49, 49, 50
	};
}
