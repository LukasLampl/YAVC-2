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

package utils;

import java.awt.Dimension;
import java.awt.Point;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
	 * <p>ColorManager for color conversion operations.</p>
	 */
	private ColorManager COLOR_MANAGER = new ColorManager();
	
	/**
	 * <p>Position of the MacroBlock, originated from the PixelRaster.</p>
	 */
	private Point position = null;
	
	/**
	 * <p>Size of the MacroBlock.</p>
	 */
	private int size = 0;
	
	/**
	 * <p>Sets a flag, whether it is subdivided into 4 more blocks or not.</p>
	 * <ul><li><b>true</b> = Is subdivided
	 * <li><b>false</b> = Is not subdivided
	 * </ul>
	 */
	private boolean isSubdivided = false;
	
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
	 * 
	 * @throws NullPointerException	When the position is null
	 * @throws IllegalArgumentException	If the size is below 0
	 */
	public MacroBlock(Point position, int size) {
		if (position == null) {
			throw new NullPointerException("MacroBlock can't have position NULL");
		} else if (size < 0) {
			throw new IllegalArgumentException("The size of " + size + " is not supported!");
		}
		
		this.position = position;
		this.size = size;
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
		} else if (size < 0) {
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
		} else if (size < 0) {
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
	
	/**
	 * <p>Determines the order in more depth MacroBlocks.</p>
	 */
	private double FACTOR_TABLE[] = {0.1, 0.01, 0.001, 0.0001, 0.00001};
	
	/**
	 * <p>Subdivides a MacroBlock into 4 equally sized subblocks using recursion.
	 * The subdivision is determined by the standard deviation of the mean-color
	 * and actual color of the current MacroBlock.</p>
	 * 
	 * <p>The minimum size is 4. When a subdivided block is out of the
	 * PixelRaster, it is destroyed. If a subdivided MacroBlock is at the
	 * boundary, it is split, until it is fully inside.</p>
	 * 
	 * @param errorThreshold	Maximum error, until the block is split
	 * @param depth	Depth of the current MacroBlock (Tree view)
	 * @param meanOf4x4Blocks	Precalculated 4x4 mean colors
	 * @param argbs	Precalculated array of all ARGB colors in the MacroBlock
	 * @param dim	Dimension of the PixelRaster
	 * @param innerPos	Position of the MacroBlock in the parent MacroBlock
	 * 
	 * @throws NullPointerException	When the mean of 4x4 blocks is null or the
	 * argb array is null
	 */
	public void subdivide(double errorThreshold, int depth, int[][] meanOf4x4Blocks, int[][][] argbs, Dimension dim, Point innerPos) {
		if (meanOf4x4Blocks == null) {
			throw new NullPointerException("No 4x4Mean, can't subdivide MacroBlock");
		} else if (argbs == null) {
			throw new NullPointerException("No ARGB colors, can't subdivide MacroBlock");
		}
		
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
				b.setOrder(this.ORDER + (this.FACTOR_TABLE[depth] * index));
				this.nodes[index++] = b;
				
				Point bPos = b.getPosition();
				Point newInnerPos = new Point(innerPos.x + x, innerPos.y + y);
				int[] meanRGB = calculateMeanOfCurrentBlock(meanOf4x4Blocks, newInnerPos, fraction);
				double standardDeviation = computeStandardDeviation(meanRGB, argbs, newInnerPos, fraction);
				b.setMeanColor(meanRGB);
				
				if (standardDeviation > errorThreshold
					|| bPos.x + fraction > dim.width
					|| bPos.y + fraction > dim.height) {
					b.subdivide(errorThreshold, depth + 1, meanOf4x4Blocks, argbs, dim, newInnerPos);
				}
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
			for (int y = 0; y < size; y++) {
				resY[x][y] = this.Y[pos.x + x][pos.y + y];
				resA[x][y] = this.A[pos.x + x][pos.y + y];
			}
		}
		
		for (int x = 0; x < halfSize; x++) {
			for (int y = 0; y < halfSize; y++) {
				int thisPosX = (pos.x / 2) + x;
				int thisPosY = (pos.y / 2) + y;
				resU[x][y] = this.U[thisPosX][thisPosY];
				resV[x][y] = this.V[thisPosX][thisPosY];
			}
		}
		
		Point position = new Point(pos.x + this.position.x, pos.y + this.position.y);
		return new MacroBlock(position, size, resY, resU, resV, resA);
	}
	
	/**
	 * <p>Calculate all 4x4 sized mean colors of the MacroBlock
	 * and while that an array of all RGB colors is created.</p>
	 * 
	 * <p><strong>Warning:</strong> The process is multithreaded.
	 * Event though it might lead to performance impact if used a lot.</p>
	 * 
	 * @return A structure that contains the 4x4 mean colors and the
	 * RGB array
	 */
	public MeanStructure calculate4x4Means() {
		int[][] meanArgbs = new int[this.size / 4][this.size / 4];
		int[][][] argbs = new int[this.size][this.size][3];
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		for (int i = 0; i < this.size; i += 32) {
			for (int j = 0; j < this.size; j += 32) {
				executor.submit(create4x4MeansFractionTask(i, j, 32, argbs, meanArgbs));
			}
		}

		executor.shutdown();
		
		try {
			while (!executor.awaitTermination(20, TimeUnit.MICROSECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new MeanStructure(meanArgbs, argbs);
	}
	
	/**
	 * <p>Creates a task for working a fraction of the means from the MacroBlock down.</p>
	 * <p>This function changes to values in the two provided arrays!</p>
	 * 
	 * @return Runnable that calculates the 4x4 means as well as the RGB color for an area
	 * within the MacroBlock, starting from startX and startY with the size frac.
	 * 
	 * @param startX	Start position X within the MacroBlock
	 * @param startY	Start position Y within the MacroBlock
	 * @param frac	Size of the fraction that should be worked down
	 * @param argbs	Array for storing all RGB colors in the fraction
	 * @param meanArgbs	Array for storing all 4x4 means
	 */
	private Runnable create4x4MeansFractionTask(final int startX, final int startY, final int frac, int[][][] argbs, int[][] meanArgbs) {
		Runnable task = () -> {
			for (int u = 0; u < frac; u += 4) {
				for (int v = 0; v < frac; v += 4) {
					int sumR = 0;
					int sumG = 0;
					int sumB = 0;
					
					for (int x = 0; x < 4; x++) {
						for (int y = 0; y < 4; y++) {
							int iPosX = startX + u + x;
							int iPosY = startY + v + y;
							int[] col = this.COLOR_MANAGER.convertYUVToRGB_intARR(getYUV(iPosX, iPosY), null);
							sumR += col[0];
							sumG += col[1];
							sumB += col[2];
							argbs[iPosX][iPosY] = col;
						}
					}
					
					int meanColor = ((sumR / 16) << 16) | ((sumG / 16) << 8) | (sumB / 16);
					meanArgbs[(startX + u) / 4][(startY + v) / 4] = meanColor;
				}
			}
		};
		
		return task;
	}

	/**
	 * <p>This calculates the mean of a child block.</p>
	 * 
	 * @return An array containing the mean of every RGB component in
	 * the following order:
	 * <ul><li>[0] = Red
	 * <li>[1] = Green
	 * <li>[2] = Blue
	 * </ul>
	 * 
	 * @param meanOf4x4Blocks	Precalculated 4x4 mean colors
	 * @param pos	Position of the child block within the root MacroBlock
	 * @param size	Size of the child block
	 */
	public int[] calculateMeanOfCurrentBlock(int[][] meanOf4x4Blocks, Point pos, int size) {
		double sumR = 0;
		double sumG = 0;
		double sumB = 0;
		int actualSize = size / 4;
		int length = actualSize * actualSize;
		int actualPosX = pos.x / 4;
		int actualPosY = pos.y / 4;
		
		for (int x = 0; x < actualSize; x++) {
			for (int y = 0; y < actualSize; y++) {
				int argb = meanOf4x4Blocks[x + actualPosX][y + actualPosY];
				double r = (argb >> 16) & 0xFF;
				double g = (argb >> 8) & 0xFF;
				double b = argb & 0xFF;
				sumR += r;
				sumG += g;
				sumB += b;
			}
		}

		sumR /= length;
		sumG /= length;
		sumB /= length;
		return new int[] {(int)Math.round(sumR), (int)Math.round(sumG), (int)Math.round(sumB)};
	}

	/**
	 * <p>Computes the standard deviation compared to the orignal
	 * colors.</p>
	 * 
	 * @return The standard deviation of red, green and blue combined.
	 * 
	 * @param mean	Mean color of the MacroBlock
	 * @param argbs	RGB color array of the root MacroBlock
	 * @param pos	Position of the child block within the root MacroBlock
	 * @param size	Size of the child block
	 */
	public double computeStandardDeviation(int[] mean, int[][][] argbs, Point pos, int size) {
		double resR = 0;
		double resG = 0;
		double resB = 0;
		double length = size * size;
		int meanR = mean[0];
		int meanG = mean[1];
		int meanB = mean[2];
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				int r = argbs[x + pos.x][y + pos.y][0] - meanR;
				int g = argbs[x + pos.x][y + pos.y][1] - meanG;
				int b = argbs[x + pos.x][y + pos.y][2] - meanB;
				resR += r * r;
				resG += g * g;
				resB += b * b;
			}
		}
		
		resR = Math.sqrt(resR / length);
		resG = Math.sqrt(resG / length);
		resB = Math.sqrt(resB / length);
		return (resR + resG + resB);
	}
}