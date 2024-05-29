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

package YAVC.Encoder;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import YAVC.Main.config;

/**
 * <p>The class {@code YAVC.Encoder.DCTEngine} contains basic functions 
 * for calcualting the DCT-II coefficients for a 8x8, 4x4 and 2x2
 * 2 dimensional double array. IT also contains functions for quantization.
 * The used DCTs are the DCT-II and DCT-III (referred as IDCT).</p>
 * 
 * <p><strong>Performance warning:</strong> Due to the nature of the DCT-II
 * there are many repeated calculations and so impact performance
 * if used in a big scale.</p>
 * 
 * @author Lukas Lampl
 * @since 1.0
 */

public class DCTEngine {
	/**
	 * <p>This array stores the pre-calculated cosines to
	 * ensure a shorter calculation time in further processing.
	 * The first dimension describes, whether the coefficients
	 * are DCT-II ([0]) or IDCT ([1]). Followed by that is the
	 * dimension of the matrix itself with 8x8 at [0], 4x4
	 * at [1] and 2x2 at [2]. The rest stores the individual
	 * coefficients.</p>
	 */
	public static double[][][][][][] DCT_COEFFICIENTS = new double[2][][][][][]; //Position at [0][][][]... is for DCT; Position at [1][][][]... is for IDCT

	/**
	 * <p>The constructor pre-calculates all cosine values
	 * to ensure a faster processing time in the next few steps
	 * of the DCT-II as well as the IDCT.</p>
	 * 
	 * <p><strong>Performance Warning:</strong> Even though there is
	 * multithreading involved, the process takes up some time
	 * especially for larger DCT matrices.</p>
	 */
	public DCTEngine() {
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		try {
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
			while (!executor.awaitTermination(20, TimeUnit.MICROSECONDS)) {}
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
		if (m <= 0) throw new IllegalArgumentException("Size m has to be greater than 0");
		
		int m2 = m * 2;
		
		Runnable task = () -> {
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
	private double step(int x, int m) {
		return x == 0 ? 1 / Math.sqrt(m) : Math.sqrt(2.0 / (double)m);
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
		case 8: return 0;
		case 4: return 1;
		case 2: return 2;
		default: throw new IllegalArgumentException("Unsupported matrix size: " + m);
		}
	}
	
	/**
	 * <p>Computes the DCT Coefficients of the absolute color difference
	 * received by a vector {@code YAVC.Utils.Vector}. First the coefficients are
	 * calculated and then they're quantified.</p>
	 * 
	 * <p><strong>IMPORTANT:</strong> this function only calculates the DCT coefficients
	 * for 8x8, 4x4 and 2x2 matrices. If a matrix exceeds that size, then the matrix is
	 * split into 8x8 matrices and sorted from Left-to-Right and Top-to-Bottom.</p>
	 * 
	 * @return ArrayList containing all 8x8 or 4x4 matrices.
	 * For the order see above.
	 * 
	 * @param diffs	AbsoluteColorDifference from vector
	 * @param size	Size of the matrix to process
	 */
	public ArrayList<double[][][]> computeDCTOfVectorColorDifference(double[][][] diffs, int size) {
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
	
	/**
	 * <p>Computes the IDCT Coefficients of the DCT-II coefficients
	 * received by the converted AbsoluteColorDifference {@code YAVC.Utils.Vector}.
	 * First the coefficients are dequantizized and then further processed.</p>
	 * 
	 * @return Reconstructed AbsoluteColorDifference array
	 * 
	 * @param DCTCoeff	Coefficients to reverse.
	 * For order see {@code YAVC.Encoder.DCTEngine.computeDCTOfVectorColorDifference()}.
	 * @param size	Size of the original matrix
	 */
	public double[][][] computeIDCTOfVectorColorDifference(ArrayList<double[][][]> DCTCoeff, int size) {
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
	
	/**
	 * <p>Writes a subarray into another array</p>
	 * 
	 * @param subArray	subarray to put into the array
	 * @param dest	destination to put the subarray into 
	 * @param posX	position x in the destination array
	 * @param posY	position y in the destination array
	 */
	private void writeSubArrayInArray(double[][][] subArray, double[][][] dest, int posX, int posY) {
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
	
	/**
	 * <p>Gets a subarray from an array.</p>
	 * 
	 * @return Created subarray from the original array
	 * 
	 * @param org	Original array to get the sub-array from
	 * @param size	size of the subarray
	 * @param posX	position x in the original array
	 * @param posY	position y in the original array
	 * 
	 * @throws ArrayIndexOutOfBoundsException	when the size is bigger than the
	 * original array or the subarray exceeds the original array
	 */
	private double[][][] getSubArray(double[][][] org, int size, int posX, int posY) {
		if (size > org[0].length) throw new ArrayIndexOutOfBoundsException("Size of the subarray can't be bigger than the original");
		else if (size + posX > org[0].length) throw new ArrayIndexOutOfBoundsException("Subarray exceeds the original array");
		else if (size + posY > org[0].length) throw new ArrayIndexOutOfBoundsException("Subarray exceeds the original array");
		
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
	private double[][][] computeChromaDCTCoefficients(double[][] U, double[][] V, int m) {
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
	private double[][][] computeChromaIDCTCoefficients(double[][] U, double[][] V, int m) {
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
	private double[][] computeLumaDCTCoefficients(double[][] Y, int m) {
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
	private double[][] computeLumaIDCTCoefficients(double[][] Y, int m) {
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
	
	/**
	 * <p>Quantifies all chroma DCT-II coefficients with the according
	 * quantization table.</p>
	 * 
	 * @see YAVC.Main.config
	 * 
	 * @param coefficients	Coefficients to quantify
	 * @param size	size of the matrix
	 */
	public void quantizeChromaDCTCoefficients(double[][][] coefficients, int size) {
		int[][] chromaQuant = getChromaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				coefficients[0][x][y] = (int)Math.round(coefficients[0][x][y] / (double)chromaQuant[x][y]);
				coefficients[1][x][y] = (int)Math.round(coefficients[1][x][y] / (double)chromaQuant[x][y]);
			}
		}
	}
	
	/**
	 * <p>Quantifies all luma DCT-II coefficients with the according
	 * quantization table.</p>
	 * 
	 * @see YAVC.Main.config
	 * 
	 * @param coefficients	Coefficients to quantify
	 * @param size	size of the matrix
	 */
	public void quantizeLumaDCTCoefficients(double[][] coefficients, int size) {
		int[][] lumaQuant = getLumaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				coefficients[x][y] = (int)Math.round(coefficients[x][y] / (double)lumaQuant[x][y]);
			}
		}
	}
	
	/**
	 * <p>Dequantizizes all chroma DCT-II coefficients with the according
	 * quantization table.</p>
	 * 
	 * @see YAVC.Main.config
	 * 
	 * @param coefficients	Coefficients to dequantizize
	 * @param size	size of the matrix
	 */
	public void dequantizeChromaDCTCoefficients(double[][][] coefficients, int size) {
		int[][] chromaQuant = getChromaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				coefficients[0][x][y] *= chromaQuant[x][y];
				coefficients[1][x][y] *= chromaQuant[x][y];
			}
		}
	}
	
	/**
	 * <p>Dequantizizes all luma DCT-II coefficients with the according
	 * quantization table.</p>
	 * 
	 * @see YAVC.Main.config
	 * 
	 * @param coefficients	Coefficients to dequantizize
	 * @param size	size of the matrix
	 */
	public void dequantizeLumaDCTCoefficients(double[][] coefficients, int size) {
		int[][] lumaQuant = getLumaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				coefficients[x][y] *= (double)lumaQuant[x][y];
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
		case 8: return config.QUANTIZATION_MATRIX_8x8_Luma;
		case 4: return config.QUANTIZATION_MATRIX_4x4_Luma;
		default: throw new IllegalArgumentException("Unsupported matrix size: " + size);
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
		case 4: return config.QUANTIZATION_MATRIX_4x4_Chroma;
		case 2: return config.QUANTIZATION_MATRIX_2x2_Chroma;
		default: throw new IllegalArgumentException("Unsupported matrix size: " + size);
		}
	}
}
