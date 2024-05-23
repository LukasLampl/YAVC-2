package Utils;

import java.awt.Dimension;
import java.awt.Point;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private int[] meanColor = new int[3];
	
	private double ORDER = 0;
	private double MSE = Double.MAX_VALUE;
	private int reference = 0;
	
	public MacroBlock(Point position, int size) {
		this.position = position;
		this.size = size;
	}
	
	public MacroBlock(Point position, int size, double[][] Y, double[][] U, double[][] V, double[][] A) {
		this.position = position;
		this.size = size;
		this.Y = Y;
		this.U = U;
		this.V = V;
		this.A = A;
	}
	
	public MacroBlock(Point position, int size, double[][][] colors) {
		this.position = position;
		this.size = size;
		this.Y = colors[0];
		this.U = colors[1];
		this.V = colors[2];
		this.A = colors[3];
	}
	
	public void setColorComponents(double[][] Y, double[][] U, double[][] V, double[][] A) {
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
		if (x < 0 || x >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(X) " + x + " is bigger than " + this.size);
		} else if (y < 0 || y >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(Y) " + y + " is bigger than " + this.size);
		}
		
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
	
	public void subdivide(double errorThreshold, int depth, int[][] meanOf4x4Blocks, Dimension dim) {
		if (this.isSubdivided == true) {
			return;
		} else if (this.size <= 4) {
			return;
		}
		
		this.isSubdivided = true;
		this.nodes = new MacroBlock[4];
		int index = 0, currentOrder = 0, fraction = this.size / 2, outlyers = 0;
		
		for (int x = 0; x < this.size; x += fraction) {
			for (int y = 0; y < this.size; y += fraction) {
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
				b.setOrder(this.ORDER + (this.FACTOR_TABLE[depth] * currentOrder++));
				this.nodes[index++] = b;
				
				int[][] subMeanColorArray = getMeanColorSubArray(x, y, fraction, meanOf4x4Blocks);
				int[] meanRGB = calculateMeanOfCurrentBlock(subMeanColorArray);
				double standardDeviation = computeStandardDeviation(meanRGB);
				b.setMeanColor(meanRGB);
				
				if (standardDeviation > errorThreshold) {
					b.subdivide(errorThreshold, depth + 1, subMeanColorArray, dim);
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
	
	public int[][] calculate4x4Means() {
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		int[][] argbs = new int[this.size / 4][this.size / 4];
		int[] rgbCache = new int[3];
		
		for (int n = 0; n < this.size; n += 32) {
			for (int j = 0; j < this.size; j += 32) {
				final int posX = n, posY = j, fracX = n / 32, fracY = j / 32;
				
				Callable<Void> task = () -> {
					for (int u = 0; u < 32; u += 4) {
						for (int v = 0; v < 32; v += 4) {
							int sumR = 0, sumG = 0, sumB = 0;
							
							for (int x = 0; x < 4; x++) {
								for (int y = 0; y < 4; y++) {
									this.COLOR_MANAGER.convertYUVToRGB_intARR(getYUV(posX + x + u, posY + y + v), rgbCache);
									sumR += rgbCache[0];
									sumG += rgbCache[1];
									sumB += rgbCache[2];
								}
							}
							
							argbs[fracX + u / 4][fracY / 16 + v / 4] = ((sumR / 16) << 16) | ((sumG / 16) << 8) | (sumB / 16);
						}
					}
					
					return null;
				};
				
				executor.submit(task);
			}
		}
		
		executor.shutdown();
		return argbs;
	}
	
	private int[][] getMeanColorSubArray(int posX, int posY, int size, int[][] meanColorArray) {
		int[][] subArray = new int[size / 4][size / 4];
		int actualPosX = posX / 4, actualPosY = posY / 4;
		int actualSize = size / 4, fraction = this.size / 4;
		
		for (int x = 0; x < actualSize; x++) {
			if (x + actualPosX >= fraction || x + actualPosX < 0) continue;
			
			for (int y = 0; y < actualSize; y++) {
				if (y + actualPosY >= fraction || y + actualPosY < 0) continue;
				
				subArray[x][y] = meanColorArray[x + actualPosX][y + actualPosY];
			}
		}
		
		return subArray;
	}
	
	public int[] calculateMeanOfCurrentBlock(int[][] meanOf4x4Blocks) {
		double sumR = 0, sumG = 0, sumB = 0;
		int actualSize = meanOf4x4Blocks.length, length = actualSize * actualSize;
		
		for (int x = 0; x < actualSize; x++) {
			for (int y = 0; y < actualSize; y++) {
				int argb = meanOf4x4Blocks[x][y];
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

	public double computeStandardDeviation(int[] mean) {
		double resR = 0, resG = 0, resB = 0;
		double length = this.size * this.size;
		int meanR = mean[0], meanG = mean[1], meanB = mean[2];
		
		for (int x = 0; x < this.size; x++) {
			for (int y = 0; y < this.size; y++) {
				int argb = this.COLOR_MANAGER.convertYUVToRGB(getYUV(x, y));
				int r = ((argb >> 16) & 0xFF) - meanR;
				int g = ((argb >> 8) & 0xFF) - meanG;
				int b = (argb & 0xFF) - meanB;
				resR += r * r;
				resG += g * g;
				resB += b * b;
			}
		}
		
		resR = Math.sqrt(resR /= length);
		resG = Math.sqrt(resG /= length);
		resB = Math.sqrt(resB /= length);
		return (resR + resG + resB);
	}
}