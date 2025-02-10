/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2025  Lukas Nian En Lampl, Hans Lampl

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

package app.engines.dct;

import app.config;
import app.engines.dct.fct.FCT;
import app.engines.dct.fct.FCT2x2;
import app.engines.dct.fct.FCT4x4;
import app.engines.dct.fct.FCT8x8;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.MathUtils;

/**
 * <p>The class {@code DCTEngine} contains basic functions 
 * for calculating the DCT-II coefficients for a 8x8, 4x4 and 2x2
 * 2 dimensional double array. It also contains functions for quantization.
 * The used DCTs are the DCT-II and DCT-III (referred as IDCT).</p>
 * 
 * <p><b>Performance warning:</b> Due to the nature of the DCT-II
 * there are many repeated calculations and so impact performance
 * if used in a big scale.</p>
 * 
 * <p><b>Structure of DCT coefficients:</b><br>
 * <b>1. 4x4 blocks</b> are the most primitive unit to convert in the
 * DCT conversion task. Another advantage is that 4x4 coefficients are
 * pretty fast to calculate since the computation only has to calculate
 * {@code (4 * 4) + 2 * (2 * 2)} coefficients and thus can be stored in
 * their natural form. By natural form I refer to a 3D array with the
 * size 4x4 at [0], 2x2 at [1] and 2x2 at [2].<br><br>
 * 
 * <b>2. 8x8 blocks</b> are just like 4x4 blocks, but take a little more
 * computations ({@code (8 * 8) + 2 * (4 * 4)} coefficients) and thus more
 * time. Those coefficients are also stored in their nature form in an 3D
 * array with size 8x8 at [0], 4x4 at [1] and 4x4 at [2].<br><br>
 * 
 * <b>3. nxn blocks, where n > 8</b> are much more complex. In YAVC there
 * are a limited amount of these: 16x16, 32x32, 64x64 and 128x128. These
 * sizes are determistic by their property of being dividable by 8. Due
 * to the nature of DCT the larger a block, the more complex the calulation
 * of a single coefficient and the higher the time consumption. To avoid
 * this the YAVC DCTEngine applies a trick, it splits the blocks to 8x8
 * sized subblocks. Those are then encoded individually and put back into
 * the full array in this order: {@code Top-Left to Bottom-Right}. This means
 * if the block would be 16x16 the 3D array would be like this: 16x16 at [0],
 * 8x8 at [1] and 8x8 at [2]. Now the block is split into four 8x8 blocks from
 * Top-Left to Bottom-Right, those are processed individually and finally
 * added back. This means the processed block at [0] is at {@code 3D[0][0]}, while
 * the [1] block is at {@code 3D[8][0]} and the [2] block at {@code 3D[0][8]}.
 * This process applies to all other sizes as well.
 * </p>
 * 
 * @author Lukas Lampl
 * @since 1.1.0
 * 
 * @see app.engines.dct.fct.FCT2x2 FCT2x2
 * @see app.engines.dct.fct.FCT4x4 FCT4x4
 * @see app.engines.dct.fct.FCT8x8 FCT8x8
 */
public class DCTEngine {
	/**
	 * The Fast Cosine Transform for 2x2 matrices.
	 */
	private static FCT FCT2x2 = new FCT2x2();
	
	/**
	 * The Fast Cosine Transform for 4x4 matrices.
	 */
	private static FCT FCT4x4 = new FCT4x4();
	
	/**
	 * The Fast Cosine Transform for 8x8 matrices.
	 */
	private static FCT FCT8x8 = new FCT8x8();
	
	/**
	 * <p>Computes the DCT Coefficients of the color difference
	 * received by a {@code Component2D}. First the
	 * coefficients are calculated and then they're quantified.</p>
	 * 
	 * <p><b>IMPORTANT:</b><br> This function only calculates the DCT coefficients
	 * for 8x8, 4x4 and 2x2 matrices. If a matrix exceeds that size, then the matrix is
	 * split into 8x8 matrices and sorted from Left-to-Right and Top-to-Bottom.</p>
	 * 
	 * @return 3D subsampled double array containing all 8x8 or 4x4 matrices in the
	 * order Top-Left to Bottom-Right.
	 * 
	 * @param deltas			Deltas to convert.
	 * @param size				Size of the matrix to process.
	 * @param quantizize		Flag for whether the quantization process should take place or not.
	 * @param modifyExisting	Flag for whether the input array is allowed to be modified.
	 */
	public double[][][] computeDCTOfDeltas(final double[][][] deltas, final int size, final boolean quantizize,
			final boolean modifyExisting) {
		double[][][] coeffs = deltas;
		
		if (!modifyExisting) {
			coeffs = ArrayUtils.get3DArray(size, true);
			ArrayUtils.copy3DArray(deltas, 0, 0, 0, coeffs, 0, 0, 0, size, size, ColorManager.CHANNELS, true);
		}

		if (size == 4) {
			FCT4x4.fct2D(coeffs[ColorManager.Y_INDEX], 0, 0);
			FCT2x2.fct2D(coeffs[ColorManager.U_INDEX], 0, 0);
			FCT2x2.fct2D(coeffs[ColorManager.V_INDEX], 0, 0);
			
			if (quantizize) {
				quantizeChromaDCTCoefficients(coeffs, 2, 0, 0);
				quantizeLumaDCTCoefficients(coeffs[ColorManager.Y_INDEX], 4, 0, 0);
			}
			
			return coeffs;
		}
		
		for (int x = 0, halfX = 0; x < size; x += 8, halfX += 4) {
			for (int y = 0, halfY = 0; y < size; y += 8, halfY += 4) {
				FCT8x8.fct2D(coeffs[ColorManager.Y_INDEX], x, y);
				FCT4x4.fct2D(coeffs[ColorManager.U_INDEX], halfX, halfY);
				FCT4x4.fct2D(coeffs[ColorManager.V_INDEX], halfX, halfY);

				if (quantizize) {
					quantizeChromaDCTCoefficients(coeffs, 4, halfX, halfY);
					quantizeLumaDCTCoefficients(coeffs[ColorManager.Y_INDEX], 8, x, y);
				}
			}
		}
		
		return coeffs;
	}
	
	/**
	 * <p>Computes the IDCT Coefficients of the DCT-II coefficients
	 * received by a {@code Component2D}.
	 * First the coefficients are dequantizized and then further processed.</p>
	 * 
	 * @return Reconstructed absolute color difference array.
	 * 
	 * @param deltas		Coefficients to reverse. The order is from Top-Left to Bottom-Right.
	 * @param size			Size of the original matrix.
	 * @param quantizize	Flag for whether the coefficients were quantizized or not.
	 * @param modifyExisting	Flag for whether the input array is allowed to be modified.
	 */
	public double[][][] computeIDCTOfDeltas(final double[][][] deltas, final int size, final boolean quantizize,
			final boolean modifyExisting) {
		double[][][] coeffs = deltas;
		
		if (!modifyExisting) {
			coeffs = ArrayUtils.get3DArray(size, true);
			ArrayUtils.copy3DArray(deltas, 0, 0, 0, coeffs, 0, 0, 0, size, size, ColorManager.CHANNELS, true);
		}
		
		if (size == 4) {
			if (quantizize) {
				dequantizeChromaDCTCoefficients(coeffs, 2, 0, 0);
				dequantizeLumaDCTCoefficients(coeffs[ColorManager.Y_INDEX], 4, 0, 0);
			}
			
			FCT4x4.ifct2D(coeffs[ColorManager.Y_INDEX], 0, 0);
			FCT2x2.ifct2D(coeffs[ColorManager.U_INDEX], 0, 0);
			FCT2x2.ifct2D(coeffs[ColorManager.V_INDEX], 0, 0);
			return coeffs;
		}
		
		for (int x = 0, halfX = 0; x < size; x += 8, halfX += 4) {
			for (int y = 0, halfY = 0; y < size; y += 8, halfY += 4) {
				if (quantizize) {
					dequantizeChromaDCTCoefficients(coeffs, 4, halfX, halfY);
					dequantizeLumaDCTCoefficients(coeffs[ColorManager.Y_INDEX], 8, x, y);
				}
				
				FCT8x8.ifct2D(coeffs[ColorManager.Y_INDEX], x, y);
				FCT4x4.ifct2D(coeffs[ColorManager.U_INDEX], halfX, halfY);
				FCT4x4.ifct2D(coeffs[ColorManager.V_INDEX], halfX, halfY);
			}
		}
		
		return coeffs;
	}
	
	
	/**
	 * <p>Quantifies all chroma DCT-II coefficients with the matching
	 * quantization table.</p>
	 * 
	 * @see app.config
	 * 
	 * @param coefficients	Coefficients to quantify.
	 * @param size			Size of the matrix.
	 */
	public void quantizeChromaDCTCoefficients(double[][][] coefficients, int size, final int offsetX, final int offsetY) {
		int[][] chromaQuant = getChromaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			final int actualX = x + offsetX;
			
			for (int y = 0; y < size; y++) {
				final int actualY = y + offsetY;
				coefficients[ColorManager.U_INDEX][actualX][actualY] =
						(int)MathUtils.round(coefficients[ColorManager.U_INDEX][actualX][actualY]
								/ (double)chromaQuant[x][y]);
				coefficients[ColorManager.V_INDEX][actualX][actualY] =
						(int)MathUtils.round(coefficients[ColorManager.V_INDEX][actualX][actualY]
								/ (double)chromaQuant[x][y]);
			}
		}
	}
	
	/**
	 * <p>Quantifies all luma DCT-II coefficients with the matching
	 * quantization table.</p>
	 * 
	 * @see app.config
	 * 
	 * @param coefficients	Coefficients to quantify.
	 * @param size			Size of the matrix.
	 */
	public void quantizeLumaDCTCoefficients(double[][] coefficients, int size, final int offsetX, final int offsetY) {
		int[][] lumaQuant = getLumaQuantizationTable(size);
		
		for (int x = 0; x < size; x++) {
			final int actualX = x + offsetX;
			
			for (int y = 0; y < size; y++) {
				final int actualY = y + offsetY;
				coefficients[actualX][actualY] = (int)MathUtils.round(coefficients[actualX][actualY]
						/ (double)lumaQuant[x][y]);
			}
		}
	}
	
	/**
	 * <p>Dequantizizes all chroma DCT-II coefficients with the matching
	 * quantization table.</p>
	 * 
	 * @see app.config
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
				coefficients[ColorManager.U_INDEX][actualX][actualY] *= (double)chromaQuant[x][y];
				coefficients[ColorManager.V_INDEX][actualX][actualY] *= (double)chromaQuant[x][y];
			}
		}
	}
	
	/**
	 * <p>Dequantizizes all luma DCT-II coefficients with the matching
	 * quantization table.</p>
	 * 
	 * @see app.config
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
	 * <p>Returns the matching luma quantization table based on the given size.</p>
	 *
	 * @return A quantization table with a maximum size of 8x8.
	 * 
	 * @param size	Size of the matrix for which to get the table for.
	 * 
	 * @throws IllegalArgumentException	When the matrix size is not supported.
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
	 * <p>Returns the matching chroma quantization table based on the given size.</p>
	 *
	 * @return A quantization table with a maximum size of 4x4.
	 * 
	 * @param size	Size of the matrix for which to get the table for.
	 * 
	 * @throws IllegalArgumentException	When the matrix size is not supported.
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
