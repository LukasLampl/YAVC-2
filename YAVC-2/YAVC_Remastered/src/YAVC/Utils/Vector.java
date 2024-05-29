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

package YAVC.Utils;

import java.awt.Point;
import java.util.ArrayList;

import YAVC.Encoder.DCTEngine;

/**
 * The class {@code YAVC.Utils.Vector} is a container structure for storing
 * spatial data, received from the inter-prediction process. It is responsible
 * for further processing and usually contains the following information:
 * Position: Start position of the vector
 * Span: How long a direction of the vector is
 * Size: The size of the reference
 * Reference: Which frame was used to reference the vector
 * Difference: An array of color differences to preserve quality
 * 
 * <strong>Performance warning:</strong> The process of getting the
 * difference DCT-II coefficients might impact the performance a lot
 * due to the recalculation of all DCT-II coefficients.
 * 
 * @author Lukas Lampl
 * @since 1.0
 */


public class Vector {
	/**
	 * Provides a DCTEngine with pre-calculated cosine table
	 */
	private DCTEngine DCT_ENGINE = null;
	
	/**
	 * The starting point of the vector
	 */
	private Point startingPoint = null;
	
	/**
	 * The individual spans of the vector, measured in pixels
	 */
	private int spanX = 0;
	private int spanY = 0;
	
	/**
	 * The size of the reference block
	 */
	private int size = 0;
	
	/**
	 * The reference frame, from which the block is reffered to
	 */
	private int reference = 0;
	
	/**
	 * The appendedBlock describes the block to be searched in the
	 * inter-prediction section, while the mostEqualBlock is the best match.
	 */
	private MacroBlock appendedBlock = null;
	private MacroBlock mostEqualBlock = null;
	
	/**
	 * AbsoluteColorDifferenceDCTCoefficients is an ArrayList containing
	 * the absolute color difference in form of 4x4 or 8x8 DCT matrices.
	 * The invokedDCTOfDifferences is set the true, if the absolute difference was invoked,
	 * else it's false
	 */
	private ArrayList<double[][][]> AbsoluteColorDifferenceDCTCoefficients = null;
	private boolean invokedDCTOfDifferences = false;
	
	/**
	 * Initializes the vector for further processing
	 * @param Point pos => starting position of the vector
	 * @param int size => size of the reference MacroBlock
	 */
	public Vector(final Point pos, final int size) {
		if (pos == null) throw new NullPointerException("Vector can't have NULL point as startingPoint");
		
		this.startingPoint = pos;
		this.size = size;
		this.DCT_ENGINE = YAVC.Main.Main.DCT_ENGINE;
	}
	
	/**
	 * @apiNote This function is never used in the actual code, but provides
	 * a good debugging option
	 * @param MacroBlock block => The appended MacroBlock of the vector
	 */
	public void setAppendedBlock(final MacroBlock block) {
		this.appendedBlock = block;
	}
	
	/**
	 * This function is never used in the actual code, but provides
	 * a good debugging option
	 * @return MacroBlock => MacroBlock that was previously appended to the vector
	 */
	public MacroBlock getAppendedBlock() {
		return this.appendedBlock;
	}
	
	/**
	 * The function sets the spanX of the vector to a given integer.
	 * The span starts from the origin (startPoint).
	 * 
	 * <strong>Warning:</strong> the set span might exceed the encoding
	 * maximum of 8 bits = 255. The offset is excluded
	 * ({@code YAVC.config.CODING_OFFSET}).
	 * 
	 * @param int span => Span to the x direction
	 */
	public void setSpanX(final int span) {
		this.spanX = span;
	}
	
	/**
	 * The function sets the spanY of the vector to a given integer.
	 * The span starts from the origin (startPoint).
	 * 
	 * <strong>Warning:</strong> the set span might exceed the encoding
	 * maximum of 8 bits = 255. The offset is excluded
	 * ({@code YAVC.config.CODING_OFFSET}).
	 * 
	 * @param int span => Span to the y direction
	 */
	public void setSpanY(final int span) {
		this.spanY = span;
	}
	
	/**
	 * Sets the reference frame of the mostEqualBlock, meaning
	 * that this number represents, out of which frame the mostEqualBlock
	 * was extracted from.
	 * 
	 * <strong>Warning:</strong> the max reference is set by
	 * {@code YAVC.config.MAX_REFERENCES}. The encoding only
	 * supports till 4!
	 * 
	 * @param int reference => reference frame number
	 */
	public void setReference(final int reference) {
		this.reference = reference;
	}
	
	/**
	 * Sets the size of the mostEqualBlock as well as
	 * the size of the appendedBlock
	 * @param int size => Size of the appendedBlock
	 */
	public void setSize(final int size) {
		this.size = size;
	}
	
	public Point getPosition() {
		return this.startingPoint;
	}
	
	public int getSpanX() {
		return this.spanX;
	}
	
	public int getSpanY() {
		return this.spanY;
	}
	
	public int getSize() {
		return this.size;
	}
	
	public int getReference() {
		return this.reference;
	}
	
	/**
	 * Sets the AbsoluteColorDifferenceDCTCoefficients to the
	 * prepared list, that is provided by the param. The order is as followed:
	 * ArrayList order: Left-to-Right & Top-to-Bottom
	 * Double Order: [0] = Y-DCT; [1] = U-DCT; [2] = V-DCT
	 * @param ArrayList<double[][][]> diffs => The prepared list to set
	 */
	public void setAbsolutedifferenceDCTCoefficients(final ArrayList<double[][][]> diffs) {
		if (diffs == null) throw new NullPointerException("Can't use NULL as difference");
		
		this.AbsoluteColorDifferenceDCTCoefficients = diffs;
		this.invokedDCTOfDifferences = true;
	}
	
	/**
	 * Sets the AbsoluteColorDifferenceDCTCoefficients.
	 * First the provided YUVDifference, with the order
	 * [0] = Y; [1] = U; [2] = V is passed to the DCTEngine, which
	 * calculates the DCT-Coefficients for each part individually.
	 * After that the AbsoluteColorDifferenceDCTCoefficients is set to
	 * the result.
	 * 
	 * <strong>NOTE: </strong> the DCT matrices are always in the sizes
	 * 2x2, 4x4 and 8x8! If the size of the difference is bigger than that
	 * the difference is split into equally sized 8x8 blocks. Those are the
	 * order of the returned ArrayList.
	 * ArrayList order: Left-to-Rigth & Top-to-Bottom
	 * Double Order: [0] = Y-DCT; [1] = U-DCT; [2] = V-DCT
	 * 
	 * @param ArrayList<double[][][]> diffs => The prepared list to set
	 */
	public void setAbsoluteDifferences(final double[][][] YUVDifference) {
		this.AbsoluteColorDifferenceDCTCoefficients = DCT_ENGINE.computeDCTOfVectorColorDifference(YUVDifference, this.size);
		this.invokedDCTOfDifferences = true;
	}
	
	public ArrayList<double[][][]> getDCTCoefficientsOfAbsoluteColorDifference() {
		return this.AbsoluteColorDifferenceDCTCoefficients;
	}
	
	/**
	 * This function uses the invoked DCT coefficients
	 * of the absolute color difference to reconstruct the absolute
	 * color difference by using the IDCT (reversed DCT-II).
	 * 
	 * @return double[][][] => Reconstructed YUV color difference
	 * 
	 * @throws NullPointerException, if no DCT-Coefficients were invoked
	 */
	public double[][][] getIDCTCoefficientsOfAbsoluteColorDifference() {
		if (this.invokedDCTOfDifferences == false) throw new NullPointerException("No absolute difference were invoked, NULL DCT-Coefficients to process");
		return DCT_ENGINE.computeIDCTOfVectorColorDifference(cloneAbsoluteColorDifference(), this.size);
	}
	
	/**
	 * Clones the absoluteColorDifferenceDCTCoefficients ArrayList.
	 * This function should be used for getting the IDCT values, since the
	 * original ArrayList is referenced and might get quantizized by mistake
	 * if not cloned.
	 * @return ArrayList<double[][][]> => Cloned ArrayList.
	 */
	private ArrayList<double[][][]> cloneAbsoluteColorDifference() {
		ArrayList<double[][][]> copy = new ArrayList<double[][][]>(this.AbsoluteColorDifferenceDCTCoefficients.size());
		
		for (int i = 0; i < this.AbsoluteColorDifferenceDCTCoefficients.size(); i++) {
			double[][][] ref = this.AbsoluteColorDifferenceDCTCoefficients.get(i);
			int size = this.size, halfSize = this.size / 2;
			double[][][] clone = new double[3][][];
			clone[0] = new double[size][size];
			clone[1] = new double[halfSize][halfSize];
			clone[2] = new double[halfSize][halfSize];
			
			for (int n = 0; n < ref.length; n++) {
				for (int j = 0; j < ref[n].length; j++) {
					clone[n][j] = ref[n][j].clone();
				}
			}
			
			copy.add(clone);
		}
		
		return copy;
	}
	
	/**
	 * Sets the mostEqual MacroBlock to the provided
	 * MacroBlock. The mostEqual MacroBlock describes the best
	 * match in the inter-prediction step.
	 * 
	 * This function is DEBUG ONLY and provides a good way for
	 * debugging inter-prediction.
	 * 
	 * @param MacroBlock block => Block to set as mostEqualBlock
	 */
	public void setMostEqualBlock(final MacroBlock block) {
		this.mostEqualBlock = block;
	}
	
	public MacroBlock getMostEqualBlock() {
		return this.mostEqualBlock;
	}
}
