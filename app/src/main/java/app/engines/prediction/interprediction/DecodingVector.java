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

/**
 * <p>The class {@code DecodingVector} is a container structure for storing
 * spatial data, received from a coded YAVC file.</p>
 * 
 * <p><b>Important:</b><br>
 * This should exclusively be used for decoding processes only.
 * </p>
 * 
 * @author Lukas Lampl
 * @since 1.0.0 [optimized_prototype_2]
 * 
 * @see app.engines.prediction.interprediction.Vector Vector
 */
public class DecodingVector extends Vector {
	/**
	 * Holds the YUV delta values in form of DCT coefficients.
	 */
	private double[][][] YUVDelta = null;
	
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
	public DecodingVector(final int x, final int y, final int size) {
		super(x, y, size);
	}
	
	/**
	 * Sets the YUV delta of the vector (actual values are awaited, not DCT coeffs).
	 * 
	 * @param YUVDelta	The delta values.
	 */
	public void setYUVDelta(final double[][][] YUVDelta) {
		this.YUVDelta = YUVDelta;
	}
	
	/**
	 * Returns the delta YUV.
	 * 
	 * @return The YUV deltas.
	 */
	public double[][][] getYUVDeltas() {
		return this.YUVDelta;
	}

	/**
	 * Resets the data inside the vector to the standard values, in order
	 * to reuse the vector.
	 */
	public void reset() {
		this.YUVDelta = null;
	}
	
	@Override
	public void discard() {
		super.discard();
		reset();
	}
}
