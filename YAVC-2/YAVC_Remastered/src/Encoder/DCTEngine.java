package Encoder;

import java.awt.Point;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Utils.PixelRaster;
import Utils.Vector;

public class DCTEngine {
	public static double[][][][][] DCT_COEFFICIENTS = new double[2][][][][]; //Position at [0][][][]... is for DCT; Position at [1][][][]... is for IDCT

	public void initDCTCoefficinets() {
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		int[] sizes = {128, 64, 32, 16, 8, 4, 2};
		DCT_COEFFICIENTS[0] = new double[sizes.length][][][];
		
		for (int i = 0; i < sizes.length; i++) {
			final int index = i;
			
			Callable<Void> task = () -> {
				int m = sizes[index];
				DCT_COEFFICIENTS[0][index] = new double[m][m][m * m];
				double m2 = m * 2;
				
				for (int v = 0; v < m; v++) {
		            for (int u = 0; u < m; u++) {
		            	for (int x = 0; x < m; x++) {
		                	double cos1 = Math.cos(((double)(2 * x + 1) * (double)v * Math.PI) / m2);
		                    for (int y = 0; y < m; y++) {
		                        double cos2 = Math.cos(((double)(2 * y + 1) * (double)u * Math.PI) / m2);
		                        DCT_COEFFICIENTS[0][index][v][u][x * m + y] = cos1 * cos2;
		                        DCT_COEFFICIENTS[1][index][x][y][u * m + v] = cos1 * cos2;
		                    }
		                }
		            }
		        }
				
				return null;
			};
			
			executor.submit(task);
		}
	}
	
	private double step(int x, int m) {
		return x == 0 ? 1 / Math.sqrt(m) : Math.sqrt(2.0 / (double)m);
	}
	
	public void applyDCTOnPixelRaster(PixelRaster raster) {
		if (raster == null) {
			System.err.println("No PixelRaster for 8x8 DCT-II > Skip process");
			return;
		}
		
		int size = 8, threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		int width = raster.getWidth(), height = raster.getHeight();
		
		for (int x = 0; x < width; x += size) {
			for (int y = 0; y < height; y += size) {
				final int posX = x, posY = y;
				
				Callable<Void> task = () -> {
					double[][][] comps = raster.getPixelBlock(new Point(posX, posY), size, null);
					double[][][] ChromaDCT = computeChromaDCTCoefficients(comps[1], comps[2], size / 2);
					double[][] LumaDCT = computeLumaDCTCoefficients(comps[0], size);
					
					quantizeChromaDCTCoefficients(ChromaDCT);
					quantizeLumaDCTCoefficients(LumaDCT);
					dequantizeChromaDCTCoefficients(ChromaDCT);
					dequantizeLumaDCTCoefficients(LumaDCT);
					
					ChromaDCT = computeChromaIDCTCoefficients(ChromaDCT[0], ChromaDCT[1], size / 2);
					LumaDCT = computeLumaIDCTCoefficients(LumaDCT, size);
					
					for (int u = 0; u < size; u++) {
						for (int v = 0; v < size; v++) {
							raster.setLuma(posX + u, posY + v, LumaDCT[u][v]);
						}
					}
					
					for (int u = 0; u < size / 2; u++) {
						for (int v = 0; v < size / 2; v++) {
							raster.setChroma((u * 2) + posX, (v * 2) + posY, ChromaDCT[0][u][v], ChromaDCT[1][u][v]);
						}
					}
					
					return null;
				};
				
				executor.submit(task);
			}
		}
		
		executor.shutdown();
	}
	
	public double[][][] computeDCTOfVectorColorDifference(Vector vector) {
		if (vector == null) {
			System.err.println("No Vector to apply DCT-II on! > NULL");
			return null;
		}
		
		double[][][] diffs = vector.getAbsoluteColorDifference();
		double[][][] chromaDCT = computeChromaDCTCoefficients(diffs[1], diffs[2], vector.getSize() / 2);
		double[][] lumaDCT = computeLumaDCTCoefficients(diffs[0], vector.getSize());
		return new double[][][] {lumaDCT, chromaDCT[0], chromaDCT[1]};
	}
	
	public double[][][] computeIDCTOfVectorColorDifference(double[][][] DCTCoeff, int size) {
		if (DCTCoeff == null) {
			System.err.println("No DCT-II Coefficients to apply IDCT-II on! > NULL");
			return null;
		}
		
		double[][][] chromaIDCT = computeChromaIDCTCoefficients(DCTCoeff[1], DCTCoeff[2], size / 2);
		double[][] lumaIDCT = computeLumaIDCTCoefficients(DCTCoeff[0], size);
		return new double[][][] {lumaIDCT, chromaIDCT[0], chromaIDCT[1]};
	}
	
	private int setIndexOfDCT(int m) {
		switch (m) {
		case 128: return 0;
		case 64: return 1;
		case 32: return 2;
		case 16: return 3;
		case 8: return 4;
		case 4: return 5;
		case 2: return 6;
		default: throw new IllegalArgumentException("Unsupported matrix size: " + m);
		}
	}
	
	private double[][][] computeChromaDCTCoefficients(double[][] U, double[][] V, int m) {
		double resU[][] = new double[m][m];
		double resV[][] = new double[m][m];
		int index = setIndexOfDCT(m);
		double[] steps = {step(0, m), step(1, m)};
		
		for (int v = 0; v < m; v++) {
            for (int u = 0; u < m; u++) {
            	double sumU = 0, sumV = 0;
            	double cos[] = DCT_COEFFICIENTS[0][index][v][u];
            	
                for (int x = 0; x < m; x++) {
                    for (int y = 0; y < m; y++) {
                    	int tLD = x * m + y;
                        sumU += (U[x][y] - 128) * cos[tLD];
                        sumV += (V[x][y] - 128) * cos[tLD];
                    }
                }
                
                double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
                resU[v][u] = Math.round(step * sumU);
                resV[v][u] = Math.round(step * sumV);
            }
        }
		
		return new double[][][] {resU, resV};
	}
	
	private double[][][] computeChromaIDCTCoefficients(double[][] U, double[][] V, int m) {
		double[][] resU = new double[m][m];
		double[][] resV = new double[m][m];
		double[] steps = {step(0, m), step(1, m)};
		int index = setIndexOfDCT(m);
		
		for (int x = 0; x < m; x++) {
			for (int y = 0; y < m; y++) {
				double sumU = 0, sumV = 0;
				double[] cos = DCT_COEFFICIENTS[1][index][x][y];
				
				for (int u = 0; u < m; u++) {
					for (int v = 0; v < m; v++) {
						double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
						int tLD = u * m + v;
                        sumU += U[u][v] * step * cos[tLD];;
                        sumV += V[u][v] * step * cos[tLD];;
					}
				}
				
				resU[x][y] = sumU + 128;
				resV[x][y] = sumV + 128;
			}
		}
		
		return new double[][][] {resU, resV};
	}
		
	private double[][] computeLumaDCTCoefficients(double[][] Y, int m) {
		double resY[][] = new double[m][m];
		int index = setIndexOfDCT(m);
		
		for (int v = 0; v < m; v++) {
            for (int u = 0; u < m; u++) {
            	double sum = 0;
            	double cos[] = DCT_COEFFICIENTS[0][index][v][u];
            	
                for (int x = 0; x < m; x++) {
                    for (int y = 0; y < m; y++) {
                        sum += (Y[x][y] - 128) * cos[x * m + y];
                    }
                }
                
                resY[v][u] = Math.round(step(v, m) * step(u, m) * sum);
            }
        }
		
		return resY;
	}
		
	private double[][] computeLumaIDCTCoefficients(double[][] Y, int m) {
		double[][] resY = new double[m][m];
		double[] steps = {step(0, m), step(1, m)};
		int index = setIndexOfDCT(m);
		
		for (int x = 0; x < m; x++) {
			for (int y = 0; y < m; y++) {
				double sum = 0;
				double[] cos = DCT_COEFFICIENTS[1][index][x][y];
				
				for (int u = 0; u < m; u++) {
					for (int v = 0; v < m; v++) {
						double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
                        sum += Y[u][v] * step * cos[u * m + v];
					}
				}
				
				resY[x][y] = sum + 128;
			}
		}
		
		return resY;
	}
	
	private static int[][] QUANTIZATION_MATRIX_8x8_Luma = {
			{16, 11, 10, 16, 24, 40, 51, 61},
			{12, 12, 14, 19, 26, 58, 60, 55},
			{14, 13, 16, 24, 40, 57, 69, 56},
			{14, 17, 22, 29, 51, 87, 80, 62},
			{18, 22, 37, 56, 68, 109, 103, 77},
			{24, 35, 55, 64, 81, 104, 113, 92},
			{49, 64, 78, 87, 103, 121, 120, 101},
			{72, 92, 95, 98, 112, 100, 103, 99}
	};
	
	private static int[][] QUANTIZATION_MATRIX_8x8_Chroma = {
			{17, 18, 24, 47, 99, 99, 99, 99},
			{18, 21, 26, 66, 99, 99, 99, 99},
			{24, 26, 56, 99, 99, 99, 99, 99},
			{47, 66, 99, 99, 99, 99, 99, 99},
			{99, 99, 99, 99, 99, 99, 99, 99},
			{99, 99, 99, 99, 99, 99, 99, 99},
			{99, 99, 99, 99, 99, 99, 99, 99},
			{99, 99, 99, 99, 99, 99, 99, 99}
	};
	
	private static int[][] QUANTIZATION_MATRIX_4x4_Luma = {
			{13, 15, 37, 57},
			{15, 29, 59, 67},
			{25, 53, 91, 96},
			{69, 90, 109, 106}
	};
	
	private static int[][] QUANTIZATION_MATRIX_4x4_Chroma = {
			{74, 41, 99, 99},
			{41, 88, 99, 99},
			{99, 99, 99, 99},
			{99, 99, 99, 99}
	};
	
	private static int[][] QUANTIZATION_MATRIX_2x2_Chroma = {
			{61, 99},
			{99, 99}
	};
	
	public void quantizeChromaDCTCoefficients(double[][][] coefficients) {
		int sizeChroma = coefficients[1].length;
		int[][] chromaQuant = getChromaQuantizationTable(sizeChroma);
		
		for (int x = 0; x < sizeChroma; x++) {
			for (int y = 0; y < sizeChroma; y++) {
				coefficients[1][x][y] = Math.round(coefficients[1][x][y] / (double)chromaQuant[x][y]);
				coefficients[2][x][y] = Math.round(coefficients[2][x][y] / (double)chromaQuant[x][y]);
			}
		}
	}
	
	public void quantizeLumaDCTCoefficients(double[][] coefficients) {
		int sizeLuma = coefficients[0].length;
		int[][] lumaQuant = getLumaQuantizationTable(sizeLuma);
		
		for (int x = 0; x < sizeLuma; x++) {
			for (int y = 0; y < sizeLuma; y++) {
				coefficients[x][y] = Math.round(coefficients[x][y] / (double)lumaQuant[x][y]);
			}
		}
	}
	
	public void dequantizeChromaDCTCoefficients(double[][][] coefficients) {
		int sizeChroma = coefficients[1].length;
		int[][] chromaQuant = getChromaQuantizationTable(sizeChroma);
		
		for (int x = 0; x < sizeChroma; x++) {
			for (int y = 0; y < sizeChroma; y++) {
				coefficients[1][x][y] *= chromaQuant[x][y];
				coefficients[2][x][y] *= chromaQuant[x][y];
			}
		}
	}
	
	public void dequantizeLumaDCTCoefficients(double[][] coefficients) {
		int sizeLuma = coefficients[0].length;
		int[][] lumaQuant = getLumaQuantizationTable(sizeLuma);
		
		for (int x = 0; x < sizeLuma; x++) {
			for (int y = 0; y < sizeLuma; y++) {
				coefficients[x][y] *= (double)lumaQuant[x][y];
			}
		}
	}
	
	private int[][] getLumaQuantizationTable(int size) {
		switch (size) {
		case 128:
		case 64:
		case 32:
		case 16: throw new IllegalArgumentException("Unsupported matrix size: " + size);
		case 8: return QUANTIZATION_MATRIX_8x8_Luma;
		case 4: return QUANTIZATION_MATRIX_4x4_Luma;
		default: throw new IllegalArgumentException("Unsupported matrix size: " + size);
		}
	}
	
	private int[][] getChromaQuantizationTable(int size) {
		switch (size) {
		case 128:
		case 64:
		case 32:
		case 16: throw new IllegalArgumentException("Unsupported matrix size: " + size);
		case 8: return QUANTIZATION_MATRIX_8x8_Chroma;
		case 4: return QUANTIZATION_MATRIX_4x4_Chroma;
		case 2: return QUANTIZATION_MATRIX_2x2_Chroma;
		default: throw new IllegalArgumentException("Unsupported matrix size: " + size);
		}
	}
}
