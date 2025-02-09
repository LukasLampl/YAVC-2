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

import app.utils.components.Component2D;

/**
 * The {@code IntraPredictionBlock} is a representative of the {@link app.utils.components.MacroBlock MacroBlock}
 * and holds data for reconstructing a intra predicted {@code MacroBlock}.
 * 
 * @author Hans Lampl
 * @version 1.1.0
 */
public abstract class IntraPredictionBlock extends Component2D {
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
	
	/**
	 * Sets the angle of the intra prediction.
	 * 
	 * @param angle	The used angle for intra prediction.
	 */
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
	
	/**
	 * Sets the horizontal border pixels used for extrapolating.
	 * 
	 * @param horizontal	The horizontal border pixels.
	 */
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
	
	/**
	 * Sets the vertical border pixels used for extrapolating.
	 * 
	 * @param vertical	The vertical border pixels.
	 */
	public void setVertical(final double[][] vertical) {
		this.vertical = vertical;
	}
	
	@Override
	public void discard() {
		super.discard();
		this.angle = 0;
		this.horizontal = null;
		this.vertical = null;
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
