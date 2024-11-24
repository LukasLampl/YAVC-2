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

package app.dct;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import app.config;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.MathUtils;

/**
 * <p>The class {@code DCTEngine} contains basic functions 
 * for calcualting the DCT-II coefficients for a 8x8, 4x4 and 2x2
 * 2 dimensional double array. IT also contains functions for quantization.
 * The used DCTs are the DCT-II and DCT-III (referred as IDCT).</p>
 * 
 * <p><strong>Performance warning:</strong> Due to the nature of the DCT-II
 * there are many repeated calculations and so impact performance
 * if used in a big scale.</p>
 * 
 * @author Lukas Lampl
 * @since 17.0
 * @version 1.0 31 May 2024
 */

public class DCTEngine {
	/**
	 * Holds precalculated step factors for the "content" of the DCT.
	 * 
	 * <p><b>Reference</b><br>
	 * <a>https://www.mathworks.com/help/images/discrete-cosine-transform.html</a> (Called at 10.11.2024).
	 * </p>
	 */
	private static double[] STEP_X_EQUALS_ZERO = new double[] { 
			1.0 / Math.sqrt(2.0),
			1.0 / Math.sqrt(4.0),
			1.0 / Math.sqrt(8.0),
			1.0 / Math.sqrt(16.0),
			1.0 / Math.sqrt(32.0),
			1.0 / Math.sqrt(64.0),
			1.0 / Math.sqrt(128.0)
	};
	
	/**
	 * Holds precalculated step factors for the "boundaries" of the DCT.
	 * 
	 * <p><b>Reference</b><br>
	 * <a>https://www.mathworks.com/help/images/discrete-cosine-transform.html</a> (Called at 10.11.2024).
	 * </p>
	 */
	private static double[] STEP_X_NOT_EQUALS_ZERO = new double[] {
			Math.sqrt(2.0 / 2.0),
			Math.sqrt(2.0 / 4.0),
			Math.sqrt(2.0 / 8.0),
			Math.sqrt(2.0 / 16.0),
			Math.sqrt(2.0 / 32.0),
			Math.sqrt(2.0 / 64.0),
			Math.sqrt(2.0 / 128.0)
	};

	/**
	 * <p>This array stores the pre-calculated cosines to
	 * ensure a shorter calculation time in further processing.
	 * The order is like this: 8x8 at [0]; 4x4 at [1] and 2x2 at [2].
	 * The rest stores the individual
	 * coefficients.</p>
	 */
	public static double[][][][][] DCT_COEFFICIENTS = null;
	public static double[][][][][] IDCT_COEFFICIENTS = null;

	/**
	 * <p>The constructor pre-calculates all cosine values
	 * to ensure a faster processing time in the next few steps
	 * of the DCT-II as well as the IDCT.</p>
	 * 
	 * <p><strong>Performance Warning:</strong><br>
	 * Even though there is
	 * multithreading involved, the process takes up some time
	 * especially for larger DCT matrices.</p>
	 */
	public DCTEngine() {
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		try {
			//Sizes that are used for the DCT in YAVC
			int[] sizes = {8, 4, 2};
			DCT_COEFFICIENTS = new double[sizes.length][][][][];
			IDCT_COEFFICIENTS = new double[sizes.length][][][][];
			
			for (int i = 0; i < sizes.length; i++) {
				int m = sizes[i];
				DCT_COEFFICIENTS[i] = new double[m][m][m][m];
				IDCT_COEFFICIENTS[i] = new double[m][m][m][m];
				
				executor.submit(getDCTCoeffs(m, i));
			}
			
			executor.shutdown();
			while (!executor.awaitTermination(250, TimeUnit.MICROSECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * <p>Creates a runnable that is then passed
	 * to run in a thread. The runnable contains the functionality
	 * to calculate all DCT coefficients for DCT-II as well as IDCT.</p>
	 * @see <a>https://en.wikipedia.org/wiki/JPEG#Discrete_cosine_transform</a> (Called at 29.05.2024)
	 * 
	 * @return Runnable with the calculations ready to be run
	 * 
	 * @param m	size of the matrix it should represent
	 * @param index	Position in the array.
	 * For 8x8 it is at position [0], 4x4 at [1] and 2x2 at [2].
	 * 
	 * @throws IllegalArgumentException	when m is not positive or is 0
	 */
	private Runnable getDCTCoeffs(int m, int index) {
		if (m <= 0) {
			throw new IllegalArgumentException("Size m has to be greater than 0");
		}
		
		int m2 = m * 2;
		
		Runnable task = () -> {
			for (int v = 0; v < m; v++) {
				for (int u = 0; u < m; u++) {
					for (int x = 0; x < m; x++) {
						double cos1 = Math.cos(((double)(2 * x + 1) * (double)v * Math.PI) / m2);
						
						for (int y = 0; y < m; y++) {
							double cos2 = Math.cos(((double)(2 * y + 1) * (double)u * Math.PI) / m2);
							double cos = cos1 * cos2;
							DCT_COEFFICIENTS[index][v][u][x][y] = cos;
							IDCT_COEFFICIENTS[index][x][y][v][u] = cos;
						}
					}
				}
			}
		};
		
		return task;
	}
	
	/**
	 * <p>Returns the factor with which the individual coefficients
	 * should be multiplied with</p>
	 * 
	 * @return Factor with which the coefficient should be multiplied
	 * with
	 * 
	 * @param x	position of the coefficient in one dimension
	 * @param m	size of the coefficient matrix
	 */
	protected double step(int x, int m) {
		int i;
		
		switch (m) {
		case 2:
			i = 0; 
			break;
		case 4:
			i = 1;
			break;
		case 8:
			i = 2;
			break;
		case 16:
			i = 3;
			break;
		case 32:
			i = 4;
			break;
		case 64:
			i = 5;
			break;
		case 128:
			i = 6;
			break;
		default:
			throw new IllegalArgumentException("Illegal size " + m + ". Must be a power of 2 (from 2 to 128).");
		}
		return x == 0 ? STEP_X_EQUALS_ZERO[i] : STEP_X_NOT_EQUALS_ZERO[i];
	}
	
	/**
	 * <p>Returns the index of the coefficient matrix based
	 * on the matrix size.</p>
	 * 
	 * @return Index of the matrix
	 * 
	 * @param m	size of the matrix
	 * 
	 * @throws IllegalArgumentException	if the matrix size is not
	 * 8x8, 4x4 or 2x2
	 */
	private int setIndexOfDCT(int m) {
		switch (m) {
		case 8:
			return 0;
		case 4:
			return 1;
		case 2:
			return 2;
		default:
			throw new IllegalArgumentException("Unsupported matrix size: " + m);
		}
	}
	
	/**
	 * <p>Computes the DCT Coefficients of the absolute color difference
	 * received by a vector {@link app.interprediction.T} First the coefficients are
	 * calculated and then they're quantified.</p>
	 * 
	 * <p><strong>IMPORTANT:</strong><br> This function only calculates the DCT coefficients
	 * for 8x8, 4x4 and 2x2 matrices. If a matrix exceeds that size, then the matrix is
	 * split into 8x8 matrices and sorted from Left-to-Right and Top-to-Bottom.</p>
	 * 
	 * @return ArrayList containing all 8x8 or 4x4 matrices.
	 * For the order see above.
	 * 
	 * @param diffs	AbsoluteColorDifference from vector
	 * @param size	Size of the matrix to process
	 */
	public double[][][] computeDCTOfVectorColorDifference(double[][][] diffs, int size, boolean quantizize) {
		double[][][] coeffs = new double[ColorManager.CHANNELS][][];

		if (size == 4) {
			double[][][] chromaDCT = computeChromaDCTCoefficients(diffs[1], diffs[2], 2, 0, 0);
			double[][] lumaDCT = computeLumaDCTCoefficients(diffs[0], 4, 0, 0);
			
			if (quantizize) {
				quantizeChromaDCTCoefficients(chromaDCT, 2, 0, 0);
				quantizeLumaDCTCoefficients(lumaDCT, 4, 0, 0);
			}
			
			coeffs[DCTConstants.Y_COEFFS_INDEX] = lumaDCT;
			coeffs[DCTConstants.U_COEFFS_INDEX] = chromaDCT[0];
			coeffs[DCTConstants.V_COEFFS_INDEX] = chromaDCT[1];
			return coeffs;
		}

		coeffs = ArrayUtils.get3DArray(size, true);
		
		for (int x = 0, halfX = 0; x < size; x += 8, halfX += 4) {
			for (int y = 0, halfY = 0; y < size; y += 8, halfY += 4) {
				double[][][] chromaDCT = computeChromaDCTCoefficients(diffs[1], diffs[2], 4, halfX, halfY);
				double[][] lumaDCT = computeLumaDCTCoefficients(diffs[0], 8, x, y);
				
				if (quantizize) {
					quantizeChromaDCTCoefficients(chromaDCT, 4, 0, 0);
					quantizeLumaDCTCoefficients(lumaDCT, 8, 0, 0);
				}
				
				ArrayUtils.copy2DArray(lumaDCT, 0, 0, coeffs[DCTConstants.Y_COEFFS_INDEX], x, y, 8, 8);
				ArrayUtils.copy2DArray(chromaDCT[0], 0, 0, coeffs[DCTConstants.U_COEFFS_INDEX], halfX, halfY, 4, 4);
				ArrayUtils.copy2DArray(chromaDCT[1], 0, 0, coeffs[DCTConstants.V_COEFFS_INDEX], halfX, halfY, 4, 4);
			}
		}

		return coeffs;
	}
	
	/**
	 * <p>Computes the IDCT Coefficients of the DCT-II coefficients
	 * received by the converted AbsoluteColorDifference {@link app.interprediction.T}.
	 * First the coefficients are dequantizized and then further processed.</p>
	 * 
	 * @return Reconstructed AbsoluteColorDifference array
	 * 
	 * @param DCTCoeff	Coefficients to reverse.
	 * For order see {@code encoder.DCTEngine.computeDCTOfVectorColorDifference()}.
	 * @param size	Size of the original matrix
	 */
	public double[][][] computeIDCTOfVectorColorDifference(double[][][] DCTCoeff, int size, boolean quantizize) {
		if (DCTCoeff == null) {
			System.err.println("No DCT-II Coefficients to apply IDCT-II on! > NULL");
			return null;
		}

		if (size == 4) {
			return compute4x4IDCT(DCTCoeff, quantizize);
		}
		
		int fraction = 8;
		int halfFraction = 4;
		int halfSize = size / 2;
		double[][][] chromaIDCT = new double[][][] {DCTCoeff[DCTConstants.U_COEFFS_INDEX], DCTCoeff[DCTConstants.V_COEFFS_INDEX]};
		double[][] lumaIDCT = DCTCoeff[DCTConstants.Y_COEFFS_INDEX];
		double[][][] res = new double[3][][];
		res[DCTConstants.Y_COEFFS_INDEX] = new double[size][size];
		res[DCTConstants.U_COEFFS_INDEX] = new double[halfSize][halfSize];
		res[DCTConstants.V_COEFFS_INDEX] = new double[halfSize][halfSize];

		for (int x = 0, halfX = 0; x < size; x += fraction, halfX += halfFraction) {
			for (int y = 0, halfY = 0; y < size; y += fraction, halfY += halfFraction) {
				if (quantizize) {
					dequantizeChromaDCTCoefficients(chromaIDCT, halfFraction, halfX, halfY);
					dequantizeLumaDCTCoefficients(lumaIDCT, fraction, x, y);
				}
				
				double[][][] chromaIDCTVals = computeChromaIDCTCoefficients(chromaIDCT[0], chromaIDCT[1], halfFraction, halfX, halfY);
				double[][] lumaIDCTVals = computeLumaIDCTCoefficients(lumaIDCT, fraction, x, y);
				ArrayUtils.copy2DArray(lumaIDCTVals, 0, 0, res[DCTConstants.Y_COEFFS_INDEX], x, y, fraction, fraction);
				ArrayUtils.copy2DArray(chromaIDCTVals[0], 0, 0, res[DCTConstants.U_COEFFS_INDEX], halfX, halfY, halfFraction, halfFraction);
				ArrayUtils.copy2DArray(chromaIDCTVals[1], 0, 0, res[DCTConstants.V_COEFFS_INDEX], halfX, halfY, halfFraction, halfFraction);
			}
		}
		
		return res;
	}
	
	private double[][][] compute4x4IDCT(double[][][] DCTCoeff, boolean quantizize) {
		double[][][] res = new double[3][][];
		double[][][] chromaIDCT = new double[][][] {DCTCoeff[DCTConstants.U_COEFFS_INDEX], DCTCoeff[DCTConstants.V_COEFFS_INDEX]};
		double[][] lumaIDCT = DCTCoeff[DCTConstants.Y_COEFFS_INDEX];
		
		if (quantizize) {
			dequantizeChromaDCTCoefficients(chromaIDCT, 2, 0, 0);
			dequantizeLumaDCTCoefficients(lumaIDCT, 4, 0, 0);
		}
		
		double[][][] chromaIDCTVals = computeChromaIDCTCoefficients(chromaIDCT[0], chromaIDCT[1], 2, 0, 0);
		double[][] lumaIDCTVals = computeLumaIDCTCoefficients(lumaIDCT, 4, 0, 0);
		res[0] = lumaIDCTVals;
		res[1] = chromaIDCTVals[0];
		res[2] = chromaIDCTVals[1];
		return res;
	}

	/**
	 * <p>Computes the DCT-II coefficients for the chroma channels.
	 * Important to know is, that the chroma is subsampled unlike the
	 * luma channel.</p>
	 * 
	 * @return Array with the DCT-II coefficients,
	 * where U is at [0] and V at [1].
	 * 
	 * @see <a>https://en.wikipedia.org/wiki/JPEG#Discrete_cosine_transform</a> (Called at 29.05.2024)
	 * 
	 * @param U	U values to convert
	 * @param V	V values to convert
	 * @param m	size of the matrix
	 */
	protected double[][][] computeChromaDCTCoefficients(double[][] U, double[][] V, int m, final int offsetX, final int offsetY) {
		double resU[][] = new double[m][m];
		double resV[][] = new double[m][m];
		int index = setIndexOfDCT(m);
		double[] steps = {step(0, m), step(1, m)};
		
		for (int v = 0; v < m; v++) {
			for (int u = 0; u < m; u++) {
				double sumU = 0;
				double sumV = 0;

				for (int x = 0; x < m; x++) {
					final int actualX = x + offsetX;
					
					for (int y = 0; y < m; y++) {
						final int actualY = y + offsetY;
						double cos = DCT_COEFFICIENTS[index][v][u][x][y];
						sumU += (U[actualX][actualY] - 128) * cos;
						sumV += (V[actualX][actualY] - 128) * cos;
					}
				}
				
				double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
				
				resU[v][u] = step * sumU;
				resV[v][u] = step * sumV;
			}
		}
		
		return new double[][][] {resU, resV};
	}
	
	/**
	 * <p>Computes the IDCT coefficients for the DCT-II
	 * coefficients.</p>
	 * 
	 * @return Array with the IDCT coefficients,
	 * where U is at [0] and V at [1].
	 * 
	 * @see <a>https://en.wikipedia.org/wiki/JPEG#Discrete_cosine_transform</a> (Called at 29.05.2024)
	 * 
	 * @param U	DCT-II U values to convert
	 * @param V	DCT-II V values to convert
	 * @param m	size of the matrix
	 */
	private double[][][] computeChromaIDCTCoefficients(double[][] U, double[][] V, int m, final int offsetX, final int offsetY) {
		double[][] resU = new double[m][m];
		double[][] resV = new double[m][m];
		double[] steps = {step(0, m), step(1, m)};
		int index = setIndexOfDCT(m);
		
		for (int x = 0; x < m; x++) {
			for (int y = 0; y < m; y++) {
				double sumU = 0;
				double sumV = 0;
				
				for (int u = 0; u < m; u++) {
					final int actualU = u + offsetX;
					
					for (int v = 0; v < m; v++) {
						final int actualV = v + offsetY;
						double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
						double cos = IDCT_COEFFICIENTS[index][x][y][u][v];
						sumU += U[actualU][actualV] * step * cos;
						sumV += V[actualU][actualV] * step * cos;
					}
				}
				
				resU[x][y] = sumU + 128;
				resV[x][y] = sumV + 128;
			}
		}
		
		return new double[][][] {resU, resV};
	}
		
	/**
	 * <p>Computes the DCT-II coefficients for the luma channel.
	 * Important to know is, that the luma is not subsampled unlike the
	 * chroma channels.</p>
	 * 
	 * @return double[][] => Array with the DCT-II coefficients
	 * 
	 * @see <a>https://en.wikipedia.org/wiki/JPEG#Discrete_cosine_transform</a> (Called at 29.05.2024)
	 * 
	 * @param double[][] Y => Y values to convert
	 * @param int m => size of the matrix
	 */
	protected double[][] computeLumaDCTCoefficients(double[][] Y, int m, final int offsetX, final int offsetY) {
		double resY[][] = new double[m][m];
		int index = setIndexOfDCT(m);
		double[] steps = {step(0, m), step(1, m)};
		
		for (int v = 0; v < m; v++) {
			for (int u = 0; u < m; u++) {
				double sum = 0;
				
				for (int x = 0; x < m; x++) {
					final int actualX = x + offsetX;
					
					for (int y = 0; y < m; y++) {
						final int actualY = y + offsetY;
						double cos = DCT_COEFFICIENTS[index][v][u][x][y];
						sum += (Y[actualX][actualY] - 128) * cos;
					}
				}
				
				double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
				resY[v][u] = step * sum;
			}
		}
		
		return resY;
	}
	
	/**
	 * <p>Computes the IDCT coefficients for the DCT-II
	 * coefficients.</p>
	 * 
	 * @return Array with the IDCT coefficients
	 * 
	 * @see <a>https://en.wikipedia.org/wiki/JPEG#Discrete_cosine_transform</a> (Called at 29.05.2024)
	 * 
	 * @param Y	DCT-II Y values to convert
	 * @param m	size of the matrix
	 */
	private double[][] computeLumaIDCTCoefficients(double[][] Y, int m, final int offsetX, final int offsetY) {
		double[][] resY = new double[m][m];
		double[] steps = {step(0, m), step(1, m)};
		int index = setIndexOfDCT(m);
		
		for (int x = 0; x < m; x++) {
			for (int y = 0; y < m; y++) {
				double sum = 0;
				
				for (int u = 0; u < m; u++) {
					final int actualU = u + offsetX;
					
					for (int v = 0; v < m; v++) {
						final int actualV = v + offsetY;
						double step = (u == 0 ? steps[0] : steps[1]) * (v == 0 ? steps[0] : steps[1]);
						double cos = IDCT_COEFFICIENTS[index][x][y][u][v];
						sum += Y[actualU][actualV] * step * cos;
					}
				}
				
				resY[x][y] = sum + 128;
			}
		}
		
		return resY;
	}
	
	/**
	 * <p>Quantifies all chroma DCT-II coefficients with the according
	 * quantization table.</p>
	 * 
	 * @see app.io.Protocol
	 * 
	 * @param coefficients	Coefficients to quantify
	 * @param size	size of the matrix
	 */
	public void quantizeChromaDCTCoefficients(double[][][] coefficients, int size, final int offsetX, final int offsetY) {
		int[][] chromaQuant = getChromaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			final int actualX = x + offsetX;
			
			for (int y = 0; y < size; y++) {
				final int actualY = y + offsetY;
				coefficients[0][actualX][actualY] = (int)MathUtils.round(coefficients[0][actualX][actualY] / (double)chromaQuant[x][y]);
				coefficients[1][actualX][actualY] = (int)MathUtils.round(coefficients[1][actualX][actualY] / (double)chromaQuant[x][y]);
			}
		}
	}
	
	/**
	 * <p>Quantifies all luma DCT-II coefficients with the according
	 * quantization table.</p>
	 * 
	 * @see app.io.Protocol
	 * 
	 * @param coefficients	Coefficients to quantify
	 * @param size	size of the matrix
	 */
	public void quantizeLumaDCTCoefficients(double[][] coefficients, int size, final int offsetX, final int offsetY) {
		int[][] lumaQuant = getLumaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			final int actualX = x + offsetX;
			
			for (int y = 0; y < size; y++) {
				final int actualY = y + offsetY;
				coefficients[actualX][actualY] = (int)MathUtils.round(coefficients[actualX][actualY] / (double)lumaQuant[x][y]);
			}
		}
	}
	
	/**
	 * <p>Dequantizizes all chroma DCT-II coefficients with the according
	 * quantization table.</p>
	 * 
	 * @see app.io.Protocol
	 * 
	 * @param coefficients	Coefficients to dequantizize.
	 * @param size			Size of the matrix.
	 */
	public void dequantizeChromaDCTCoefficients(double[][][] coefficients, int size, final int offsetX, final int offsetY) {
		int[][] chromaQuant = getChromaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			final int actualX = x + offsetX;
			
			for (int y = 0; y < size; y++) {
				final int actualY = y + offsetY;
				coefficients[0][actualX][actualY] *= (double)chromaQuant[x][y];
				coefficients[1][actualX][actualY] *= (double)chromaQuant[x][y];
			}
		}
	}
	
	/**
	 * <p>Dequantizizes all luma DCT-II coefficients with the according
	 * quantization table.</p>
	 * 
	 * @see app.io.Protocol
	 * 
	 * @param coefficients	Coefficients to dequantizize.
	 * @param size			Size of the matrix.
	 */
	public void dequantizeLumaDCTCoefficients(double[][] coefficients, int size, final int offsetX, final int offsetY) {
		int[][] lumaQuant = getLumaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			final int actualX = x + offsetX;
			
			for (int y = 0; y < size; y++) {
				final int actualY = y + offsetY;
				coefficients[actualX][actualY] *= (double)lumaQuant[x][y];
			}
		}
	}
	
	/**
	 * <p>Returns the according luma quantization table.</p>
	 *
	 * @return Quantization table
	 * 
	 * @param size	size of the matrix
	 * 
	 * @throws IllegalArgumentException	when the matrix size is not supported
	 */
	private int[][] getLumaQuantizationTable(int size) {
		switch (size) {
		case 8:
			return config.QUANTIZATION_MATRIX_8x8_Luma;
		case 4:
			return config.QUANTIZATION_MATRIX_4x4_Luma;
		default:
			throw new IllegalArgumentException("Unsupported matrix size: " + size);
		}
	}
	
	/**
	 * <p>Returns the according chroma quantization table.</p>
	 *
	 * @return Quantization table
	 * 
	 * @param size	Size of the matrix
	 * 
	 * @throws IllegalArgumentException	when the matrix size is not supported
	 */
	private int[][] getChromaQuantizationTable(int size) {
		switch (size) {
		case 4:
			return config.QUANTIZATION_MATRIX_4x4_Chroma;
		case 2:
			return config.QUANTIZATION_MATRIX_2x2_Chroma;
		default:
			throw new IllegalArgumentException("Unsupported matrix size: " + size);
		}
	}
}
