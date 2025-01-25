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

package app.utils;

import java.awt.Dimension;
import java.awt.Point;

import app.rendering.ColorManager;

/**
 * 
 * 
 * @author Lukas Lampl
 * @since 17.0
 * @version 1.0 29 May 2024
 */

public class WeakMacroBlock implements Discardable {
	private PixelRaster canvas = null;
	
	/**
	 * <p>Position of the MacroBlock, originated from the PixelRaster.</p>
	 */
	private int positionX = 0;
	private int positionY = 0;
	
	private int positionRelativeToParentX = 0;
	private int positionRelativeToParentY = 0;
	
	/**
	 * <p>Size of the MacroBlock.</p>
	 */
	private int size = 0;
	
	/**
	 * <p>Squared size of the MacroBlock.</p>
	 */
	private int squared_size = 0;
	
	/**
	 * <p>Sets a flag, whether it is subdivided into 4 more blocks or not.</p>
	 * <ul><li><b>true</b> = Is subdivided
	 * <li><b>false</b> = Is not subdivided
	 * </ul>
	 */
	private boolean isSubdivided = false;
	
	private boolean isConvertedToVector = false;
	
	private boolean isBlockReferencing = false;
	
	/**
	 * <p>Nodes of the MacroBlock.</p>
	 * <p>Only filled, if the MacroBlock was
	 * subdivided.</p>
	 */
	private WeakMacroBlock[] nodes = null;
	
	/**
	 * <p>Mean color based on the subdivision of
	 * the MacroBlock.</p>
	 */
	private double[] meanColor = ColorManager.NULL_COLOR;
	
	/**
	 * <p>Defines the total MSE (= Mean Square Error) from the
	 * inter-prediction part.</p>
	 */
	private double MSE = Double.MAX_VALUE;
	
	/**
	 * <p>Defines the reference frame from the inter-prediction
	 * part.</p>
	 */
	private int reference = 0;

	public WeakMacroBlock(final int x, final int y, final int size) {
		if (size < 0 || size > 65535) {
			throw new IllegalArgumentException("The size of " + size + " is not supported!");
		}
		
		this.positionX = x;
		this.positionY = y;
		this.size = size;
		this.squared_size = size * size;
	}
	

	public WeakMacroBlock(final int x, final int y, final int size, PixelRaster canvas) {
		if (size < 0 || size > 65535) {
			throw new IllegalArgumentException("The size of " + size + " is not supported!");
		}
		
		this.positionX = x;
		this.positionY = y;
		this.size = size;
		this.squared_size = size * size;
		this.canvas = canvas;
		this.isBlockReferencing = true;
	}

	public void setCanvas(final PixelRaster canvas) {
		this.canvas = canvas;
		this.isBlockReferencing = true;
	}

	
	/**
	 * <p>Returns the YUV color at the specified position x, y.<br>
	 * <b>Important:</b> The position is relative to the MacroBlock!</p>
	 * 
	 * @return Double array with Y at [0], U at [1] and V at [2].
	 * 
	 * @param x	Position X in the MacroBlock itself.
	 * @param y Position Y in the MacroBlock itself.
	 * 
	 * @throws ArrayIndexOutOfBoundsException	if the x or y coordinate
	 * is out of bounds within the MacroBlock
	 * @throws NullPointerException	if the following situations:
	 * <ul><li>If the Y component is null
	 * <li>If the U component is null
	 * <li>If the V component is null
	 * </ul>
	 */
	public double[] getYUV(final int x, final int y) {
		if (x < 0 || x >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(X) " + x + " is out of bounds (" + this.size + ")");
		} else if (y < 0 || y >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(Y) " + y + " is out of bounds (" + this.size + ")");
		} else if (!this.isBlockReferencing) {
			throw new NullPointerException("No canvas from which to get the colors from.");
		}

		return this.canvas.getYUV(x + this.positionX, y + this.positionY);
	}
	
	/**
	 * <p>Returns the YUV color at the specified position x, y.<br>
	 * <b>Important:</b> The position is relative to the MacroBlock!</p>
	 * 
	 * @return Double array with Y at [0], U at [1] and V at [2].
	 * 
	 * @param x	Position X in the MacroBlock itself.
	 * @param y Position Y in the MacroBlock itself.
	 * 
	 * @throws ArrayIndexOutOfBoundsException	if the x or y coordinate
	 * is out of bounds within the MacroBlock
	 * @throws NullPointerException	if the following situations:
	 * <ul><li>If the Y component is null
	 * <li>If the U component is null
	 * <li>If the V component is null
	 * </ul>
	 */
	public double[] getYUV(final int x, final int y, double[] cache) {
		if (x < 0 || x >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(X) " + x + " is out of bounds (" + this.size + ")");
		} else if (y < 0 || y >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(Y) " + y + " is out of bounds (" + this.size + ")");
		} else if (!this.isBlockReferencing) {
			throw new IllegalStateException("No canvas from which to get the colors from.");
		}
		
		double[] YUV = this.canvas.getYUV(x + this.positionX, y + this.positionY);
		cache[ColorManager.Y_INDEX] = YUV[ColorManager.Y_INDEX];
		cache[ColorManager.U_INDEX] = YUV[ColorManager.U_INDEX];
		cache[ColorManager.V_INDEX] = YUV[ColorManager.V_INDEX];
		return cache;
	}
	
	/**
	 * <p>Sets the provided YUV color at a specific
	 * position.</p>
	 * 
	 * @param x	position X of the color
	 * @param y	position Y of the color
	 * @param YUV	YUV color to set
	 */
	public void setYUV(final int x, final int y, final double[] YUV) {
		if (x < 0 || x >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(X) " + x + " is out of bounds (" + this.size + ")");
		} else if (y < 0 || y >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(Y) " + y + " is out of bounds (" + this.size + ")");
		} else if (!this.isBlockReferencing) {
			throw new IllegalStateException("No canvas from which to get the colors from.");
		}
		
		this.canvas.setYUV(x + this.positionX, y + this.positionY, YUV);
	}
	
	public Point getPositionRelativeToParent() {
		return new Point(this.positionRelativeToParentX, this.positionRelativeToParentY);
	}
	
	public int getPositionRelativeToParentX() {
		return this.positionRelativeToParentX;
	}
	
	public int getPositionRelativeToParentY() {
		return this.positionRelativeToParentY;
	}
	
	private void setPositionRelativeToParent(final int x, final int y) {
		this.positionRelativeToParentX = x;
		this.positionRelativeToParentY = y;
	}
	
	/**
	 * <p>Subdivides a MacroBlock into 4 equally sized subblocks.</p>
	 * 
	 * <p>The minimum size is 4. When a subdivided block is out of the
	 * PixelRaster, it is destroyed. If a subdivided MacroBlock is at the
	 * boundary, it is split, until it is fully inside.</p>
	 * 
	 * @param dim	Dimension of the PixelRaster
	 */
	public void subdivide(final Dimension dim) {
		if (this.isSubdivided == true) {
			return;
		} else if (this.size <= 4) {
			return;
		}
		
		this.isSubdivided = true;
		this.nodes = new WeakMacroBlock[4];
		int index = 0;
		final int fraction = this.size / 2;
		int outlyers = 0;
		
		for (int x = 0; x < this.size; x += fraction) {
			for (int y = 0; y < this.size; y += fraction) {
				if ((this.positionX + x >= dim.width
					|| this.positionX + x < 0)
					|| (this.positionY + y >= dim.height
					|| this.positionY + y < 0)) {
					if (outlyers++ >= 4) {
						this.isSubdivided = false;
					}
					
					continue;
				}
				
				WeakMacroBlock b = getSubBlock(new Point(x, y), fraction);
				b.setPositionRelativeToParent(x + this.positionRelativeToParentX, y + this.positionRelativeToParentY);
				this.nodes[index++] = b;
			}
		}
	}
	
	/**
	 * <p>Get the nodes of the current MacroBlock.</p>
	 * @return Nodes of the MacroBlock
	 */
	public WeakMacroBlock[] getNodes() {
		return this.nodes;
	}
	
	/**
	 * <p>Flag whether the MacroBlock is subdivided or not.</p>
	 * @return Flag whether the MacroBlock is subdivided or not
	 */
	public boolean isSubdivided() {
		return this.isSubdivided;
	}
	
	/**
	 * <p>Get the position of the MacroBlock.</p>
	 * @return Position of the MacroBlock
	 */
	public Point getPosition() {
		return new Point(this.positionX, this.positionY);
	}
	
	/**
	 * Get the x position of the MacroBlock.
	 * 
	 * @return The x position of the MacroBlock.
	 */
	public int getPositonX() {
		return this.positionX;
	}
	
	/**
	 * Get the y position of the MacroBlock.
	 * 
	 * @return The y position of the MacroBlock.
	 */
	public int getPositionY() {
		return this.positionY;
	}
	
	/**
	 * <p>Get the size of the MacroBlock.</p>
	 * @return Size of the MacroBlock
	 */
	public int getSize() {
		return this.size;
	}
	
	/**
	 * <p>Get the squared size of the MacroBlock.</p>
	 * @return Squared size of the MacroBlock
	 */
	public int getSquaredSize() {
		return this.squared_size;
	}
	
	/**
	 * <p>Get the mean color of the MacroBlock.</p>
	 * @return Mean color of the MacroBlock
	 */
	public double[] getMeanColor() {
		if (!this.isBlockReferencing) {
			throw new IllegalStateException("No canvas from which to get the colors from.");
		}
		
		return this.meanColor;
	}
	
	/**
	 * <p>Set the mean color of the MacroBlock.</p>
	 * 
	 * @param meanColor	mean color of the MacroBlock
	 */
	public void setMeanColor(final double[] meanColor) {
		this.meanColor = meanColor;
	}
	
	/**
	 * <p>Get the colors of the MacroBlock.</p>
	 * 
	 * <p><b>Important:</b><br>
	 * The array might be bigger than the actual data itself because of
	 * caching techniques, please refer to use {@link #getSize()} for the
	 * approximate size of the array!
	 * </p>
	 * @return Colors of the MacroBlock
	 */
	public double[][][] getColors() {
		if (!this.isBlockReferencing) {
			throw new IllegalStateException("No canvas from which to get the colors from.");
		}
		
		return this.canvas.getPixelBlock(this.positionX, this.positionY, this.size, null);
	}
	
	/**
	 * <p>Get the MSE of the MacroBlock.</p>
	 * @return MSE of the MacroBlock
	 */
	public double getMSE() {
		return this.MSE;
	}
	
	/**
	 * <p>Set the MSE of the MacroBlock.</p>
	 * 
	 * @param MSE	Mean Square Error of the MacroBlock
	 * received by the inter-prediction
	 */
	public void setMSE(final double MSE) {
		this.MSE = MSE;
	}
	
	/**
	 * <p>Set the reference of the MacroBlock.</p>
	 * 
	 * @param ref	Reference of the MacroBlock
	 * to the best matching block
	 */
	public void setReference(final int ref) {
		this.reference = ref;
	}
	
	/**
	 * <p>Get the reference of the MacroBlock.</p>
	 * @return reference of the MacroBlock
	 */
	public int getReference() {
		return this.reference;
	}

	/**
	 * <p>Get a smaller sub-block off of the current MacroBlock with
	 * the specified size.</p>
	 * 
	 * @return Sub-block from the MacroBlock
	 * 
	 * @param pos	Position of the sub-block within the MacroBlock
	 * @param size	Size of the sub-block
	 * 
	 * @throws ArrayIndexOutOfBoundsException	If x or y is below 0 or bigger
	 * than the MacroBlock size
	 * @throws IllegalArgumentException	When the size is smaller than 1 or bigger
	 * than the MacroBlock itself
	 */
	private WeakMacroBlock getSubBlock(final Point pos, final int size) {
		final int posX = pos.x + this.positionX;
		final int posY = pos.y + this.positionY;
		return new WeakMacroBlock(posX, posY, size, this.canvas);
	}
	
	/**
	 * Sets the flag for whether this MacroBlock has been converted to a
	 * vector or not to the given boolean.
	 * 
	 * @param convertedToVector	Flag for whether the block has been converted or not.
	 */
	public void setConvertedToVector(final boolean convertedToVector) {
		this.isConvertedToVector = convertedToVector;
	}
	
	/**
	 * Returns whether the MacroBlock has been converted to a vector or not.
	 * 
	 * @return
	 * <ul>
	 * <li>{@code true} - If the MacroBlock has been converted
	 * <li>{@code false} - If the MacroBlock hasn't been converted
	 */
	public boolean isConvertedToVector() {
		return this.isConvertedToVector;
	}
	
	/**
	 * Repositions the MacroBlocks position.
	 * 
	 * @param x	X position of the MacroBlock.
	 * @param y	Y position of the MacroBlock.
	 */
	public void moveBlock(final int x, final int y) {
		this.positionX = x;
		this.positionY = y;
	}
	
	public void reset(final int x, final int y, final int size) {
		this.positionX = x;
		this.positionY = y;
		this.positionRelativeToParentX = 0;
		this.positionRelativeToParentY = 0;
		this.size = size;
		
		this.nodes = null;
		this.isConvertedToVector = false;
		this.isSubdivided = false;
		this.meanColor = ColorManager.NULL_COLOR;
		this.MSE = 0;
		this.squared_size = size * size;
		this.reference = 0;
		this.isBlockReferencing = false;
		this.canvas = null;
	}
	
	@Override
	public void discard() {
		reset(0, 0, 0);
	}
}