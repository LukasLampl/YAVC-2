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
import app.rendering.ColorManager;
import app.utils.ArrayUtils;
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
	 * yuvDeltaDCTCoefficients is an 3D array containing
	 * the absolute color difference in form of 4x4 or 8x8 DCT matrices.
	 * The invokedDCTOfDifferences is set the true, if the absolute difference was invoked,
	 * else it's false.
	 */
	private double[][][] yuvDeltaDCTCoefficients = null;
	
	/**
	 * Holds the delta values of the vector compared to the reference block in decoded form.
	 * This is only used in the decoder.
	 */
	private double[][][] yuvDelta = null;
	
	/**
	 * Flag for whether the absolute color difference has been invoked or not.
	 */
	private boolean invokedDCTOfDifferences = false;
	
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
	 * Sets the absolute color difference DCT coefficients of the vector,
	 * thus enabling the ability to reconstruct the "original" colors.
	 * 
	 * @param diffs	The prepared array to set.
	 * 
	 * @see app.io.Protocol#getVectorAbsoluteColorDifferenceBytes(double[][][], int)
	 */
	public void setAbsolutedifferenceDCTCoefficients(final double[][][] diffs) {
		if (diffs == null) {
			throw new NullPointerException("Can't use NULL as difference");
		}
		
		this.yuvDeltaDCTCoefficients = diffs;
		this.invokedDCTOfDifferences = true;
	}
	
	/**
	 * Sets the absolute color difference DCT coefficients of the vector,
	 * thus enabling the ability to reconstruct the "original" colors.
	 * 
	 * @param YUVDifference	The prepared array to set.
	 * 
	 * @see app.io.Protocol#getVectorAbsoluteColorDifferenceBytes(double[][][], int)
	 */
	public void setAbsoluteDifferences(final double[][][] YUVDifference) {
		this.yuvDeltaDCTCoefficients = DCT_ENGINE.computeDCTOfVectorColorDifference(YUVDifference, this.size, true);
		this.invokedDCTOfDifferences = true;
	}
	
	/**
	 * Sets the delta components in YUV format that must be applied to the predicted block to
	 * get the original block.
	 * 
	 * @param YUVDelta	The YUV formatted delta.
	 */
	public void setYUVDelta(final double[][][] YUVDelta) {
		this.yuvDelta = YUVDelta;
	}
	
	/**
	 * Get the matrix with all DCT coefficients of color differences.
	 * 
	 * @return Matrix with the DCT coefficients of the color difference.
	 * 
	 * @see app.io.Protocol#getVectorAbsoluteColorDifferenceBytes(double[][][], int)
	 */
	public double[][][] getDCTCoefficientsOfAbsoluteColorDifference() {
		return this.yuvDeltaDCTCoefficients;
	}
	
	/**
	 * This function uses the invoked DCT coefficients
	 * of the absolute color difference to reconstruct the absolute
	 * color difference by using the IDCT.
	 * 
	 * @param allowModificationToOriginalData	Flag for whether the
	 * original data will be copied before processing or not.
	 * 
	 * @return Reconstructed YUV color difference.
	 * 
	 * @throws NullPointerException	If no DCT-Coefficients were invoked.
	 */
	public double[][][] getIDCTCoefficientsOfAbsoluteColorDifference(boolean allowModificationToOriginalData) {
		if (this.invokedDCTOfDifferences == false) {
			throw new NullPointerException("No absolute difference were invoked, NULL DCT-Coefficients to process");
		}
		
		if (allowModificationToOriginalData) {
			return DCT_ENGINE.computeIDCTOfVectorColorDifference(this.yuvDeltaDCTCoefficients, this.size, true);
		}
		
		double[][][] clone = cloneAbsoluteColorDifference();
		return DCT_ENGINE.computeIDCTOfVectorColorDifference(clone, this.size, true);
	}
	
	/**
	 * Gets the YUV delta that must be applied to the predicted block in order to
	 * get the original back.
	 * 
	 * @return The delta values in YUV format.
	 */
	public double[][][] getYUVDelta() {
		return this.yuvDelta;
	}
	
	/**
	 * Clones the {@link #absoluteColorDifferenceDCTCoefficients} array.
	 * This function should be used for getting the IDCT values, since the
	 * original array is referenced and might get quantified by mistake
	 * if not cloned.
	 * 
	 * @return Cloned array with all the data.
	 */
	private double[][][] cloneAbsoluteColorDifference() {
		double[][][] ref = this.yuvDeltaDCTCoefficients;
		double[][][] clone = ArrayUtils.get3DArray(this.size, true);
		
		for (int i = 0; i < ColorManager.CHANNELS; i++) {
			ArrayUtils.copy2DArray(ref[i], 0, 0, clone[i], 0, 0, this.size, this.size);
		}
		
		return clone;
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
		this.invokedDCTOfDifferences = false;
		
		if (this.yuvDeltaDCTCoefficients != null || forceClear) {
			this.yuvDeltaDCTCoefficients = null;
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
