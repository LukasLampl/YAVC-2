package YAVC.Utils;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MacroBlock {
	private double[][] Y = null;
	private double[][] U = null;
	private double[][] V = null;
	private double[][] A = null;
	
	private ColorManager COLOR_MANAGER = new ColorManager();
	private Point position = null;
	private int size = 0;
	private boolean isSubdivided = false;
	private MacroBlock[] nodes = null;
	private int[] meanColor = {255, 0, 255};
	
	private double ORDER = 0;
	private double MSE = Double.MAX_VALUE;
	private int reference = 0;
	
	public MacroBlock(Point position, int size) {
		if (position == null) throw new NullPointerException("MacroBlock can't have position NULL");
		else if (size < 0) throw new IllegalArgumentException("The size of " + size + " is not supported!");
		
		this.position = position;
		this.size = size;
	}
	
	public MacroBlock(Point position, int size, double[][] Y, double[][] U, double[][] V, double[][] A) {
		if (position == null) throw new NullPointerException("MacroBlock can't have position NULL");
		else if (size < 0) throw new IllegalArgumentException("The size of " + size + " is not supported!");
		else if (Y == null) throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		else if (U == null) throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		else if (V == null) throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		else if (A == null) throw new NullPointerException("MacroBlock can't have a NULL Alpha channel");
		
		this.position = position;
		this.size = size;
		this.Y = Y;
		this.U = U;
		this.V = V;
		this.A = A;
	}
	
	public MacroBlock(Point position, int size, double[][][] colors) {
		if (position == null) throw new NullPointerException("MacroBlock can't have position NULL");
		else if (size < 0) throw new IllegalArgumentException("The size of " + size + " is not supported!");
		else if (colors[0] == null) throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		else if (colors[1] == null) throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		else if (colors[2] == null) throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		else if (colors[3] == null) throw new NullPointerException("MacroBlock can't have a NULL Alpha channel");
		
		this.position = position;
		this.size = size;
		this.Y = colors[0];
		this.U = colors[1];
		this.V = colors[2];
		this.A = colors[3];
	}
	
	public void setColorComponents(double[][] Y, double[][] U, double[][] V, double[][] A) {
		if (Y == null) throw new NullPointerException("MacroBlock can't have a NULL Luma-Y channel");
		else if (U == null) throw new NullPointerException("MacroBlock can't have a NULL Chroma-U channel");
		else if (V == null) throw new NullPointerException("MacroBlock can't have a NULL Chroma-V channel");
		else if (A == null) throw new NullPointerException("MacroBlock can't have a NULL Alpha channel");
		
		this.Y = Y;
		this.U = U;
		this.V = V;
		this.A = A;
	}
	
	/*
	 * Purpose: Get the YUV color at the specific position (x and y) with the reverse subsampled chroma values
	 * Return Type: double[] => YUV color (Y at [0], U at [1] and V at [2])
	 * Params: int x => X position;
	 * 			int y => Y position
	 */
	public double[] getYUV(int x, int y) {
		if (x < 0 || x >= this.size) throw new ArrayIndexOutOfBoundsException("(X) " + x + " is out of bounds (" + this.size + ")");
		else if (y < 0 || y >= this.size) throw new ArrayIndexOutOfBoundsException("(Y) " + y + " is out of bounds (" + this.size + ")");
		else if (this.Y == null) throw new NullPointerException("No Luma-Y Component");
		else if (this.U == null) throw new NullPointerException("No Chroma-U Component");
		else if (this.V == null) throw new NullPointerException("No Chroma-V Component");
		
		int subSX = x / 2, subSY = y / 2;
		return new double[] {this.Y[x][y], this.U[subSX][subSY], this.V[subSX][subSY]};
	}

	/*
	 * Purpose: Subdivides the current MacroBlock recursively down to 4 smaller MacroBlock.
	 * Return Type: void
	 * Params: double errorThreshold => Maximum color error, till subdivision gets aborted;
	 * 			int depth => Depth of the subdivision
	 */
	private double FACTOR_TABLE[] = {0.1, 0.01, 0.001, 0.0001, 0.00001};
	
	public void subdivide(double errorThreshold, int depth, int[][] meanOf4x4Blocks, int[][][] argbs, Dimension dim, Point innerPos) {
		if (meanOf4x4Blocks == null) throw new NullPointerException("No 4x4Mean, can't subdivide MacroBlock");
		else if (argbs == null) throw new NullPointerException("No ARGB colors, can't subdivide MacroBlock");
		
		if (this.isSubdivided == true) return;
		else if (this.size <= 4) return;
		
		this.isSubdivided = true;
		this.nodes = new MacroBlock[4];
		int index = 0, fraction = this.size / 2, outlyers = 0;
		
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
				
				Point newInnerPos = new Point(innerPos.x + x, innerPos.y + y);
				int[] meanRGB = calculateMeanOfCurrentBlock(meanOf4x4Blocks, newInnerPos, fraction);
				double standardDeviation = computeStandardDeviation(meanRGB, argbs, newInnerPos, fraction);
				b.setMeanColor(meanRGB);
				
				if (standardDeviation > errorThreshold) {
					b.subdivide(errorThreshold, depth + 1, meanOf4x4Blocks, argbs, dim, newInnerPos);
				}
			}
		}
	}
	
	public MacroBlock[] getNodes() {
		return this.nodes;
	}
	
	public boolean isSubdivided() {
		return this.isSubdivided;
	}
	
	public Point getPosition() {
		return this.position;
	}
	
	public int getSize() {
		return this.size;
	}
	
	public int[] getMeanColor() {
		return this.meanColor;
	}
	
	public void setMeanColor(int[] meanColor) {
		this.meanColor = meanColor;
	}
	
	public double[][][] getColors() {
		return new double[][][] {this.Y, this.U, this.V, this.A};
	}
	
	public double getMSE() {
		return this.MSE;
	}
	
	public void setMSE(double MSE) {
		this.MSE = MSE;
	}
	
	public void setReference(int ref) {
		this.reference = ref;
	}
	
	public int getReference() {
		return this.reference;
	}
	
	public double getOrder() {
		return this.ORDER;
	}
	
	public void setOrder(double order) {
		this.ORDER = order;
	}
	
	/*
	 * Purpose: Gets a smaller sub-block of the current block with the size "size"
	 * Return Type: MacroBlock => Sub-block
	 * Params: Point pos => Position of the sub-block within the current block;
	 * 			int size => Size of the sub-block
	 */
	private MacroBlock getSubBlock(Point pos, int size) {
		if (pos.x < 0 || pos.x >= this.size) throw new ArrayIndexOutOfBoundsException();
		else if (pos.y < 0 || pos.y >= this.size) throw new ArrayIndexOutOfBoundsException();
		else if (size < 1 || size > this.size) throw new IllegalArgumentException("Size cannot exceed the maximum size itself and cannot be 0 or lower");
		
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
				int thisPosX = (pos.x / 2) + x, thisPosY = (pos.y / 2) + y;
				resU[x][y] = this.U[thisPosX][thisPosY];
				resV[x][y] = this.V[thisPosX][thisPosY];
			}
		}
		
		return new MacroBlock(new Point(pos.x + this.position.x, pos.y + this.position.y), size, resY, resU, resV, resA);
	}
	
	public MeanStructure calculate4x4Means() {
		int[][] meanArgbs = new int[this.size / 4][this.size / 4];
		int[][][] argbs = new int[this.size][this.size][3];
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		ArrayList<Future<?>> futures = new ArrayList<>();
		
		for (int u = 0; u < this.size; u += 4) {
			for (int v = 0; v < this.size; v += 4) {
				final int startX = u, startY = v;
				
				futures.add(executor.submit(() -> {
					int sumR = 0, sumG = 0, sumB = 0;
					
					for (int x = 0; x < 4; x++) {
						for (int y = 0; y < 4; y++) {
							int iPosX = startX + x, iPosY = startY + y;
							int[] col = this.COLOR_MANAGER.convertYUVToRGB_intARR(getYUV(iPosX, iPosY), null);
							sumR += col[0];
							sumG += col[1];
							sumB += col[2];
							argbs[iPosX][iPosY] = col;
						}
					}
					
					meanArgbs[startX / 4][startY / 4] = ((sumR / 16) << 16) | ((sumG / 16) << 8) | (sumB / 16);
				}));
			}
		}
		
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		executor.shutdown();
		return new MeanStructure(meanArgbs, argbs);
	}

	public int[] calculateMeanOfCurrentBlock(int[][] meanOf4x4Blocks, Point pos, int size) {
		double sumR = 0, sumG = 0, sumB = 0;
		int actualSize = size / 4, length = actualSize * actualSize;
		int actualPosX = pos.x / 4, actualPosY = pos.y / 4;
		
		for (int x = 0; x < actualSize; x++) {
			for (int y = 0; y < actualSize; y++) {
				int argb = meanOf4x4Blocks[x + actualPosX][y + actualPosY];
				double r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
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

	public double computeStandardDeviation(int[] mean, int[][][] argbs, Point pos, int size) {
		double resR = 0, resG = 0, resB = 0;
		double length = size * size;
		int meanR = mean[0], meanG = mean[1], meanB = mean[2];
		
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