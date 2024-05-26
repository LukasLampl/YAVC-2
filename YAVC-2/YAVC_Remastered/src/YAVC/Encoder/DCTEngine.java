package YAVC.Encoder;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import YAVC.Main.config;

public class DCTEngine {
	public static double[][][][][][] DCT_COEFFICIENTS = new double[2][][][][][]; //Position at [0][][][]... is for DCT; Position at [1][][][]... is for IDCT

	public DCTEngine() {
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		//Sizes that are used for the DCT in YAVC
		int[] sizes = {8, 4, 2};
		DCT_COEFFICIENTS[0] = new double[sizes.length][][][][];
		DCT_COEFFICIENTS[1] = new double[sizes.length][][][][];
		
		for (int i = 0; i < sizes.length; i++) {
			int index = i, m = sizes[index];
			DCT_COEFFICIENTS[0][i] = new double[m][m][m][m];
			DCT_COEFFICIENTS[1][i] = new double[m][m][m][m];
			
			executor.submit(getDCTCoeffs(m, index));
		}
		
		executor.shutdown();
	}
	
	private Callable<Void> getDCTCoeffs(final int m, final int index) {
		int m2 = m * 2;
		
		Callable<Void> task = () -> {
			for (int v = 0; v < m; v++) {
				for (int u = 0; u < m; u++) {
					for (int x = 0; x < m; x++) {
						double cos1 = Math.cos(((double)(2 * x + 1) * (double)v * Math.PI) / m2);
						
						for (int y = 0; y < m; y++) {
							double cos2 = Math.cos(((double)(2 * y + 1) * (double)u * Math.PI) / m2);
							double cos = cos1 * cos2;
							DCT_COEFFICIENTS[0][index][v][u][x][y] = cos;
							DCT_COEFFICIENTS[1][index][x][y][v][u] = cos;
						}
					}
				}
			}
			
			return null;
		};
		
		return task;
	}
	
	private double step(final int x, final int m) {
		return x == 0 ? 1 / Math.sqrt(m) : Math.sqrt(2.0 / (double)m);
	}
	
	private int setIndexOfDCT(final int m) {
		switch (m) {
		case 8: return 0;
		case 4: return 1;
		case 2: return 2;
		default: throw new IllegalArgumentException("Unsupported matrix size: " + m);
		}
	}
		
	public ArrayList<double[][][]> computeDCTOfVectorColorDifference(final double[][][] diffs, final int size) {
		int estimatedSize = (size / 8) * (size / 8);
		ArrayList<double[][][]> coeffs = new ArrayList<double[][][]>(estimatedSize <= 0 ? 2 : estimatedSize);

		if (size == 4) {
			double[][][] chromaDCT = computeChromaDCTCoefficients(diffs[1], diffs[2], 2);
			double[][] lumaDCT = computeLumaDCTCoefficients(diffs[0], 4);
			quantizeChromaDCTCoefficients(chromaDCT, 2);
			quantizeLumaDCTCoefficients(lumaDCT, 4);
			
			double[][][] cache = new double[3][][];
			cache[0] = lumaDCT;
			cache[1] = chromaDCT[0];
			cache[2] = chromaDCT[1];
			
			coeffs.add(cache);
			return coeffs;
		}

		for (int x = 0; x < size; x += 8) {
			for (int y = 0; y < size; y += 8) {
				double[][][] subArr = getSubArray(diffs, 8, x, y);
				double[][][] chromaDCT = computeChromaDCTCoefficients(subArr[1], subArr[2], 4);
				double[][] lumaDCT = computeLumaDCTCoefficients(subArr[0], 8);
				quantizeChromaDCTCoefficients(chromaDCT, 4);
				quantizeLumaDCTCoefficients(lumaDCT, 8);
				
				double[][][] cache = new double[3][][];
				cache[0] = lumaDCT;
				cache[1] = chromaDCT[0];
				cache[2] = chromaDCT[1];
				coeffs.add(cache);
			}
		}
		
		return coeffs;
	}
	
	public double[][][] computeIDCTOfVectorColorDifference(final ArrayList<double[][][]> DCTCoeff, final int size) {
		if (DCTCoeff == null || DCTCoeff.size() == 0) {
			System.err.println("No DCT-II Coefficients to apply IDCT-II on! > NULL");
			return null;
		}
		
		double[][][] res = new double[3][][];
		
		if (size == 4) {
			double[][][] objToProcess = DCTCoeff.get(0);
			double[][][] chromaIDCT = new double[][][] {objToProcess[1], objToProcess[2]};
			double[][] lumaIDCT = objToProcess[0];
			dequantizeChromaDCTCoefficients(chromaIDCT, 2);
			dequantizeLumaDCTCoefficients(lumaIDCT, 4);
			chromaIDCT = computeChromaIDCTCoefficients(chromaIDCT[0], chromaIDCT[1], 2);
			lumaIDCT = computeLumaIDCTCoefficients(lumaIDCT, 4);
			res[0] = lumaIDCT;
			res[1] = chromaIDCT[0];
			res[2] = chromaIDCT[1];
			return res;
		}
		
		res[0] = new double[size][size];
		res[1] = new double[size / 2][size / 2];
		res[2] = new double[size / 2][size / 2];
		
		for (int x = 0, index = 0; x < size; x += 8) {
			for (int y = 0; y < size; y += 8) {
				double[][][] CoeffGroup = DCTCoeff.get(index++);
				double[][][] chromaIDCT = new double[][][] {CoeffGroup[1], CoeffGroup[2]};
				double[][] lumaIDCT = CoeffGroup[0];
				dequantizeChromaDCTCoefficients(chromaIDCT, 4);
				dequantizeLumaDCTCoefficients(lumaIDCT, 8);
				chromaIDCT = computeChromaIDCTCoefficients(chromaIDCT[0], chromaIDCT[1], 4);
				lumaIDCT = computeLumaIDCTCoefficients(lumaIDCT, 8);
				
				double[][][] cache = new double[3][][];
				cache[0] = lumaIDCT;
				cache[1] = chromaIDCT[0];
				cache[2] = chromaIDCT[1];
				writeSubArrayInArray(cache, res, x, y);
			}
		}

		return res;
	}
	
	private void writeSubArrayInArray(final double[][][] subArray, double[][][] dest, final int posX, final int posY) {
		int size = subArray[0].length, halfSize = subArray[1].length;
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				dest[0][posX + x][posY + y] = subArray[0][x][y];
			}
		}
		
		for (int x = 0; x < halfSize; x++) {
			for (int y = 0; y < halfSize; y++) {
				int subSX = (posX + x) / 2, subSY = (posY + y) / 2;
				dest[1][subSX][subSY] = subArray[1][x][y];
				dest[2][subSX][subSY] = subArray[2][x][y];
			}
		}
	}
	
	private double[][][] getSubArray(final double[][][] org, final int size, final int posX, final int posY) {
		double arr[][][] = new double[3][][];
		int halfSize = size / 2;
		
		arr[0] = new double[size][size];
		arr[1] = new double[halfSize][halfSize];
		arr[2] = new double[halfSize][halfSize];
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				arr[0][x][y] = org[0][posX + x][posY + y];
			}
		}
		
		for (int x = 0; x < halfSize; x++) {
			for (int y = 0; y < halfSize; y++) {
				int subSX = (posX + x) / 2, subSY = (posY + y) / 2;
				arr[1][x][y] = org[1][subSX][subSY];
				arr[2][x][y] = org[2][subSX][subSY];
			}
		}
		
		return arr;
	}
	
	private double[][][] computeChromaDCTCoefficients(final double[][] U, final double[][] V, final int m) {
		double resU[][] = new double[m][m];
		double resV[][] = new double[m][m];
		int index = setIndexOfDCT(m);
		double[] steps = {step(0, m), step(1, m)};
		
		for (int v = 0; v < m; v++) {
			for (int u = 0; u < m; u++) {
				double sumU = 0, sumV = 0;

				for (int x = 0; x < m; x++) {
					for (int y = 0; y < m; y++) {
						double cos = DCT_COEFFICIENTS[0][index][v][u][x][y];
						sumU += (U[x][y] - 128) * cos;
						sumV += (V[x][y] - 128) * cos;
					}
				}
				
				double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
				
				resU[v][u] = step * sumU;
				resV[v][u] = step * sumV;
            }
        }
		
		return new double[][][] {resU, resV};
	}
	
	private double[][][] computeChromaIDCTCoefficients(final double[][] U, final double[][] V, final int m) {
		double[][] resU = new double[m][m];
		double[][] resV = new double[m][m];
		double[] steps = {step(0, m), step(1, m)};
		int index = setIndexOfDCT(m);
		
		for (int x = 0; x < m; x++) {
			for (int y = 0; y < m; y++) {
				double sumU = 0, sumV = 0;
				
				for (int u = 0; u < m; u++) {
					for (int v = 0; v < m; v++) {
						double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
						double cos = DCT_COEFFICIENTS[1][index][x][y][u][v];
						sumU += U[u][v] * step * cos;
						sumV += V[u][v] * step * cos;
					}
				}
				
				resU[x][y] = sumU + 128;
				resV[x][y] = sumV + 128;
			}
		}
		
		return new double[][][] {resU, resV};
	}
		
	private double[][] computeLumaDCTCoefficients(final double[][] Y, final int m) {
		double resY[][] = new double[m][m];
		int index = setIndexOfDCT(m);
		double[] steps = {step(0, m), step(1, m)};
		
		for (int v = 0; v < m; v++) {
			for (int u = 0; u < m; u++) {
				double sum = 0;
				
				for (int x = 0; x < m; x++) {
					for (int y = 0; y < m; y++) {
						double cos = DCT_COEFFICIENTS[0][index][v][u][x][y];
						sum += (Y[x][y] - 128) * cos;
					}
				}
				
				double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
				resY[v][u] = step * sum;
			}
		}
		
		return resY;
	}
	
	private double[][] computeLumaIDCTCoefficients(final double[][] Y, final int m) {
		double[][] resY = new double[m][m];
		double[] steps = {step(0, m), step(1, m)};
		int index = setIndexOfDCT(m);
		
		for (int x = 0; x < m; x++) {
			for (int y = 0; y < m; y++) {
				double sum = 0;
				
				for (int u = 0; u < m; u++) {
					for (int v = 0; v < m; v++) {
						double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
						double cos = DCT_COEFFICIENTS[1][index][x][y][u][v];
						sum += Y[u][v] * step * cos;
					}
				}
				
				resY[x][y] = sum + 128;
			}
		}
		
		return resY;
	}

	public void quantizeChromaDCTCoefficients(double[][][] coefficients, final int size) {
		int[][] chromaQuant = getChromaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				coefficients[0][x][y] = (int)Math.round(coefficients[0][x][y] / (double)chromaQuant[x][y]);
				coefficients[1][x][y] = (int)Math.round(coefficients[1][x][y] / (double)chromaQuant[x][y]);
			}
		}
	}
	
	public void quantizeLumaDCTCoefficients(double[][] coefficients, final int size) {
		int[][] lumaQuant = getLumaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				coefficients[x][y] = (int)Math.round(coefficients[x][y] / (double)lumaQuant[x][y]);
			}
		}
	}
	
	public void dequantizeChromaDCTCoefficients(double[][][] coefficients, final int size) {
		int[][] chromaQuant = getChromaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				coefficients[0][x][y] *= chromaQuant[x][y];
				coefficients[1][x][y] *= chromaQuant[x][y];
			}
		}
	}
	
	public void dequantizeLumaDCTCoefficients(double[][] coefficients, final int size) {
		int[][] lumaQuant = getLumaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				coefficients[x][y] *= (double)lumaQuant[x][y];
			}
		}
	}
	
	private int[][] getLumaQuantizationTable(final int size) {
		switch (size) {
		case 8: return config.QUANTIZATION_MATRIX_8x8_Luma;
		case 4: return config.QUANTIZATION_MATRIX_4x4_Luma;
		default: throw new IllegalArgumentException("Unsupported matrix size: " + size);
		}
	}
	
	private int[][] getChromaQuantizationTable(final int size) {
		switch (size) {
		case 4: return config.QUANTIZATION_MATRIX_4x4_Chroma;
		case 2: return config.QUANTIZATION_MATRIX_2x2_Chroma;
		default: throw new IllegalArgumentException("Unsupported matrix size: " + size);
		}
	}
}
