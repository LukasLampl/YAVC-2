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

import java.awt.Point;

import app.engines.dct.DCTEngine;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.Discardable;
import app.utils.MacroBlock;

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
 * @since 1.1.0
 */


public class Vector implements Discardable {
	/**
	 * Provides a DCTEngine with pre-calculated cosine table.
	 */
	private static DCTEngine DCT_ENGINE = app.Main.DCT_ENGINE;
	
	/**
	 * The starting point of the vector.
	 */
	private int startingPointX = 0;
	private int startingPointY = 0;
	
	/**
	 * The individual spans of the vector, measured in pixels.
	 */
	private int spanX = 0;
	private int spanY = 0;
	
	/**
	 * The size of the reference block.
	 */
	private int size = 0;
	
	/**
	 * The squared size of the vector.
	 */
	private int squaredSize = 0;
	
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
	 * AbsoluteColorDifferenceDCTCoefficients is an 3D array containing
	 * the absolute color difference in form of 4x4 or 8x8 DCT matrices.
	 * The invokedDCTOfDifferences is set the true, if the absolute difference was invoked,
	 * else it's false.
	 * 
	 * @see app.io.Protocol#getVectorAbsoluteColorDifferenceBytes(double[][][], int)
	 */
	private double[][][] absoluteColorDifferenceDCTCoefficients = null;
	
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
		if (size <= 0) {
			throw new IllegalArgumentException("Vector can't have a 0 or negative area of reference");
		}
		
		this.startingPointX = x;
		this.startingPointY = y;
		this.size = size;
		this.squaredSize = size * size;
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
	 * Sets the size of the mostEqualBlock as well as
	 * the size of the appendedBlock.
	 * 
	 * @param size	Size of the appendedBlock.
	 */
	public void setSize(final int size) {
		this.size = size;
		this.squaredSize = size * size;
	}
	
	/**
	 * Get the position of the vector.
	 * 
	 * @return Position of the vector.
	 */
	public Point getPosition() {
		return new Point(this.startingPointX, this.startingPointY);
	}
	
	/**
	 * Sets the starting point of the vector.
	 * 
	 * @param x		X coordinate of the vectors starting point.
	 * @param y		Y coordinate of the vectors starting point.
	 */
	public void setPosition(final int x, final int y) {
		this.startingPointX = x;
		this.startingPointY = y;
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
	 * Get the size of the vector reference.
	 * 
	 * @return Size of the vector reference.
	 */
	public int getSize() {
		return this.size;
	}
	
	/**
	 * Get the squared size of the vector reference.
	 * 
	 * @return The squared size of the vector reference.
	 */
	public int getSquaredSize() {
		return this.squaredSize;
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
		
		this.absoluteColorDifferenceDCTCoefficients = diffs;
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
		this.absoluteColorDifferenceDCTCoefficients = DCT_ENGINE.computeDCTOfVectorColorDifference(YUVDifference, this.size, true);
		this.invokedDCTOfDifferences = true;
	}
	
	/**
	 * Get the matrix with all DCT coefficients of color differences.
	 * 
	 * @return Matrix with the DCT coefficients of the color difference.
	 * 
	 * @see app.io.Protocol#getVectorAbsoluteColorDifferenceBytes(double[][][], int)
	 */
	public double[][][] getDCTCoefficientsOfAbsoluteColorDifference() {
		return this.absoluteColorDifferenceDCTCoefficients;
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
			return DCT_ENGINE.computeIDCTOfVectorColorDifference(this.absoluteColorDifferenceDCTCoefficients, this.size, true);
		}
		
		double[][][] clone = cloneAbsoluteColorDifference();
		return DCT_ENGINE.computeIDCTOfVectorColorDifference(clone, this.size, true);
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
		double[][][] ref = this.absoluteColorDifferenceDCTCoefficients;
		double[][][] clone = ArrayUtils.get3DArray(size, true);
		
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
	public void reset(final int x, final int y, final int size, boolean forceClear) {
		this.startingPointX = x;
		this.startingPointY = y;
		this.size = size;
		this.squaredSize = this.size * this.size;
		this.spanX = 0;
		this.spanY = 0;
		this.reference = 0;
		this.appendedBlock = null;
		this.mostEqualBlock = null;
		this.invokedDCTOfDifferences = false;
		
		if (this.absoluteColorDifferenceDCTCoefficients != null || forceClear) {
			this.absoluteColorDifferenceDCTCoefficients = null;
		}
	}
	
	@Override
	public void discard() {
		reset(0, 0, 0, true);
	}
	
	@Override
	public int hashCode() {
		int res = 0;
		res |= ((this.startingPointX & 0xFFFF) ^ (this.startingPointY & 0xFFFF)) << 16;
		res |= (this.reference & 0xFF) ^ (this.spanX & 0xFF) ^ (this.spanY & 0xFF) << 8;
		res |= (this.size & 0xFF);
		return res;
	}
}
