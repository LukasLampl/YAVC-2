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

package app.utils;

/**
 * Functions as a container for the mean RGB values and individual
 * RGB values of a MacroBlock.
 * 
 * @author Lukas Lampl
 * @since 1.3
 */
public class MeanStructure {
	/**
	 * Array with all 4x4 mean YUV colors.
	 */
	private final double[][][] meanArgbs;
	
	/**
	 * Array with all YUV colors.
	 */
	private final double[][][] argbs;
	
	/**
	 * Creates a new MeanStructure with the given 4x4 mean color and RGB values.
	 * 
	 * @param meanArgbs	The 4x4 mean YUV values.
	 * @param argbs		The YUV values of every pixel.
	 */
	public MeanStructure(final double[][][] meanArgbs, final double[][][] argbs) {
		this.meanArgbs = meanArgbs;
		this.argbs = argbs;
	}
	
	/**
	 * Gets the 4x4 mean YUV value matrix.
	 * @return The 4x4 mean YUV value matrix.
	 */
	public double[][][] get4x4Means() {
		return this.meanArgbs;
	}
	
	/**
	 * Gets the YUV values of all pixels.
	 * 
	 * @return The YUV values of all pixels.
	 */
	public double[][][] getArgbs() {
		return this.argbs;
	}
}
