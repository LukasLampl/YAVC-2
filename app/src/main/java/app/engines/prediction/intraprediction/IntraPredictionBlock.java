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

package app.engines.prediction.intraprediction;

import app.Main;
import app.engines.dct.DCTEngine;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.components.Component2D;
import app.utils.components.MacroBlock;

/**
 * The {@code IntraPredictionBlock} is a representative of the {@link app.utils.components.MacroBlock MacroBlock}
 * and holds data for reconstructing a intra predicted {@code MacroBlock}.
 * 
 * @author Hans Lampl
 * @version 1.1.0
 */
public class IntraPredictionBlock extends Component2D {
	/**
	 * The DCT-Engine of the app.
	 */
	private static DCTEngine DCT_ENGINE = Main.DCT_ENGINE;
	
	/**
	 * Holds the appended MacroBlock, from which the {@code IntraPredictionBlock} was predicted.
	 */
	private MacroBlock appendedBlock = null;
	
	/**
	 * The prediction angle used.
	 */
	private int angle = 0;
	
	/**
	 * Holds all horizontal border pixels used for intra prediction.
	 */
	private double horizontal[][] = null;
	
	/**
	 * Holds all vertical border pixels used for intra prediction.
	 */
	private double vertical[][] = null;
	
	/**
	 * Holds all deltas in form of DCT coefficients.
	 */
	private double dctDelta[][][] = null;
	
	/**
	 * Holds all deltas in form of YUV colors.
	 */
	private double yuvDelta[][][] = null;

	/**
	 * Creates a new {@code IntraPredictionBlock} at the given position, angle and size.
	 * 
	 * @param x			Position in the x direction.
	 * @param x			Position in the y direction.
	 * @param angle		Angle used for prediction.
	 * @param size		Size of the block.
	 */
	public IntraPredictionBlock(final int x, final int y, final int angle,
			final int size) {
		super(x, y, size);
		
		this.angle = angle;
	}
	
	/**
	 * Gets the angle that was used during intra prediction.
	 * 
	 * @return The used angle in degrees.
	 */
	public int getAngle() {
		return this.angle;
	}
	
	public void setAngle(final int angle) {
		this.angle = angle;
	}
	
	/**
	 * Gets the horizontal border pixels that were used for intra prediction.
	 * 
	 * @return The horizontal pixels.
	 */
	public double[][] getHorizontal() {
		return this.horizontal;
	}
	
	public void setHorizontal(final double[][] horizontal) {
		this.horizontal = horizontal;
	}
	
	/**
	 * Gets the vertical border pixels that were used for intra prediction.
	 * 
	 * @return The vertical pixels.
	 */
	public double[][] getVertical() {
		return this.vertical;
	}
	
	public void setVertical(final double[][] vertical) {
		this.vertical = vertical;
	}
	
	/**
	 * Gets the IDCT coefficients (YUV deltas) of the color deltas.
	 * 
	 * @param allowModificationToOriginalData	Whether modifications to the data can be made.
	 * @return The converted deltas in YUV format.
	 */
	public double[][][] getIDCTCoefficientsDelta(boolean allowModificationToOriginalData) {
		if (allowModificationToOriginalData) {
			return DCT_ENGINE.computeIDCTOfVectorColorDifference(this.dctDelta, this.size, true);
		}
		
		double[][][] clone = cloneDCTDeltaValues();
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
	private double[][][] cloneDCTDeltaValues() {
		double[][][] ref = this.dctDelta;
		double[][][] clone = ArrayUtils.get3DArray(this.size, true);
		
		for (int i = 0; i < ColorManager.CHANNELS; i++) {
			ArrayUtils.copy2DArray(ref[i], 0, 0, clone[i], 0, 0, this.size, this.size);
		}
		
		return clone;
	}
	
	/**
	 * Get the raw DCT coefficients of the delta values.
	 * 
	 * @return The raw DCT coefficients of the delta values.
	 */
	public double[][][] getDCTCoefficientsOfDelta() {
		return this.dctDelta;
	}
	
	/**
	 * Sets the YUV deltas and converts the directly into DCT representation.
	 * 
	 * @param YUVDifference	The deltas of the {@code IntraPredictionBlock}.
	 */
	public void setDelta(double[][][] YUVDifference) {
		this.dctDelta = DCT_ENGINE.computeDCTOfVectorColorDifference(YUVDifference, this.size, true);
	}
	
	/**
	 * Sets DCT coefficient deltas to the {@code IntraPredictionBlock}.
	 * 
	 * @param YUVCoefficients	The DCT coefficients to set.
	 */
	public void setDeltaCoefficients(double[][][] YUVCoefficients) {
		this.dctDelta = YUVCoefficients;
	}
	
	/**
	 * Sets the delta values of the {@code IntraPredictionBlock} with YUV values.
	 * 
	 * @param YUVDelta	The YUV delta values.
	 */
	public void setYUVDelta(double[][][] YUVDelta) {
		this.yuvDelta = YUVDelta;
	}
	
	/**
	 * Sets the appended block used in intra prediction as a reference.
	 * 
	 * @param block	The used reference {@code MacroBlock}.
	 */
	public void setAppendedBlock(MacroBlock block) {
		this.appendedBlock = block;
	}
	
	/**
	 * Gets the appended {@code MacroBlock} that was used as reference
	 * for the intra predicted block.
	 * 
	 * @return The appended {@code MacroBlock}.
	 */
	public MacroBlock getAppendedBlock() {
		return this.appendedBlock;
	}
	
	@Override
	public void discard() {
		super.discard();
		this.angle = 0;
		this.horizontal = null;
		this.vertical = null;
		this.dctDelta = null;
	}
}
