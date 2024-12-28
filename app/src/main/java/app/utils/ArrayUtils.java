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

package app.utils;

import app.config;
import app.rendering.ColorManager;

/**
 * The class {@code ArrayUtils} provides basic functions
 * for manipulating data in arrays.
 */
public class ArrayUtils {
	/**
	 * Copies a given array ({@code src}) to a second array ({@code dest}).
	 * 
	 * <p><b>Note:</b><br>
	 * This function also adjusts the boundaries if the given copyWidth or
	 * copyHeight is out of bounds for the destination array.
	 * </p>
	 * 
	 * @param src			The source array to copy.
	 * @param srcX			The offset to the X in the source array.
	 * @param srcY			The offset to the Y in the source array.
	 * @param dest			The destination array to copy into.
	 * @param destX			The X offset in the destination array.
	 * @param destY			The Y offset in the destination array.
	 * @param copyWidth		The length to copy in the x direction.
	 * @param copyHeight	The length to copy in the y direction.
	 * 
	 * @throws ArrayIndexOutOfBoundsException	When either a position is < 0
	 * 											or a position is greater than the array.
	 * @throws IllegalArgumentException	When either the {@code src} of {@code dest} is {@code null}.
	 */
	public static void copy2DArray(final double[][] src, final int srcX, final int srcY,
									double[][] dest, final int destX, final int destY,
									int copyWidth, int copyHeight) {
		if (src == null || dest == null) {
			throw new IllegalArgumentException("Can't copy \"null\".");
		} else if (destX < 0 || destY < 0 || srcX < 0 || srcY < 0) {
			throw new ArrayIndexOutOfBoundsException("Positions cannot be < 0.");
		} else if (destX >= dest.length || srcX >= src.length) {
			throw new ArrayIndexOutOfBoundsException("Positions in X cannot be greater than the array itself.");
		} else if (destY >= dest[0].length || srcY >= src[0].length) {
			throw new ArrayIndexOutOfBoundsException("Positions in Y cannot be greater than the array itself.");
		}
		
		if (destY + copyHeight > dest[0].length) {
			copyHeight = dest[0].length - destY;
		}
		
		if (destX + copyWidth > dest.length) {
			copyWidth = dest.length - destX;
		}
		
		for (int x = 0; x < copyWidth; x++) {
			System.arraycopy(src[x + srcX], srcY, dest[x + destX], destY, copyHeight);
		}
	}
	
	/**
	 * Creates an empty 3D array that has the given size and can be subsampled
	 * on the second and third channel.
	 * 
	 * @param size			Size of the 3D array.
	 * @param subsampled	Whether the second and third channel should be subsampled or not.
	 * @return The created array.
	 */
	public static double[][][] get3DArray(final int size, final boolean subsampled) {
		final int channelSize = subsampled ? size / config.SUBSAMPLE_COEFFICIENT : size;
		double[][][] arr = new double[ColorManager.CHANNELS][][];
		arr[0] = new double[size][size];
		arr[1] = new double[channelSize][channelSize];
		arr[2] = new double[channelSize][channelSize];
		return arr;
	}
	
	/**
	 * Copies the data from the given source to the given destination with the given
	 * offsets and length.
	 * 
	 * @param src		Source to copy from.
	 * @param srcX		The offset on the source.
	 * @param dest		Destination array in which to copy into.
	 * @param destX		Offset in the destination array.
	 * @param length	Length of data to copy.
	 */
	public static void copyArray(final byte[] src, final int srcX, byte[] dest, final int destX, final int length) {
		System.arraycopy(src, srcX, dest, destX, length);
	}
}
