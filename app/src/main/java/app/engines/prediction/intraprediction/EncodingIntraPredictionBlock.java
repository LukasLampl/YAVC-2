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
import app.utils.components.MacroBlock;

/**
 * The {@code EncodingIntraPredictionBlock} is a representative of the {@link app.engines.prediction.intraprediction.IntraPredictionBlock
 * IntraPredictionBlock} and extends it by DCT conversion of the deltas.
 * 
 * <p><b>Important:</b><br>
 * The should exclusively be used for encoding processes.
 * </p>
 * 
 * @author Hans Lampl
 * @version 1.0.0 [optimized_prototype_2]
 * 
 * @see app.engines.prediction.intraprediction.IntraPredictionBlock IntraPredictionBlock
 */
public class EncodingIntraPredictionBlock extends IntraPredictionBlock {
	/**
	 * The DCT-Engine of the app.
	 */
	private static DCTEngine DCT_ENGINE = Main.DCT_ENGINE;
	
	/**
	 * Holds the appended MacroBlock, from which the {@code IntraPredictionBlock} was predicted.
	 */
	private MacroBlock appendedBlock = null;
	
	/**
	 * Holds the DCT YUV delta coefficients.
	 */
	private double dctYUVDelta[][][] = null;
	
	/**
	 * Creates a new {@code EncodingIntraPredictionBlock} at the given position, angle and size.
	 * 
	 * @param x			Position in the x direction.
	 * @param x			Position in the y direction.
	 * @param angle		Angle used for prediction.
	 * @param size		Size of the block.
	 */
	public EncodingIntraPredictionBlock(final int x, final int y, final int angle, final int size) {
		super(x, y, angle, size);
	}

	/**
	 * Returns the IDCT (original YUV).
	 * 
	 * @return The converted YUV.
	 */
	public double[][][] getIDCTOfDeltas() {
		return DCT_ENGINE.computeIDCTOfDeltas(this.dctYUVDelta, this.size, true, false);
	}
	
	/**
	 * Gets the YUV delta that was applied to the block.
	 * IDCT will be performed.
	 * 
	 * @return The delta values.
	 */
	public double[][][] getYUVDeltas() {
		return this.dctYUVDelta;
	}
	
	/**
	 * Sets the delta values of the {@code IntraPredictionBlock} with YUV values.
	 * The deltas will be converted to DCT coefficients.
	 * 
	 * @param YUVDelta	The YUV delta values.
	 */
	public void setYUVDelta(final double[][][] YUVDelta) {
		this.dctYUVDelta = DCT_ENGINE.computeDCTOfDeltas(YUVDelta, this.size, true, true);
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
		this.appendedBlock = null;
		this.dctYUVDelta = null;
	}
}
