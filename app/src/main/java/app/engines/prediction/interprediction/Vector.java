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

import app.utils.components.Component2D;

/**
 * <p>The class {@code Vector} is a container structure for storing
 * spatial data.</p>
 * 
 * <p>It is responsible for further processing and usually contains the following
 * information:</p>
 * <ul><li><b>Position</b>: Start position of the vector
 * <li><b>Span</b>: How long a direction of the vector is
 * <li><b>Size</b>: The size of the reference
 * <li><b>Reference</b>: Which frame was used to reference the vector
 * <li><b>Difference</b>: An array of color differences to preserve quality
 * </ul>
 * 
 * @author Lukas Lampl
 * @since 1.1.1
 */
public abstract class Vector extends Component2D {
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
	 * Resets the data inside the vector to the standard values, in order
	 * to reuse the vector.
	 */
	public void reset() {
		this.spanX = 0;
		this.spanY = 0;
		this.reference = 0;
	}
	
	@Override
	public void discard() {
		super.discard();
		reset();
	}
	
	@Override
	public int hashCode() {
		int res = 0;
		res |= ((this.positionX & 0xFFFF) ^ (this.positionY & 0xFFFF)) << 16;
		res |= (this.reference & 0xFF) ^ (this.spanX & 0xFF) ^ (this.spanY & 0xFF) << 8;
		res |= (this.size & 0xFF);
		return res;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()
				+ "[Position: x=" + this.positionX + ", y=" + this.positionY + "; "
				+ "Size: " + this.size + "; "
				+ "Span: x=" + this.spanX + ", y=" + this.spanY + "; "
				+ "Reference: " + this.reference
				+ "]";
	}
}
