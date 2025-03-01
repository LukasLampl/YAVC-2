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

package app.utils.components;

import java.awt.Dimension;
import java.awt.Point;

import app.rendering.ColorManager;
import app.utils.ArrayUtils;

/**
 * <p>The class {@code MacroBlock} is the main transform unit
 * in the YAVC video compressor. It is either a 128x128, 64x64, 32x32,
 * 16x16, 8x8 or 4x4 part of a frame and is used for all transformations.
 * </p>
 * 
 * @author Lukas Lampl
 * @since 1.0.0
 */

public class MacroBlock extends Component2D {
	/**
	 * <p>The Y values of the MacroBlock.</p>
	 */
	private double[][] Y = null;
	
	/**
	 * <p>The U values of the MacroBlock (subsampled).</p>
	 */
	private double[][] U = null;
	
	/**
	 * <p>The U values of the MacroBlock (subsampled).</p>
	 */
	private double[][] V = null;
	
	private int positionRelativeToParentX = 0;
	private int positionRelativeToParentY = 0;
	
	/**
	 * <p>Sets a flag, whether it is subdivided into 4 more blocks or not.</p>
	 * <ul><li><b>true</b> = Is subdivided
	 * <li><b>false</b> = Is not subdivided
	 * </ul>
	 */
	private boolean isSubdivided = false;
	
	/**
	 * Flag for whether the colors of the MacroBlock are set or not.
	 */
	private boolean isColorSet = false;
	
	/**
	 * <p>Nodes of the MacroBlock.</p>
	 * <p>Only filled, if the MacroBlock was
	 * subdivided.</p>
	 */
	private MacroBlock[] nodes = null;
	
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
	
	/**
	 * The linked component.
	 */
	private Component2D link = null;
	
	public MacroBlock(final MacroBlock block) {
		super(block.getPositionX(), block.getPositionY(), block.getSize());
		this.positionRelativeToParentX = block.getPositionRelativeToParentX();
		this.positionRelativeToParentY = block.getPositionRelativeToParentY();
		double[][][] col = block.getColors();
		this.Y = col[0];
		this.U = col[1];
		this.V = col[2];
		this.isColorSet = true;
	}
	
	/**
	 * <p>Creates an empty MacroBlock, with a position and size.</p>
	 * 
	 * @param x				Position x of the MacroBlock based on the PixelRaster.
	 * @param y				Position y of the MacroBlock based on the PixelRaster.
	 * @param size			Size of the MacroBlock.
	 * @param initColor		Flag for whether the Y, U and V components are initialized or not.
	 * 
	 * @throws NullPointerException	When the position is null
	 * @throws IllegalArgumentException	If the size is below 0
	 */
	public MacroBlock(final int x, final int y, final int size, final boolean initColor) {
		super(x, y, size);
		
		if (initColor) {
			final int halfSize = size >> 1;
			this.Y = new double[size][size];
			this.U = new double[halfSize][halfSize];
			this.V = new double[halfSize][halfSize];
		}
		
		this.isColorSet = initColor;
	}
	
	/**
	 * <p>Initializes a MacroBlock with Position, Size,
	 * Y, U and V.</p>
	 * 
	 * @param x		Position x of the MacroBlock based on the PixelRaster.
	 * @param y		Position y of the MacroBlock based on the PixelRaster.
	 * @param size	Size of the MacroBlock.
	 * @param Y		Y values in the MacroBlock.
	 * @param U		U values in the MacroBlock.
	 * @param V		V values in the MacroBlock.
	 * 
	 * @throws NullPointerException	in the following situations:
	 * <ul><li>If the provided position is null
	 * <li>If the Y component is null
	 * <li>If the U component is null
	 * <li>If the V component is null
	 * </ul>
	 * 
	 * @throws IllegalArgumentException	If the size is below 0
	 */
	public MacroBlock(final int x, final int y, final int size, final double[][] Y, final double[][] U, final double[][] V) {
		super(x, y, size);
		
		if (Y == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		} else if (U == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		} else if (V == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		}

		this.Y = Y;
		this.U = U;
		this.V = V;
		this.isColorSet = true;
	}
	
	/**
	 * <p>Initializes a MacroBlock with Position, Size,
	 * Y, U and V. The color components should have the following order:
	 * <ul><li>[0] = Y
	 * <li> [1] = U
	 * <li> [2] = V
	 * </ul>
	 * 
	 * @param x		Position x of the MacroBlock based on the PixelRaster.
	 * @param y		Position y of the MacroBlock based on the PixelRaster.
	 * @param size	Size of the MacroBlock.
	 * @param Y		Y values in the MacroBlock.
	 * @param U		U values in the MacroBlock.
	 * @param V		V values in the MacroBlock.
	 * 
	 * @throws NullPointerException	if the following situations:
	 * <ul><li>If the provided position is null
	 * <li>If the Y component is null
	 * <li>If the U component is null
	 * <li>If the V component is null
	 * </ul>
	 * 
	 * @throws IllegalArgumentException	If the size is below 0
	 */
	public MacroBlock(final int x, final int y, final int size, final double[][][] colors) {
		super(x, y, size);
		
		if (colors[ColorManager.Y_INDEX] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		} else if (colors[ColorManager.U_INDEX] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		} else if (colors[ColorManager.V_INDEX] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		}

		this.Y = colors[ColorManager.Y_INDEX];
		this.U = colors[ColorManager.U_INDEX];
		this.V = colors[ColorManager.V_INDEX];
		this.isColorSet = true;
	}
	
	/**
	 * <p>Initializes the color components of the MacroBlock individually.</p>
	 * 
	 * @param colors	Color components of the MacroBlock.
	 * 
	 * @throws NullPointerException	if the following situations:
	 * <ul><li>If the Y component is null
	 * <li>If the U component is null
	 * <li>If the V component is null
	 * </ul>
	 */
	public void setColorComponents(final double[][][] colors) {
		if (colors[ColorManager.Y_INDEX] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		} else if (colors[ColorManager.U_INDEX] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		} else if (colors[ColorManager.V_INDEX] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		} else if (colors[ColorManager.Y_INDEX].length != this.size) {
			throw new IllegalArgumentException("The given luminance size is not equal to the MacroBlock size!");
		} else if (colors[ColorManager.U_INDEX].length != this.size >> 1
				|| colors[ColorManager.V_INDEX].length != this.size >> 1) {
			throw new IllegalArgumentException("The given chrominance size is not equal to the MacroBlock size!");
		}
		
		if (this.size == 0) {
			super.setSize(colors[ColorManager.Y_INDEX].length);
		}
		
		this.Y = colors[ColorManager.Y_INDEX];
		this.U = colors[ColorManager.U_INDEX];
		this.V = colors[ColorManager.V_INDEX];
		this.isColorSet = true;
	}
	
	/**
	 * <p>Initializes the color components of the MacroBlock individually.</p>
	 * 
	 * @param Y	Y values in the MacroBlock
	 * @param U	U values in the MacroBlock
	 * @param V	V values in the MacroBlock
	 * 
	 * @throws NullPointerException	if the following situations:
	 * <ul><li>If the Y component is null
	 * <li>If the U component is null
	 * <li>If the V component is null
	 * </ul>
	 */
	public void setColorComponents(final double[][] Y, final double[][] U, final double[][] V) {
		if (Y == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		} else if (U == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		} else if (V == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		} else if (Y.length != this.size) {
			throw new IllegalArgumentException("The given luminance size is not equal to the MacroBlock size!");
		} else if (U.length != this.size >> 1 || V.length != this.size >> 1) {
			throw new IllegalArgumentException("The given chrominance size is not equal to the MacroBlock size!");
		}
		
		if (this.size == 0) {
			super.setSize(Y.length);
		}
		
		this.Y = Y;
		this.U = U;
		this.V = V;
		this.isColorSet = true;
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
		} else if (this.Y == null) {
			throw new NullPointerException("No Luma-Y Component");
		} else if (this.U == null) {
			throw new NullPointerException("No Chroma-U Component");
		} else if (this.V == null) {
			throw new NullPointerException("No Chroma-V Component");
		} if (!this.isColorSet) {
			throw new IllegalStateException("The MacroBlock is ready, but no data was set!");
		}
		
		final int subSX = x >> 1;
		final int subSY = y >> 1;
		return new double[] {this.Y[x][y], this.U[subSX][subSY], this.V[subSX][subSY]};
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
		} else if (this.Y == null) {
			throw new NullPointerException("No Luma-Y Component");
		} else if (this.U == null) {
			throw new NullPointerException("No Chroma-U Component");
		} else if (this.V == null) {
			throw new NullPointerException("No Chroma-V Component");
		} if (!this.isColorSet) {
			throw new IllegalStateException("The MacroBlock is ready, but no data was set!");
		}
		
		final int subSX = x >> 1;
		final int subSY = y >> 1;
		cache[ColorManager.Y_INDEX] = this.Y[x][y];
		cache[ColorManager.U_INDEX] = this.U[subSX][subSY];
		cache[ColorManager.V_INDEX] = this.V[subSX][subSY];
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
		}
		
		final int subSX = x >> 1;
		final int subSY = y >> 1;
		this.Y[x][y] = YUV[ColorManager.Y_INDEX];
		this.U[subSX][subSY] = YUV[ColorManager.U_INDEX];
		this.V[subSX][subSY] = YUV[ColorManager.V_INDEX];
	}
	
	/**
	 * Gets the position relative to the parent in form of a {@code Point}.
	 * 
	 * @return The position relative to its parent.
	 */
	public Point getPositionRelativeToParent() {
		return new Point(this.positionRelativeToParentX, this.positionRelativeToParentY);
	}
	
	/**
	 * Gets the position relative to its parent to the x direction.
	 * 
	 * @return The x position relative to its parent.
	 */
	public int getPositionRelativeToParentX() {
		return this.positionRelativeToParentX;
	}
	
	/**
	 * Gets the position relative to its parent to the y direction.
	 * 
	 * @return The y position relative to its parent.
	 */
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
		this.nodes = new MacroBlock[4];
		int index = 0;
		final int fraction = this.size >> 1;
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
				
				final MacroBlock b;
				
				if (this.isColorSet) {
					b = getSubBlock(x, y, fraction);
				} else {
					b = new MacroBlock(x, y, fraction, false);
				}
				
				b.setPositionRelativeToParent(x + this.positionRelativeToParentX, y + this.positionRelativeToParentY);
				this.nodes[index++] = b;
			}
		}
		
		//All color components are referenced by the children.
		this.Y = null;
		this.U = null;
		this.V = null;
	}
	
	/**
	 * <p>Get the nodes of the current MacroBlock.</p>
	 * @return Nodes of the MacroBlock
	 */
	public MacroBlock[] getNodes() {
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
	 * <p>Get the mean color of the MacroBlock.</p>
	 * @return Mean color of the MacroBlock
	 */
	public double[] getMeanColor() {
		if (!this.isColorSet) {
			throw new IllegalStateException("The MacroBlock is ready, but no data was set!");
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
		if (!this.isColorSet) {
			throw new IllegalStateException("The MacroBlock is ready, but no data was set!");
		}
		
		return new double[][][] {this.Y, this.U, this.V};
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
	 * @param x		Position x of the parent block.
	 * @param y		Position y of the parent block.
	 * @param size	Size of the sub-block
	 * 
	 * @throws ArrayIndexOutOfBoundsException	If x or y is below 0 or bigger
	 * than the MacroBlock size
	 * @throws IllegalArgumentException	When the size is smaller than 1 or bigger
	 * than the MacroBlock itself
	 */
	private MacroBlock getSubBlock(final int x, final int y, final int size) {
		final double[][][] res = getColorSubBlock(x, y, size, null);
		final int posX = x + this.positionX;
		final int posY = y + this.positionY;
		return new MacroBlock(posX, posY, size, res[ColorManager.Y_INDEX], res[ColorManager.U_INDEX], res[ColorManager.V_INDEX]);
	}
	
	/**
	 * <p>Get a smaller sub-block off of the current MacroBlock with
	 * the specified size.</p>
	 * 
	 * @return Sub-block from the MacroBlock.
	 * 
	 * @param posX	X position of the sub-block.
	 * @param posY	Y position of the sub-block.
	 * @param size	Size of the sub-block.
	 * @param cache	Cache for storing colors temporarily.
	 * 
	 * @throws ArrayIndexOutOfBoundsException	If x or y is below 0 or bigger
	 * than the MacroBlock size
	 * @throws IllegalArgumentException	When the size is smaller than 1 or bigger
	 * than the MacroBlock itself
	 */
	private double[][][] getColorSubBlock(final int posX, final int posY, final int size, double[][][] cache) {
		if (posX < 0 || posX >= this.size) {
			throw new ArrayIndexOutOfBoundsException();
		} else if (posY < 0 || posY >= this.size) {
			throw new ArrayIndexOutOfBoundsException();
		} else if (size < 1 || size > this.size) {
			throw new IllegalArgumentException("Size cannot exceed the maximum size itself and cannot be 0 or lower");
		} else if (!this.isColorSet) {
			throw new IllegalStateException("The MacroBlock is ready, but no data was set!");
		}
		
		final int halfSize = size >> 1;
		final int halfPosX = posX >> 1;
		final int halfPosY = posY >> 1;
		double[][][] res = cache == null ? ArrayUtils.get3DArray(size, true)
				: cache[ColorManager.Y_INDEX].length < size ? ArrayUtils.get3DArray(size, true) : cache;
		ArrayUtils.copy2DArray(this.Y, posX, posY, res[ColorManager.Y_INDEX], 0, 0, size, size);
		ArrayUtils.copy2DArray(this.U, halfPosX, halfPosY, res[ColorManager.U_INDEX], 0, 0, halfSize, halfSize);
		ArrayUtils.copy2DArray(this.V, halfPosX, halfPosY, res[ColorManager.V_INDEX], 0, 0, halfSize, halfSize);
		return res;
	}
	
	/**
	 * Sets the linked component.
	 * 
	 * @param comp	Linked component.
	 */
	public void setLink(final Component2D comp) {
		this.link = comp;
	}
	
	/**
	 * Gets the linked component.
	 * 
	 * @return The linked component.
	 */
	public Component2D getLink() {
		return this.link;
	}
	
	public void reset(final boolean initColors) {
		this.positionRelativeToParentX = 0;
		this.positionRelativeToParentY = 0;
		
		if (initColors) {
			final int halfSize = size >> 1;
			this.Y = new double[size][size];
			this.U = new double[halfSize][halfSize];
			this.V = new double[halfSize][halfSize];
		} else {
			this.Y = null;
			this.U = null;
			this.V = null;
		}
		
		this.nodes = null;
		this.isSubdivided = false;
		this.meanColor = ColorManager.NULL_COLOR;
		this.MSE = 0;
		this.reference = 0;
		this.isColorSet = false;
	}
	
	@Override
	public void discard() {
		super.discard();
		reset(false);
	}
	
	@Override
	public MacroBlock clone() {
		final int halfSize = this.size >> 1;
		double[][] YClone = new double[this.size][this.size];
		double[][] UClone = new double[halfSize][halfSize];
		double[][] VClone = new double[halfSize][halfSize];
		ArrayUtils.copy2DArray(this.Y, 0, 0, YClone, 0, 0, this.size, this.size);
		ArrayUtils.copy2DArray(this.U, 0, 0, UClone, 0, 0, halfSize, halfSize);
		ArrayUtils.copy2DArray(this.V, 0, 0, VClone, 0, 0, halfSize, halfSize);
		return new MacroBlock(this.positionX, this.positionY, this.size, YClone, UClone, VClone);
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "["
			+ "x=" + this.positionX + ", y=" + this.positionY + ", "
			+ "x_relToParent=" + this.positionRelativeToParentX + ", y_relToParent=" + this.positionRelativeToParentY + ", "
			+ "size=" + this.size + ", " 
			+ "subdivided=" + this.isSubdivided
			+ "]";
	}
}