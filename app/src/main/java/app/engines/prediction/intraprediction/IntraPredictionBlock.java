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
	 * Holds the delta values of the IntraPredictionBlock.
	 * This can be used by the encoder as well as the decoder.
	 * Both will holds the data in different forms, eg. the encoder in DCT coeffs,
	 * while the decoder in actual YUV values.
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
	 * @return The converted deltas in YUV format.
	 */
	public double[][][] getIDCTYUVDelta() {
		return DCT_ENGINE.computeIDCTOfDeltas(this.yuvDelta, this.size, true);
	}
	
	/**
	 * Gets the YUV delta that was applied to the block.
	 * 
	 * @return The delta values.
	 */
	public double[][][] getYUVDelta() {
		return this.yuvDelta;
	}
	
	/**
	 * Sets the delta values of the {@code IntraPredictionBlock} with YUV values.
	 * If the block is a coding unit (encoding process) the deltas will be converted
	 * to DCT coeffs.
	 * 
	 * @param YUVDelta	The YUV delta values.
	 * @param encoding	Whether the block is a coding unit or not. (Encoding process)
	 */
	public void setYUVDelta(double[][][] YUVDelta, boolean encoding) {
		if (encoding) {
			this.yuvDelta = DCT_ENGINE.computeDCTOfDeltas(YUVDelta, this.size, true);
		} else {
			this.yuvDelta = YUVDelta;
		}
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
		this.yuvDelta = null;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()
				+ "[Position: x=" + this.positionX + ", y=" + this.positionY + "; "
				+ "Size: " + this.size + "; "
				+ "Angle: " + this.angle
				+ "]";
	}
}
