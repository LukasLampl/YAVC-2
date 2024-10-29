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

/**
 * <p>The class {@code MacroBlock} is the main transform unit
 * in the YAVC video compressor.</p>
 * 
 * @author Lukas Lampl
 * @since 17.0
 * @version 1.0 29 May 2024
 */

public class MacroBlock {
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
	 * <p>The Alpha values of the MacroBlock.
	 * Alpha only plays a role in inter-prediction,
	 * thats why it is only applied in the MacroBlock
	 * and not the PixelRaster.</p>
	 */
	private double[][] A = null;
	
	/**
	 * <p>Position of the MacroBlock, originated from the PixelRaster.</p>
	 */
	private Point position = null;
	
	private Point positionRelativeToParent = new Point(0, 0);
	
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
	private int[] meanColor = {255, 0, 255};
	
	/**
	 * <p>A schematic encoding order for the
	 * MacroBlock</p>
	 */
	private double ORDER = 0;
	
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
	 * @param position	Position of the MacroBlock based on the PixelRaster
	 * @param size	Size of the MacroBlock
	 * @param initColor		Flag for whether the Y, U and V components are initialized or not.
	 * 
	 * @throws NullPointerException	When the position is null
	 * @throws IllegalArgumentException	If the size is below 0
	 */
	public MacroBlock(Point position, int size, boolean initColor) {
		if (position == null) {
			throw new NullPointerException("MacroBlock can't have position NULL");
		} else if (size < 0 || size > 65535) {
			throw new IllegalArgumentException("The size of " + size + " is not supported!");
		}
		
		this.position = position;
		this.size = size;
		this.squared_size = size * size;
		
		if (initColor) {
			int halfSize = size / 2;
			this.Y = new double[size][size];
			this.U = new double[halfSize][halfSize];
			this.V = new double[halfSize][halfSize];
		}
	}
	
	/**
	 * <p>Initializes a MacroBlock with Position, Size,
	 * Y, U, V and A.</p>
	 * 
	 * @param position	Position of the MacroBlock based on the PixelRaster
	 * @param size	Size of the MacroBlock
	 * @param Y	Y values in the MacroBlock
	 * @param U	U values in the MacroBlock
	 * @param V	V values in the MacroBlock
	 * @param A	A values in the MacroBlock
	 * 
	 * @throws NullPointerException	in the following situations:
	 * <ul><li>If the provided position is null
	 * <li>If the Y component is null
	 * <li>If the U component is null
	 * <li>If the V component is null
	 * <li>If the A component is null
	 * </ul>
	 * 
	 * @throws IllegalArgumentException	If the size is below 0
	 */
	public MacroBlock(Point position, int size, double[][] Y, double[][] U, double[][] V, double[][] A) {
		if (position == null) {
			throw new NullPointerException("MacroBlock can't have position NULL");
		} else if (size < 0 || size > 65535) {
			throw new IllegalArgumentException("The size of " + size + " is not supported!");
		} else if (Y == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		} else if (U == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		} else if (V == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		} else if (A == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Alpha channel");
		}
		
		this.position = position;
		this.size = size;
		this.squared_size = size * size;
		this.Y = Y;
		this.U = U;
		this.V = V;
		this.A = A;
	}
	
	/**
	 * <p>Initializes a MacroBlock with Position, Size,
	 * Y, U, V and A. The color components should have the following order:
	 * <ul><li>[0] = Y
	 * <li> [1] = U
	 * <li> [2] = V
	 * <li> [3] = A
	 * </ul>
	 * 
	 * @param position	Position of the MacroBlock based on the PixelRaster
	 * @param size	Size of the MacroBlock
	 * @param Y	Y values in the MacroBlock
	 * @param U	U values in the MacroBlock
	 * @param V	V values in the MacroBlock
	 * @param A	A values in the MacroBlock
	 * 
	 * @throws NullPointerException	if the following situations:
	 * <ul><li>If the provided position is null
	 * <li>If the Y component is null
	 * <li>If the U component is null
	 * <li>If the V component is null
	 * <li>If the A component is null
	 * </ul>
	 * 
	 * @throws IllegalArgumentException	If the size is below 0
	 */
	public MacroBlock(Point position, int size, double[][][] colors) {
		if (position == null) {
			throw new NullPointerException("MacroBlock can't have position NULL");
		} else if (size < 0 || size > 65535) {
			throw new IllegalArgumentException("The size of " + size + " is not supported!");
		} else if (colors[0] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		} else if (colors[1] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		} else if (colors[2] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		} else if (colors[3] == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Alpha channel");
		}
		
		this.position = position;
		this.size = size;
		this.squared_size = size * size;
		this.Y = colors[0];
		this.U = colors[1];
		this.V = colors[2];
		this.A = colors[3];
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
	public void setColorComponents(double[][] Y, double[][] U, double[][] V, double[][] A) {
		if (Y == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		} else if (U == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		} else if (V == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		} else if (A == null) {
			throw new NullPointerException("MacroBlock can't have a NULL Alpha channel");
		}
		
		this.size = Y.length;
		this.squared_size = this.size * this.size;
		this.Y = Y;
		this.U = U;
		this.V = V;
		this.A = A;
	}
	
	/**
	 * <p>Returns the YUV color at the specified position x, y.<br>
	 * <b>Important:</b> The position is relative to the MacroBlock!</p>
	 * 
	 * @return Double array with Y at [0], U at [1] and V at [2].
	 * 
	 * @param x	position X in the MacroBlock itself
	 * @param y position Y in the MacroBlock itself
	 * 
	 * @throws ArrayIndexOutOfBoundsException	if the x or y coordinate
	 * is out of bounds within the MacroBlock
	 * @throws NullPointerException	if the following situations:
	 * <ul><li>If the Y component is null
	 * <li>If the U component is null
	 * <li>If the V component is null
	 * </ul>
	 */
	public double[] getYUV(int x, int y) {
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
		}
		
		int subSX = x / 2;
		int subSY = y / 2;
		return new double[] {this.Y[x][y], this.U[subSX][subSY], this.V[subSX][subSY]};
	}
	
	/**
	 * <p>Sets the provided YUV color at a specific
	 * position.</p>
	 * 
	 * @param x	position X of the color
	 * @param y	position Y of the color
	 * @param YUV	YUV color to set
	 */
	public void setYUV(int x, int y, double[] YUV) {
		if (x < 0 || x >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(X) " + x + " is out of bounds (" + this.size + ")");
		} else if (y < 0 || y >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(Y) " + y + " is out of bounds (" + this.size + ")");
		}
		
		int subSX = x / 2;
		int subSY = y / 2;
		this.Y[x][y] = YUV[0];
		this.U[subSX][subSY] = YUV[1];
		this.V[subSX][subSY] = YUV[2];
	}
	
	public Point getPositionRelativeToParent() {
		return this.positionRelativeToParent;
	}
	
	private void setPositionRelativeToParent(Point pos) {
		this.positionRelativeToParent = pos;
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
	public void subdivide(Dimension dim) {
		if (this.isSubdivided == true) {
			return;
		} else if (this.size <= 4) {
			return;
		}
		
		this.isSubdivided = true;
		this.nodes = new MacroBlock[4];
		int index = 0;
		int fraction = this.size / 2;
		int outlyers = 0;
		
		for (int x = 0; x < size; x += fraction) {
			for (int y = 0; y < size; y += fraction) {
				if ((this.position.x + x >= dim.width
					|| this.position.x + x < 0)
					|| (this.position.y + y >= dim.height
					|| this.position.y + y < 0)) {
					if (outlyers++ >= 4) {
						this.isSubdivided = false;
					}
					
					continue;
				}
				
				MacroBlock b = getSubBlock(new Point(x, y), fraction);
				b.setPositionRelativeToParent(new Point(x + this.positionRelativeToParent.x, y + this.positionRelativeToParent.y));
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
		return this.position;
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
		return this.meanColor;
	}
	
	/**
	 * <p>Set the mean color of the MacroBlock.</p>
	 * 
	 * @param meanColor	mean color of the MacroBlock
	 */
	public void setMeanColor(int[] meanColor) {
		this.meanColor = meanColor;
	}
	
	/**
	 * <p>Get the colors of the MacroBlock.</p>
	 * @return Colors of the MacroBlock
	 */
	public double[][][] getColors() {
		return new double[][][] {this.Y, this.U, this.V, this.A};
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
	public void setMSE(double MSE) {
		this.MSE = MSE;
	}
	
	/**
	 * <p>Set the reference of the MacroBlock.</p>
	 * 
	 * @param ref	Reference of the MacroBlock
	 * to the best matching block
	 */
	public void setReference(int ref) {
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
	 * <p>Get the order of the MacroBlock.</p>
	 * @return Order of the MacroBlock
	 */
	public double getOrder() {
		return this.ORDER;
	}
	
	/**
	 * <p>Set the order of the MacroBlock.</p>
	 * 
	 * @param order	Order of the block in the
	 * encoding process
	 */
	public void setOrder(double order) {
		this.ORDER = order;
	}
	
	/**
	 * <p>Get a smaller subblock off of the current MacroBlock with
	 * the specified size.</p>
	 * 
	 * @return Subblock from the MacroBlock
	 * 
	 * @param pos	Position of the subblock within the MacroBlock
	 * @param size	Size of the subblock
	 * 
	 * @throws ArrayIndexOutOfBoundsException	If x or y is below 0 or bigger
	 * than the MacroBlock size
	 * @throws IllegalArgumentException	When the size is smaller than 1 or bigger
	 * than the MacroBlock itself
	 */
	private MacroBlock getSubBlock(Point pos, int size) {
		if (pos.x < 0 || pos.x >= this.size) {
			throw new ArrayIndexOutOfBoundsException();
		} else if (pos.y < 0 || pos.y >= this.size) {
			throw new ArrayIndexOutOfBoundsException();
		} else if (size < 1 || size > this.size) {
			throw new IllegalArgumentException("Size cannot exceed the maximum size itself and cannot be 0 or lower");
		}
		
		int halfSize = size / 2;
		double[][] resY = new double[size][size];
		double[][] resU = new double[halfSize][halfSize];
		double[][] resV = new double[halfSize][halfSize];
		double[][] resA = new double[size][size];
		
		for (int x = 0; x < size; x++) {
			int posX = pos.x + x;
			
			for (int y = 0; y < size; y++) {
				int posY = pos.y + y;
				resY[x][y] = this.Y[posX][posY];
				resA[x][y] = this.A[posX][posY];
			}
		}
		
		for (int x = 0; x < halfSize; x++) {
			int thisPosX = (pos.x / 2) + x;
			
			for (int y = 0; y < halfSize; y++) {
				int thisPosY = (pos.y / 2) + y;
				resU[x][y] = this.U[thisPosX][thisPosY];
				resV[x][y] = this.V[thisPosX][thisPosY];
			}
		}
		
		Point position = new Point(pos.x + this.position.x, pos.y + this.position.y);
		return new MacroBlock(position, size, resY, resU, resV, resA);
	}
	
	public void setConvertedToVector(boolean convertedToVector) {
		this.isConvertedToVector = convertedToVector;
	}
	
	public boolean isConvertedToVector() {
		return this.isConvertedToVector;
	}
}