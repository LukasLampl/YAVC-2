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

package interprediction;

import java.awt.Point;
import java.util.ArrayList;

import encoder.DCTEngine;
import utils.MacroBlock;

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
 * @since 17.0
 * @version 1.0 29 May 2024
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
	 * The reference frame, from which the block is referred to
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
	 * <p>Initializes the vector for further processing</p>
	 * 
	 * @param pos	starting position of the vector
	 * @param size	size of the reference MacroBlock
	 * 
	 * @throws NullPointerException	when the position is null
	 * @throws IllegalArgumentException	if the area of the reference is 0 or negative
	 */
	public Vector(final Point pos, final int size) {
		if (pos == null) {
			throw new NullPointerException("Vector can't have NULL point as startingPoint");
		} else if (size <= 0) {
			throw new IllegalArgumentException("Vector can't have a 0 or negative area of reference");
		}
		
		this.startingPoint = pos;
		this.size = size;
		this.DCT_ENGINE = app.Main.DCT_ENGINE;
	}
	
	/**
	 * <p>This function is never used in the actual code, but provides
	 * a good debugging option</p>
	 * <p>Sets the appended block "block to be searched"</p>
	 *
	 * @param block	The appended MacroBlock of the vector
	 */
	public void setAppendedBlock(final MacroBlock block) {
		this.appendedBlock = block;
	}
	
	/**
	 * <p>This function is never used in the actual code, but provides
	 * a good debugging option</p>
	 * <p>Returns the appended block</p>
	 * 
	 * @return MacroBlock that was previously appended to the vector
	 */
	public MacroBlock getAppendedBlock() {
		return this.appendedBlock;
	}
	
	/**
	 * <p>The function sets the spanX of the vector to a given integer.
	 * The span starts from the origin (startPoint).</p>
	 * 
	 * <p><strong>Warning:</strong><br> The set span might exceed the encoding
	 * maximum of 8 bits = 255. The offset is excluded
	 * ({@link app.Protocol#CODING_OFFSET}).</p>
	 * 
	 * @param span	Span to the x direction
	 */
	public void setSpanX(final int span) {
		this.spanX = span;
	}
	
	/**
	 * <p>The function sets the spanY of the vector to a given integer.
	 * The span starts from the origin (startPoint).</p>
	 * 
	 * <p><strong>Warning:</strong><br> The set span might exceed the encoding
	 * maximum of 8 bits = 255. The offset is excluded
	 * ({@link app.Protocol#CODING_OFFSET}).</p>
	 * 
	 * @param span	Span to the y direction
	 */
	public void setSpanY(final int span) {
		this.spanY = span;
	}
	
	/**
	 * <p>Sets the reference frame of the mostEqualBlock, meaning
	 * that this number represents, out of which frame the mostEqualBlock
	 * was extracted from.</p>
	 * 
	 * <p><strong>Warning:</strong><br> The max reference is set by
	 * {@link app.Protocol#MAX_REFERENCES}.<br>
	 * <u>The encoding only supports till 4 references into the past!</u></p>
	 * 
	 * @param reference	reference frame number
	 */
	public void setReference(final int reference) {
		this.reference = reference;
	}
	
	/**
	 * <p>Sets the size of the mostEqualBlock as well as
	 * the size of the appendedBlock.</p>
	 * 
	 * @param size	Size of the appendedBlock
	 */
	public void setSize(final int size) {
		this.size = size;
	}
	
	/**
	 * <p>Get the position of the vector.</p>
	 * 
	 * @return Position of the vector.
	 */
	public Point getPosition() {
		return this.startingPoint;
	}
	
	/**
	 * <p>Get the x span of the vector.</p>
	 * 
	 * @return Span x of the vector.
	 */
	public int getSpanX() {
		return this.spanX;
	}
	
	/**
	 * <p>Get the y span of the vector.</p>
	 * 
	 * @return Span y of the vector.
	 */
	public int getSpanY() {
		return this.spanY;
	}
	
	/**
	 * <p>Get the size of the vector reference.</p>
	 * 
	 * @return Size of the vector reference.
	 */
	public int getSize() {
		return this.size;
	}
	
	/**
	 * <p>Get the reference frame number of the vectors reference.</p>
	 * 
	 * @return Reference number
	 */
	public int getReference() {
		return this.reference;
	}
	
	/**
	 * <p>Sets the AbsoluteColorDifferenceDCTCoefficients to the
	 * prepared list, that is provided by the parameters.</p>
	 * <p>The order is as followed:
	 * <ul><li>ArrayList order: Left-to-Right & Top-to-Bottom
	 * <li>Double order: [0] = Y-DCT; [1] = U-DCT; [2] = V-DCT
	 * </ul></p>
	 * 
	 * @param diffs	The prepared list to set
	 */
	public void setAbsolutedifferenceDCTCoefficients(final ArrayList<double[][][]> diffs) {
		if (diffs == null) {
			throw new NullPointerException("Can't use NULL as difference");
		}
		
		this.AbsoluteColorDifferenceDCTCoefficients = diffs;
		this.invokedDCTOfDifferences = true;
	}
	
	/**
	 * <p>Sets the AbsoluteColorDifferenceDCTCoefficients.
	 * First the provided YUVDifference, with the order
	 * [0] = Y; [1] = U; [2] = V is passed to the DCTEngine, which
	 * calculates the DCT-Coefficients for each part individually.
	 * After that the AbsoluteColorDifferenceDCTCoefficients is set to
	 * the result.</p>
	 * 
	 * <p><strong>NOTE: </strong><br> The DCT matrices are always in the sizes
	 * 2x2, 4x4 and 8x8! If the size of the difference is bigger than that
	 * the difference is split into equally sized 8x8 blocks. Those are the
	 * order of the returned ArrayList.
	 * <ul><li>ArrayList order: Left-to-Rigth & Top-to-Bottom
	 * <li>Double order: [0] = Y-DCT; [1] = U-DCT; [2] = V-DCT
	 * </ul></p>
	 * 
	 * @param diffs	The prepared list to set
	 */
	public void setAbsoluteDifferences(final double[][][] YUVDifference) {
		this.AbsoluteColorDifferenceDCTCoefficients = DCT_ENGINE.computeDCTOfVectorColorDifference(YUVDifference, this.size);
		this.invokedDCTOfDifferences = true;
	}
	
	/**
	 * <p>Get the ArrayList with all DCT coefficients of color differences.</p>
	 * 
	 * @return ArrayList with the DCT coefficients of the color difference
	 */
	public ArrayList<double[][][]> getDCTCoefficientsOfAbsoluteColorDifference() {
		return this.AbsoluteColorDifferenceDCTCoefficients;
	}
	
	/**
	 * <p>This function uses the invoked DCT coefficients
	 * of the absolute color difference to reconstruct the absolute
	 * color difference by using the IDCT.</p>
	 * 
	 * @param allowModificationToOriginalData	Flag for whether the
	 * original data will be copied before processing or not.
	 * 
	 * @return Reconstructed YUV color difference
	 * 
	 * @throws NullPointerException	if no DCT-Coefficients were invoked
	 */
	public double[][][] getIDCTCoefficientsOfAbsoluteColorDifference(boolean allowModificationToOriginalData) {
		if (this.invokedDCTOfDifferences == false) {
			throw new NullPointerException("No absolute difference were invoked, NULL DCT-Coefficients to process");
		}
		
		if (allowModificationToOriginalData) {
			return DCT_ENGINE.computeIDCTOfVectorColorDifference(this.AbsoluteColorDifferenceDCTCoefficients, this.size);
		}
		
		return DCT_ENGINE.computeIDCTOfVectorColorDifference(cloneAbsoluteColorDifference(), this.size);
	}
	
	/**
	 * <p>Clones the absoluteColorDifferenceDCTCoefficients ArrayList.
	 * This function should be used for getting the IDCT values, since the
	 * original ArrayList is referenced and might get quantified by mistake
	 * if not cloned.</p>
	 * 
	 * @return Cloned ArrayList with all data.
	 */
	private ArrayList<double[][][]> cloneAbsoluteColorDifference() {
		int len = this.AbsoluteColorDifferenceDCTCoefficients.size();
		ArrayList<double[][][]> copy = new ArrayList<double[][][]>(len);
		int size = this.size;
		int halfSize = this.size / 2;
		
		for (int i = 0; i < len; i++) {
			double[][][] ref = this.AbsoluteColorDifferenceDCTCoefficients.get(i);
			double[][][] clone = new double[3][][];
			clone[0] = new double[size][size];
			clone[1] = new double[halfSize][halfSize];
			clone[2] = new double[halfSize][halfSize];
			
			for (int n = 0; n < ref.length; n++) {
				for (int j = 0; j < ref[n].length; j++) {
					System.arraycopy(ref[n][j], 0, clone[n][j], 0, ref[n][j].length);
				}
			}
			
			copy.add(clone);
		}
		
		return copy;
	}
	
	/**
	 * <p>Sets the mostEqual MacroBlock to the provided
	 * MacroBlock. The mostEqual MacroBlock describes the best
	 * match in the inter-prediction step.</p>
	 * 
	 * <p><u>This function is DEBUG ONLY and provides a good way for
	 * debugging inter-prediction.</u></p>
	 * 
	 * @param block	Block to set as mostEqualBlock
	 */
	public void setMostEqualBlock(final MacroBlock block) {
		this.mostEqualBlock = block;
	}
	
	/**
	 * <p>Get the mostEqualBlock MacroBloc.</p>
	 * 
	 * @return The set mostEqualBlock
	 */
	public MacroBlock getMostEqualBlock() {
		return this.mostEqualBlock;
	}
}
