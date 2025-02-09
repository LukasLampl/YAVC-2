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

package app.engines.prediction.interprediction;

import app.engines.dct.DCTEngine;
import app.utils.components.MacroBlock;

/**
 * <p>The class {@code EncodingVector} is a container structure for storing
 * spatial data, received from the inter-prediction process.</p>
 * 
 * <p><b>Important:</b><br>
 * This should exclusively be used for encoding processes only.
 * </p>
 * 
 * <p><b>Performance warning:</b><br>
 * The process of getting the
 * difference DCT-II coefficients might impact the performance a lot
 * due to the recalculation of all DCT-II coefficients.</p>
 * 
 * @author Lukas Lampl
 * @since 1.0.0 [optimized_prototype_2]
 * 
 * @see app.engines.prediction.interprediction.Vector Vector
 */
public class EncodingVector extends Vector {
	/**
	 * Provides a DCTEngine with pre-calculated cosine table.
	 */
	private static DCTEngine DCT_ENGINE = app.Main.DCT_ENGINE;
	
	/**
	 * The appendedBlock describes the block to be searched in the
	 * inter-prediction section, while the mostEqualBlock is the best match.
	 */
	private MacroBlock appendedBlock = null;
	
	/**
	 * The most equal block found during prediction task.
	 */
	private MacroBlock mostEqualBlock = null;
	
	/**
	 * Holds the YUV delta values in form of DCT coefficients.
	 */
	private double[][][] dctYUVDelta = null;
	
	/**
	 * <p>Initializes the vector for further processing.</p>
	 * 
	 * @param x		X coordinate of the vectors starting point.
	 * @param y		Y coordinate of the vectors starting point.
	 * @param size	Size of the reference MacroBlock.
	 * 
	 * @throws NullPointerException	When the position is null.
	 * @throws IllegalArgumentException	If the area of the reference is 0 or negative.
	 */
	public EncodingVector(final int x, final int y, final int size) {
		super(x, y, size);
	}
	
	/**
	 * <p>This function is never used in the actual code, but provides
	 * a good debugging option.</p>
	 * <p>Sets the appended block "block to be searched".</p>
	 *
	 * @param block	The appended MacroBlock of the vector.
	 */
	public void setAppendedBlock(final MacroBlock block) {
		this.appendedBlock = block;
	}
	
	/**
	 * <p>This function is never used in the actual code, but provides
	 * a good debugging option.</p>
	 * <p>Returns the appended block.</p>
	 * 
	 * @return MacroBlock that was previously appended to the vector.
	 */
	public MacroBlock getAppendedBlock() {
		return this.appendedBlock;
	}
	
	/**
	 * Sets the {@link #mostEqualBlock} to the provided
	 * MacroBlock. The mostEqual MacroBlock describes the best
	 * match in the inter-prediction step.
	 * 
	 * @param block	Block to set as mostEqualBlock
	 */
	public void setMostEqualBlock(final MacroBlock block) {
		this.mostEqualBlock = block;
	}
	
	/**
	 * Get the mostEqualBlock MacroBlock.
	 * 
	 * @return The set mostEqualBlock.
	 */
	public MacroBlock getMostEqualBlock() {
		return this.mostEqualBlock;
	}
	
	/**
	 * Sets the YUV delta of the vector and converts them to DCT representation.
	 * 
	 * @param YUVDelta	The delta values.
	 */
	public void setYUVDelta(final double[][][] YUVDelta) {
		this.dctYUVDelta = DCT_ENGINE.computeDCTOfDeltas(YUVDelta, this.size, true);
	}
	
	/**
	 * Returns the IDCT (original YUV).
	 * 
	 * @return The converted YUV.
	 */
	public double[][][] getIDCTOfDeltas() {
		return DCT_ENGINE.computeIDCTOfDeltas(this.dctYUVDelta, this.size, true);
	}
	
	/**
	 * Gets the YUV delta that was passed to the vector.
	 * 
	 * @return The delta values.
	 */
	public double[][][] getYUVDelta() {
		return this.dctYUVDelta;
	}
	

	/**
	 * Resets the data inside the vector to the standard values, in order
	 * to reuse the vector.
	 */
	public void reset() {
		this.dctYUVDelta = null;
		this.appendedBlock = null;
		this.mostEqualBlock = null;
	}
	
	@Override
	public void discard() {
		super.discard();
		reset();
	}
}
