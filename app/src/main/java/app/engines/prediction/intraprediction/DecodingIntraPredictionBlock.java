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


/**
 * The {@code DecodingIntraPredictionBlock} is a representative of the {@link app.engines.prediction.intraprediction.IntraPredictionBlock
 * IntraPredictionBlock} and extends it by the YUV deltas needed for reconstructing
 * coding errors made by the intra prediction.
 * 
 * <p><b>Important:</b><br>
 * The should exclusively be used for decoding processes.
 * </p>
 * 
 * @author Hans Lampl
 * @version 1.0.0 [optimized_prototype_2]
 * 
 * @see app.engines.prediction.intraprediction.IntraPredictionBlock IntraPredictionBlock
 */
public class DecodingIntraPredictionBlock extends IntraPredictionBlock {
	/**
	 * Holds the YUV delta coefficients.
	 */
	private double yuvDelta[][][] = null;
	
	/**
	 * Creates a new {@code DecodingIntraPredictionBlock} at the given position, angle and size.
	 * 
	 * @param x			Position in the x direction.
	 * @param x			Position in the y direction.
	 * @param angle		Angle used for prediction.
	 * @param size		Size of the block.
	 */
	public DecodingIntraPredictionBlock(final int x, final int y, final int angle, final int size) {
		super(x, y, angle, size);
	}

	/**
	 * Gets the YUV delta that was applied to the block.
	 * 
	 * @return The delta values.
	 */
	public double[][][] getYUVDeltas() {
		return this.yuvDelta;
	}
	
	/**
	 * Sets the delta values of the {@code IntraPredictionBlock} with YUV values.
	 * The deltas are awaited as being in YUV format not DCT coeffs.
	 * 
	 * @param YUVDelta	The YUV delta values.
	 */
	public void setYUVDelta(final double[][][] YUVDelta) {
		this.yuvDelta = YUVDelta;
	}
	
	@Override
	public void discard() {
		super.discard();
		this.yuvDelta = null;
	}
}
