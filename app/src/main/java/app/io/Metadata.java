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

package app.io;

import java.awt.Dimension;

/**
 * A container for Metadata out of an YAVC file.
 */
public class Metadata {
	/**
	 * The total frame number denoted in the YAVC file.
	 */
	private int frameNumber = 0;
	
	/**
	 * Dimension of all frames.
	 */
	private Dimension dimensionOfFrames = new Dimension(1, 1);
	
	/**
	 * Creates a Metadata container with the given data.
	 * 
	 * @param frameNumber		Number of frames in the file.
	 * @param dimensionOfFrames	Dimension of all frames.
	 */
	public Metadata(int frameNumber, Dimension dimensionOfFrames) {
		this.frameNumber = frameNumber;
		this.dimensionOfFrames = dimensionOfFrames;
	}
	
	/**
	 * Gets the frame number inside the YAVC file.
	 * 
	 * @return The number of frames in the YAVC file.
	 */
	public int getFrameNumber() {
		return this.frameNumber;
	}
	
	/**
	 * Gets the dimension of all frames.
	 * 
	 * @return The dimension of all frames.
	 */
	public Dimension getDimensionOfFrames() {
		return this.dimensionOfFrames;
	}
}
