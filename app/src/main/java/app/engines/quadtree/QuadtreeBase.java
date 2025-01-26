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

package app.engines.quadtree;

public abstract class QuadtreeBase {
	/**
	 * The start size and maximum size of a Quadtree MacroBlock.
	 */
	public static final int MAX_SIZE = 128;
	
	/**
	 * Total amount of sizes.
	 * 4x4, 8x8, 16x16, 32x32, 64x64 and 128x128
	 */
	public static final int NUMBER_OF_SIZES = 6;
	
	/**
	 * Index at which to expect 4x4 blocks.
	 */
	public static final int INDEX_4x4 = 0;
	
	/**
	 * Index at which to expect 8x8 blocks.
	 */
	public static final int INDEX_8x8 = 1;
	
	/**
	 * Index at which to expect 16x16 blocks.
	 */
	public static final int INDEX_16x16 = 2;
	
	/**
	 * Index at which to expect 32x32 blocks.
	 */
	public static final int INDEX_32x32 = 3;
	
	/**
	 * Index at which to expect 64x64 blocks.
	 */
	public static final int INDEX_64x64 = 4;
	
	/**
	 * Index at which to expect 128x128 blocks.
	 */
	public static final int INDEX_128x128 = 5;
	
	/**
	 * Get the index in an array with all MacroBlock sizes represented based
	 * on the given size.
	 * 
	 * @param size	The size to convert to an index.
	 * @return The index.
	 */
	public static int getIndexBySize(final int size) {
		switch (size) {
		case 128:
			return INDEX_128x128;
		case 64:
			return INDEX_64x64;
		case 32:
			return INDEX_32x32;
		case 16:
			return INDEX_16x16;
		case 8:
			return INDEX_8x8;
		case 4:
			return INDEX_4x4;
		default:
			throw new IllegalArgumentException("The size " + size + " is currently no supported.");
		}
	}
	
	public static int getSizeByIndex(final int index) {
		switch (index) {
		case INDEX_128x128:
			return 128;
		case INDEX_64x64:
			return 64;
		case INDEX_32x32:
			return 32;
		case INDEX_16x16:
			return 16;
		case INDEX_8x8:
			return 8;
		case INDEX_4x4:
			return 4;
		default:
			throw new IllegalArgumentException("The index " + index + " is currently no supported.");
		}
	}
}
