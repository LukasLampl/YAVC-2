package Utils;

import java.awt.Point;

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
	private double[] meanColor = new double[3];
	
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
		this.meanColor = computeRGBMean();
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
		this.meanColor = computeRGBMean();
	}
	
	public double[] getYUV(int x, int y) {
		if (x < 0 || x >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(X) " + x + " is bigger than " + this.size);
		} else if (y < 0 || y >= this.size) {
			throw new ArrayIndexOutOfBoundsException("(Y) " + y + " is bigger than " + this.size);
		}
		
		int subSX = x / 2, subSY = y / 2;
		return new double[] {this.Y[x][y], this.U[subSX][subSY], this.V[subSX][subSY]};
	}

	private double FACTOR_TABLE[] = {0.1, 0.01, 0.001, 0.0001, 0.00001};
	
	public void subdivide(double errorThreshold, int depth) {
		if (this.isSubdivided == true) {
			return;
		} else if (this.size <= 4) {
			return;
		}
		
		this.isSubdivided = true;
		this.nodes = new MacroBlock[4];
		int index = 0, currentOrder = 0;
		
		for (int x = 0; x < this.size; x += this.size / 2) {
			for (int y = 0; y < this.size; y += this.size / 2) {
				MacroBlock b = getSubBlock(new Point(x, y), this.size / 2);
				b.setOrder(this.ORDER + (this.FACTOR_TABLE[depth] * currentOrder++));
				this.nodes[index] = b;
				
				double standardDeviation = computeStandardDeviation(this.meanColor);
				
				if (standardDeviation > errorThreshold) {
					b.subdivide(errorThreshold, depth + 1);
				}
				
				index++;
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
	
	public double[] getMeanColor() {
		return this.meanColor;
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
	
	private MacroBlock getSubBlock(Point pos, int size) {
		double[][] resY = new double[size][size];
		double[][] resU = new double[size / 2][size / 2];
		double[][] resV = new double[size / 2][size / 2];
		double[][] resA = new double[size][size];
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				resY[x][y] = this.Y[pos.x + x][pos.y + y];
				resU[x / 2][y / 2] = this.U[(pos.x + x) / 2][(pos.y + y) / 2];
				resV[x / 2][y / 2] = this.V[(pos.x + x) / 2][(pos.y + y) / 2];
				resA[x][y] = this.A[x][y];
			}
		}
		
		return new MacroBlock(new Point(pos.x + this.position.x, pos.y + this.position.y), size, resY, resU, resV, resA);
	}
	
	private double[] computeRGBMean() {
		double sumR = 0, sumG = 0, sumB = 0;
		double length = this.size * this.size;
		
		for (int x = 0; x < this.size; x++) {
			for (int y = 0; y < this.size; y++) {
				int[] rgb = this.COLOR_MANAGER.convertYUVToRGB_intARR(getYUV(x, y));
				sumR += rgb[0];
				sumG += rgb[1];
				sumB += rgb[2];
			}
		}
		
		return new double[] {sumR /= length, sumG /= length, sumB /= length};
	}

	private double computeStandardDeviation(double[] mean) {
		double resR = 0, resG = 0, resB = 0;
		double length = this.size * this.size;
		int meanR = (int)mean[0], meanG = (int)mean[1], meanB = (int)mean[2];
		
		for (int x = 0; x < this.size; x++) {
			for (int y = 0; y < this.size; y++) {
				int[] rgb = this.COLOR_MANAGER.convertYUVToRGB_intARR(getYUV(x, y));
				int r = rgb[0] - meanR;
				int g = rgb[1] - meanG;
				int b = rgb[2] - meanB;
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
