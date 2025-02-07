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
import app.utils.components.Component2D;
import app.utils.components.MacroBlock;

/**
 * <p>The class {@code Vector} is a container structure for storing
 * spatial data, received from the inter-prediction process.</p>
 * <p>It is responsible for further processing and usually contains the following
 * information:</p>
 * <ul><li><b>Position</b>: Start position of the vector
 * <li><b>Span</b>: How long a direction of the vector is
 * <li><b>Size</b>: The size of the reference
 * <li><b>Reference</b>: Which frame was used to reference the vector
 * <li><b>Difference</b>: An array of color differences to preserve quality
 * </ul>
 * 
 * <p><strong>Performance warning:</strong> The process of getting the
 * difference DCT-II coefficients might impact the performance a lot
 * due to the recalculation of all DCT-II coefficients.</p>
 * 
 * @author Lukas Lampl
 * @since 1.1.1
 */
public class Vector extends Component2D {
	/**
	 * Provides a DCTEngine with pre-calculated cosine table.
	 */
	private static DCTEngine DCT_ENGINE = app.Main.DCT_ENGINE;
	
	/**
	 * The individual spans of the vector, measured in pixels.
	 */
	private int spanX = 0;
	private int spanY = 0;
	
	/**
	 * The reference frame, from which the block is referred to.
	 */
	private int reference = 0;
	
	/**
	 * The appendedBlock describes the block to be searched in the
	 * inter-prediction section, while the mostEqualBlock is the best match.
	 */
	private MacroBlock appendedBlock = null;
	private MacroBlock mostEqualBlock = null;
	
	/**
	 * Holds the delta values of the vector compared to the reference block.
	 * This can be used by the encoder as well as the decoder.
	 * Both will holds the data in different forms, eg. the encoder in DCT coeffs,
	 * while the decoder in actual YUV values.
	 */
	private double[][][] yuvDelta = null;
	
	/**
	 * Flag for whether the delta YUV has been invoked or not.
	 */
	private boolean invokedYUVDelta = false;
	
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
	public Vector(final int x, final int y, final int size) {
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
	 * The function sets the spanX of the vector to a given integer.
	 * The span starts from the origin (startPoint).
	 * 
	 * <p><b>Warning:</b><br>
	 * The span should be in between -127 and 127 to fit into a byte.
	 * </p>
	 * 
	 * @param span	Span in the x direction.
	 */
	public void setSpanX(final int span) {
		this.spanX = span;
	}
	
	/**
	 * The function sets the spanY of the vector to a given integer.
	 * The span starts from the origin (startPoint).
	 * 
	 * <p><b>Warning:</b><br>
	 * The span should be in between -127 and 127 to fit into a byte.
	 * </p>
	 * 
	 * @param span	Span in the y direction.
	 */
	public void setSpanY(final int span) {
		this.spanY = span;
	}
	
	/**
	 * Sets the reference frame of the {@code mostEqualBlock} meaning
	 * that this number represents, out of which frame the mostEqualBlock
	 * was extracted from.
	 * 
	 * <p><b>Note:</b><br> The max reference is set by
	 * {@link app.config#MAX_REFERENCES config.MAX_REFERENCES}.<br>
	 * </p>
	 * 
	 * @param reference	Reference frame number.
	 */
	public void setReference(final int reference) {
		this.reference = reference;
	}
	
	/**
	 * Get the x span of the vector.
	 * 
	 * @return Span x of the vector.
	 */
	public int getSpanX() {
		return this.spanX;
	}
	
	/**
	 * Get the y span of the vector.
	 * 
	 * @return Span y of the vector.
	 */
	public int getSpanY() {
		return this.spanY;
	}
	
	/**
	 * Get the reference frame number of the vectors reference.
	 * 
	 * @return Reference number.
	 */
	public int getReference() {
		return this.reference;
	}

	/**
	 * Sets the YUV delta of the vector. If it is a coding unit (encoded) the
	 * DCT coefficients are calculated, else the raw values are stored.
	 * 
	 * @param YUVDelta	The delta values.
	 * @param encoding	Whether it is a coding unit or not. (Encoding process)
	 */
	public void setYUVDelta(final double[][][] YUVDelta, final boolean encoding) {
		if (encoding) {
			this.yuvDelta = DCT_ENGINE.computeDCTOfDeltas(YUVDelta, this.size, true);
			this.invokedYUVDelta = true;
		} else {
			this.yuvDelta = YUVDelta;
		}
	}
	
	/**
	 * Returns the IDCT (original YUV) or the delta YUV.
	 * 
	 * @return The converted YUV.
	 */
	public double[][][] getIDCTOfDeltas() {
		if (this.invokedYUVDelta == false) {
			throw new NullPointerException("No absolute difference were invoked, NULL DCT-Coefficients to process");
		}
		
		return DCT_ENGINE.computeIDCTOfDeltas(this.yuvDelta, this.size, true);
	}
	
	/**
	 * Gets the YUV delta that was passed to the vector.
	 * 
	 * @return The delta values.
	 */
	public double[][][] getYUVDelta() {
		return this.yuvDelta;
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
	 * Resets the data inside the vector to the standard values, in order
	 * to reuse the vector.
	 */
	public void reset(boolean forceClear) {
		this.spanX = 0;
		this.spanY = 0;
		this.reference = 0;
		this.appendedBlock = null;
		this.mostEqualBlock = null;
		this.invokedYUVDelta = false;
		
		if (this.yuvDelta != null || forceClear) {
			this.yuvDelta = null;
		}
	}
	
	@Override
	public void discard() {
		super.discard();
		reset(true);
	}
	
	@Override
	public int hashCode() {
		int res = 0;
		res |= ((this.positionX & 0xFFFF) ^ (this.positionY & 0xFFFF)) << 16;
		res |= (this.reference & 0xFF) ^ (this.spanX & 0xFF) ^ (this.spanY & 0xFF) << 8;
		res |= (this.size & 0xFF);
		return res;
	}
}
