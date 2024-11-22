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

package app.utils;

import java.awt.Dimension;
import java.awt.Point;

import app.rendering.ColorManager;

/**
 * <p>The class {@code MacroBlock} is the main transform unit
 * in the YAVC video compressor.</p>
 * 
 * @author Lukas Lampl
 * @since 17.0
 * @version 1.0 29 May 2024
 */

public class MacroBlock implements Discardable {
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
	private int[] meanColor = ColorManager.NULL_COLOR;
	
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
		if (size < 0 || size > 65535) {
			throw new IllegalArgumentException("The size of " + size + " is not supported!");
		}
		
		this.positionX = x;
		this.positionY = y;
		this.size = size;
		this.squared_size = size * size;
		
		if (initColor) {
			final int halfSize = size / 2;
			this.Y = new double[size][size];
			this.U = new double[halfSize][halfSize];
			this.V = new double[halfSize][halfSize];
			this.isColorSet = true;
		}
	}
	
	/**
	 * <p>Initializes a MacroBlock with Position, Size,
	 * Y, U, V and A.</p>
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
		if (size < 0 || size > 65535) {
			throw new IllegalArgumentException("The size of " + size + " is not supported!");
		} else if (Y == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		} else if (U == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		} else if (V == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		}
		
		this.positionX = x;
		this.positionY = y;
		this.size = size;
		this.squared_size = size * size;
		this.Y = Y;
		this.U = U;
		this.V = V;
		this.isColorSet = true;
	}
	
	/**
	 * <p>Initializes a MacroBlock with Position, Size,
	 * Y, U, V and A. The color components should have the following order:
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
		if (size < 0 || size > 65535) {
			throw new IllegalArgumentException("The size of " + size + " is not supported!");
		} else if (colors[ColorManager.Y_INDEX] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		} else if (colors[ColorManager.U_INDEX] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		} else if (colors[ColorManager.V_INDEX] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		}
		
		this.positionX = x;
		this.positionY = y;
		this.size = size;
		this.squared_size = size * size;
		this.Y = colors[ColorManager.Y_INDEX];
		this.U = colors[ColorManager.U_INDEX];
		this.V = colors[ColorManager.V_INDEX];
		this.isColorSet = true;
	}
	
	/**
	 * <p>Initializes the color components of the MacroBlock individually.</p>
	 * 
	 * @param color	Color components of the MacroBlock.
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
		} else if (colors[ColorManager.U_INDEX].length != this.size / 2 || colors[ColorManager.V_INDEX].length != this.size / 2) {
			throw new IllegalArgumentException("The given chrominance size is not equal to the MacroBlock size!");
		}
		
		if (this.size == 0) {
			this.size = colors[ColorManager.Y_INDEX].length;
			this.squared_size = this.size * this.size;
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
	 * @param A	A values in the MacroBlock
	 * 
	 * @throws NullPointerException	if the following situations:
	 * <ul><li>If the Y component is null
	 * <li>If the U component is null
	 * <li>If the V component is null
	 * <li>If the A component is null
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
		} else if (U.length != this.size / 2 || V.length != this.size / 2) {
			throw new IllegalArgumentException("The given chrominance size is not equal to the MacroBlock size!");
		}
		
		if (this.size == 0) {
			this.size = Y.length;
			this.squared_size = this.size * this.size;
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
		
		final int subSX = x / 2;
		final int subSY = y / 2;
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
		
		final int subSX = x / 2;
		final int subSY = y / 2;
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
		
		final int subSX = x / 2;
		final int subSY = y / 2;
		this.Y[x][y] = YUV[ColorManager.Y_INDEX];
		this.U[subSX][subSY] = YUV[ColorManager.U_INDEX];
		this.V[subSX][subSY] = YUV[ColorManager.V_INDEX];
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
		this.nodes = new MacroBlock[4];
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
				
				MacroBlock b = getSubBlock(new Point(x, y), fraction);
				b.setPositionRelativeToParent(x + this.positionRelativeToParentX, y + this.positionRelativeToParentY);
				this.nodes[index++] = b;
			}
		}
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
	public int[] getMeanColor() {
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
	public void setMeanColor(final int[] meanColor) {
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
	 * @param pos	Position of the sub-block within the MacroBlock
	 * @param size	Size of the sub-block
	 * 
	 * @throws ArrayIndexOutOfBoundsException	If x or y is below 0 or bigger
	 * than the MacroBlock size
	 * @throws IllegalArgumentException	When the size is smaller than 1 or bigger
	 * than the MacroBlock itself
	 */
	private MacroBlock getSubBlock(final Point pos, final int size) {
		final double[][][] res = getColorSubBlock(pos.x, pos.y, size, null);
		final int posX = pos.x + this.positionX;
		final int posY = pos.y + this.positionY;
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
		
		final int halfSize = size / 2;
		final int halfPosX = posX / 2;
		final int halfPosY = posY / 2;
		double[][][] res = cache == null ? getArray(size) : cache[ColorManager.Y_INDEX].length < size ? getArray(size) : cache;
		ArrayUtils.copy2DArray(this.Y, posX, posY, res[ColorManager.Y_INDEX], 0, 0, size, size);
		ArrayUtils.copy2DArray(this.U, halfPosX, halfPosY, res[ColorManager.U_INDEX], 0, 0, halfSize, halfSize);
		ArrayUtils.copy2DArray(this.V, halfPosX, halfPosY, res[ColorManager.V_INDEX], 0, 0, halfSize, halfSize);
		return res;
	}
	
	/**
	 * <p>Get an array of 2D arrays.</p>
	 * 
	 * @param size	size if the arrays
	 * @return initialized array
	 */
	private double[][][] getArray(final int size) {
		final int halfSize = size / 2;
		double[][][] res = new double[4][][]; //0 = Y; 1 = U; 2 = V
		res[ColorManager.Y_INDEX] = new double[size][size];
		res[ColorManager.U_INDEX] = new double[halfSize][halfSize];
		res[ColorManager.V_INDEX] = new double[halfSize][halfSize];
		return res;
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
	
	public void reset(final int x, final int y, final int size, final boolean initColors) {
		this.positionX = x;
		this.positionY = y;
		this.positionRelativeToParentX = 0;
		this.positionRelativeToParentY = 0;
		this.size = size;
		
		if (initColors) {
			final int halfSize = size / 2;
			this.Y = new double[size][size];
			this.U = new double[halfSize][halfSize];
			this.V = new double[halfSize][halfSize];
		} else {
			this.Y = null;
			this.U = null;
			this.V = null;
		}
		
		this.nodes = null;
		this.isConvertedToVector = false;
		this.isSubdivided = false;
		this.meanColor = ColorManager.NULL_COLOR;
		this.MSE = 0;
		this.squared_size = size * size;
		this.reference = 0;
		this.isColorSet = false;
	}
	
	@Override
	public void discard() {
		reset(0, 0, 0, false);
	}
}