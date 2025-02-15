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

package app.io;

import app.ArgumentProcessor;
import app.engines.dct.DCTConstants;
import app.exceptions.DCTCoefficientOutOfBoundsException;
import app.io.coder.zigzag.ZigZagCoder;
import app.io.coder.zigzag.ZigZagCoder2x2;
import app.io.coder.zigzag.ZigZagCoder4x4;
import app.io.coder.zigzag.ZigZagCoder8x8;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;

public abstract class ProtocolBase {
	private static ZigZagCoder ZigZagCoder2x2 = new ZigZagCoder2x2();
	private static ZigZagCoder ZigZagCoder4x4 = new ZigZagCoder4x4();
	private static ZigZagCoder ZigZagCoder8x8 = new ZigZagCoder8x8();
	
	
	/**
	 * The size of an integer in bytes.
	 */
	public static final int SIZE_OF_INT = 4;
	
	/**
	 * The length of a size in bytes.
	 */
	public static final int SIZE_LENGTH = 3;
	
	/**
	 * Get the DCT coefficient in form of a byte.
	 * 
	 * @param coeff	The coefficient to convert.
	 * @return The byte representative of the coefficient.
	 */
	public static byte getDCTCoeffByte(final double coeff) {
		byte result = (byte)((int)Math.abs(coeff) & 0x7F);
		
		if (coeff < 0) {
			result |= (1 << 7);
		}
		
		return (byte)(result & 0xFF);
	}
	
	/**
	 * Get the DCT coefficient from a DCT coefficient byte.
	 * 
	 * @param coeff	The byte to convert back to the DCT coefficient.
	 * @return The converted coefficient.
	 */
	public static final double getDCTCoeff(final byte coeff) {
		double result = coeff & 0x7F;
		return (coeff & 0x80) != 0 ? -result : result;
	}
	
	/**
	 * Get the given position (single coordinate) in byte representation.
	 * 
	 * @param pos	The Position coordinate to convert.
	 * @return A byte array containing the position information.
	 * 
	 * @throws IllegalArgumentException	When the coordinate is below 0 or above 65536.
	 */
	public static byte[] getPositionBytes(final int pos) {
		if (pos > 65536) {
			throw new IllegalArgumentException("Position of vector exceeds maximum limit of 65536");
		} else if (pos < 0) {
			throw new IllegalArgumentException("Position of vector is smaller than 0 (out of frame)");
		}
		
		return new byte[] {(byte)((pos >> 8) & 0xFF), (byte)(pos & 0xFF)};
	}
	
	/**
	 * Converts the given bytes to the integer form of the position.
	 * 
	 * @param c1	Upper byte.
	 * @param c2	Lower byte.
	 * @return The reconstructed integer.
	 * 
	 * @see #getPositionBytes(int)
	 */
	public static int getPosition(final byte c1, final byte c2) {
		int res = (c1 & 0xFF) << 8 | (c2 & 0xFF);
		return res;
	}
	
	/**
	 * Converts a subsampled delta matrix to a byte matrix which can be then encoded.
	 * 
	 * <p><b>Process:</b><br>
	 * Since using a DCT on {@code n} sized blocks where {@code n > 8} is time consuming, a block that
	 * is bigger than 8x8 will be split.<br><br>
	 * 
	 * First a 4x4 and 8x8 block will be normally written with its 3 channels, first in a {@code n * n} long
	 * Y-channel then two {@code 2 * (size * size / 4)} channels.<br><br>
	 * 
	 * But if a block exceeds 8x8 it'll be spit into 8x8 blocks that are individually processed further.
	 * This means if we had a 16x16 block it'll be split into 4 8x8 blocks. Those will be written from
	 * Top-Left to Bottom-Right in each channel individually.<br><br>
	 * 
	 * If you want to perceive data, you'll have to get all 3 channels, then the offset of the Y-Channel,
	 * the U-Channel and the V-Channel. After that the Y-Component in the Y-Channel is as long as
	 * the block length ({@code n * n}). Now to get the U-Component you'll need to offset to the U start,
	 * which can be achieved by calculating {@code UStart = YStart + YLength}. The U and V components are
	 * approximately {@code (n / 2) * (n / 2)} or {@code (n * n) / 4} long. REMEMBER the layout is from Top-Left
	 * to Bottom-Right.
	 * </p>
	 * 
	 * @param deltaMatrix	The matrix with all deltas to get bytes from.
	 * @param size			The size of the matrix.
	 * @return A matrix representation of the absolute color difference.
	 * 
	 * @throws DCTCoefficientOutOfBoundsException When a DCT coefficient is out of bounds from -127 to 127.
	 * 
	 * @throws IllegalArgumentException	When a coefficient is > 127 or < -127 and {@code autoAdjust} is off.
	 */
	public static byte[][] getDeltaMatrixBytes(double[][][] deltaMatrix, final int size) throws DCTCoefficientOutOfBoundsException {
		final int halfSize = size >> 1;
		final int frac = size == 4 ? 4 : 8;
		final int groups = size == 4 ? 1 : (size * size) / 64;
		final byte[] YBytes = new byte[size * size];
		final byte[] UBytes = new byte[halfSize * halfSize];
		final byte[] VBytes = new byte[halfSize * halfSize];
		
		int YIndex = 0;
		int UIndex = 0;
		int VIndex = 0;
		
		for (int n = 0, xToAdd = 0, halfXToAdd = 0, yToAdd = 0, halfYToAdd = 0; n < groups; n++, xToAdd += 8, halfXToAdd += 4) {
			if (xToAdd >= size) {
				xToAdd = 0;
				halfXToAdd = 0;
				yToAdd += 8;
				halfYToAdd += 4;
			}
			
			byte[] YStream = null;
			byte[] UStream = null;
			byte[] VStream = null;
			
			if (frac == 4) {
				YStream = ZigZagCoder4x4.code(deltaMatrix[ColorManager.Y_INDEX], xToAdd, yToAdd);
				UStream = ZigZagCoder2x2.code(deltaMatrix[ColorManager.U_INDEX], halfXToAdd, halfYToAdd);
				VStream = ZigZagCoder2x2.code(deltaMatrix[ColorManager.V_INDEX], halfXToAdd, halfYToAdd);
			} else {
				YStream = ZigZagCoder8x8.code(deltaMatrix[ColorManager.Y_INDEX], xToAdd, yToAdd);
				UStream = ZigZagCoder4x4.code(deltaMatrix[ColorManager.U_INDEX], halfXToAdd, halfYToAdd);
				VStream = ZigZagCoder4x4.code(deltaMatrix[ColorManager.V_INDEX], halfXToAdd, halfYToAdd);
			}

			ArrayUtils.copyArray(YStream, 0, YBytes, YIndex, YStream.length);
			ArrayUtils.copyArray(UStream, 0, UBytes, UIndex, UStream.length);
			ArrayUtils.copyArray(VStream, 0, VBytes, VIndex, VStream.length);
			YIndex += YStream.length;
			UIndex += UStream.length;
			VIndex += VStream.length;
		}
		
		return new byte[][] {YBytes, UBytes, VBytes};
	}
	
	/**
	 * Gets an 3D double array that represent the DCT-II coefficients.
	 * 
	 * <p><b>Note:</b><br>
	 * The function gets one when the {@code size = 4}. When the {@code size > 4} the
	 * function iterates multiple times until the size is fully read in. The partial reading
	 * in is defined by the Protocol, which says that a n-sized block has 3 data channels for
	 * the 3 color channels, each composes of 8x8 (or subsampled 4x4) datablocks that are
	 * aligned in a row. In other terms:<br><br>
	 * 
	 * If I had an 16x16 sized DCT-II block data the Protocol will split it into these arrays:<br>
	 * - Y[16 * 16]<br>
	 * - U[8 * 8]<br>
	 * - V[8 * 8]<br><br>
	 * 
	 * After that It'll split the 16x16 block into 8x8 subblocks, for performance reasons in the DCT-II
	 * conversion process. Now we have 4 8x8 blocks each containing a 8x8 Y-Channel, 4x4 U-Channel and
	 * 4x4 V-Channel.<br><br>
	 * 
	 * This data is then filled in the previously mentioned arrays by firstly taking the first block,
	 * writing it's data into the given array (Y-Channel in Y-array, U-Channel in U-array and so on).
	 * After the first has finished the second block is read in, but now the offset of the previous
	 * data is used to offset the writer to the end of the previous block data, which in other words
	 * mean, the second, third and fourth block are starting all at a offset of 64 from each other or
	 * 16 for the subsampled channels.<br><br>
	 * 
	 * To convert it back it just has to be read with the same algorithm.
	 * </p>
	 * 
	 * @param data			The raw data containing the DCT-II coefficients.
	 * @param startPos		The position from where to start getting the DCT-II coefficients.
	 * @param size			The size of the object.
	 * @return The converted vector color difference.
	 */
	public static double[][][] getDeltaCoefficientsFromDatastream(final byte[] data,
			final int startPos, final int size) {
		double[][][] DCTCoeffGroups = ArrayUtils.get3DArray(size, true);
		final int YLength = size * size;
		final int halfSize = size >> 1;
		final int UVLength = halfSize * halfSize;
		
		final int YStart = startPos;
		final int UStart = YStart + YLength;
		final int VStart = UStart + UVLength;
		
		if (size == 4) {
			writeDCTCoeffsOutOfByteStream(data, 4, DCTCoeffGroups, YStart, UStart, VStart, 0, 0);
		} else {
			for (int u = 0, subSU = 0, x = 0, y = 0; u < YLength; u += 64, subSU += 16, x += 8) {
				if (x >= size) {
					y += 8;
					x = 0;
				}
				
				final int actualYStart = YStart + u;
				final int actualUStart = UStart + subSU;
				final int actualVStart = VStart + subSU;
				writeDCTCoeffsOutOfByteStream(data, 8, DCTCoeffGroups, actualYStart, actualUStart, actualVStart, x, y);
			}
		}
		
		return DCTCoeffGroups;
	}
	
	/**
	 * Gets the DCT-II coefficients out of the raw data stream and writes them into the given array.
	 * 
	 * @param data						The raw data from which to get the DCT-II coefficients.
	 * @param size						Size of the object.
	 * @param arrayToWriteInto			Array to write the coefficients into.
	 * @param YStart					Start of the Y channel data.
	 * @param UStart					Start of the U channel data.
	 * @param VStart					Start of the V channel data.
	 * @param offsetX					The offset to the x in which to write the data.
	 * @param offsetY					The offset to the y in which to write the data.
	 */
	private static void writeDCTCoeffsOutOfByteStream(final byte[] data, final int size,
			double[][][] arrayToWriteInto, final int YStart, final int UStart, final int VStart,
			final int offsetX, final int offsetY) {
		final int halfSize = size >> 1;
		final int halfOffsetX = offsetX >> 1;
		final int halfOffsetY = offsetY >> 1;
		
		double[][] YChannel = arrayToWriteInto[DCTConstants.Y_COEFFS_INDEX];
		double[][] UChannel = arrayToWriteInto[DCTConstants.U_COEFFS_INDEX];
		double[][] VChannel = arrayToWriteInto[DCTConstants.V_COEFFS_INDEX];
		
		double[][] YMat = null;
		double[][] UMat = null;
		double[][] VMat = null;
		
		if (size == 4) {
			YMat = ZigZagCoder4x4.decode(data, YStart);
			UMat = ZigZagCoder2x2.decode(data, UStart);
			VMat = ZigZagCoder2x2.decode(data, VStart);
		} else {
			YMat = ZigZagCoder8x8.decode(data, YStart);
			UMat = ZigZagCoder4x4.decode(data, UStart);
			VMat = ZigZagCoder4x4.decode(data, VStart);
		}
		
		ArrayUtils.copy2DArray(YMat, 0, 0, YChannel, offsetX, offsetY, size, size);
		ArrayUtils.copy2DArray(UMat, 0, 0, UChannel, halfOffsetX, halfOffsetY, halfSize, halfSize);
		ArrayUtils.copy2DArray(VMat, 0, 0, VChannel, halfOffsetX, halfOffsetY, halfSize, halfSize);
	}
	
	/**
	 * Adjusts an DCT coefficient if it's out of bounds, but only when
	 * {@link app.ArgumentProcessor#autoAdjust ArgumentProcessor.autoAdjust} is on.
	 * 
	 * @param coeff	The coefficient to adjust when possible.
	 * @return The adjusted coefficient.
	 * 
	 * @throws DCTCoefficientOutOfBoundsException When the DCT coefficient is out of bounds from -127 to 127.
	 */
	public static double getAdjustedDCTCoefficient(double coeff) throws DCTCoefficientOutOfBoundsException {
		if (coeff > 127 || coeff < -127) {
			double adjustedValue = ArgumentProcessor.autoAdjust ? coeff < -127 ? -127 : 127 : Double.NaN;
			String autoAdjust = ArgumentProcessor.autoAdjust ? "on" : "off";
			String msg = "The DCT-Coefficient must lie between -127 and 127."
					+ "You might need to adjust the quantization values. (Automatic adjust: "
					+ autoAdjust + "; from: " + coeff + "; to: " + adjustedValue + ")";

			if (ArgumentProcessor.autoAdjust) {
				System.err.println(msg);
				return adjustedValue;
			} else {
				throw new DCTCoefficientOutOfBoundsException(msg);
			}
		}
		
		return coeff;
	}
	
	/**
	 * Converts an integer to a 4 byte long byte array.
	 * 
	 * @param integer	The integer to convert.
	 * @return A byte array containing all information for reconstructing the integer.
	 */
	public static byte[] getIntBytes(final int integer) {
		byte[] arr = new byte[4];
		arr[0] = (byte)((integer >> 24) & 0xFF);
		arr[1] = (byte)((integer >> 16) & 0xFF);
		arr[2] = (byte)((integer >> 8) & 0xFF);
		arr[3] = (byte)(integer & 0xFF);
		return arr;
	}
	
	/**
	 * Reconstructs a byte array back to an integer.
	 * 
	 * @param data	The bytes to reconstruct the integer.
	 * @return The reconstructed integer.
	 * 
	 * @see #getIntBytes(int)
	 */
	public static int getIntFromBytes(final byte[] data) {
		int num = 0;
		num |= (data[0] & 0xFF) << 24;
		num |= (data[1] & 0xFF) << 16;
		num |= (data[2] & 0xFF) << 8;
		num |= data[3] & 0xFF;
		return num;
	}
	
	/**
	 * Converts a size into bytes.
	 * 
	 * <p><b>Note:</b><br>
	 * The size is considered to be <u>3 bytes</u> long.
	 * </p>
	 * 
	 * @param size	The size to convert.
	 * @return Byte array with the size.
	 */
	public static byte[] getSizeBytes(final int size) {
		byte[] arr = new byte[3];
		arr[0] = (byte)((size >> 16) & 0xFF);
		arr[1] = (byte)((size >> 8) & 0xFF);
		arr[2] = (byte)(size & 0xFF);
		return arr;
	}
	
	/**
	 * Converts a byte array that represents a size back to a size.
	 * 
	 * @param data	The byte array to convert.
	 * @return The converted size.
	 * @see #getSizeBytes(int)
	 */
	public static int getSizeFromBytes(final byte[] data) {
		int num = 0;
		num |= (data[0] & 0xFF) << 16;
		num |= (data[1] & 0xFF) << 8;
		num |= data[2] & 0xFF;
		return num;
	}
	
	/**
	 * Splits an array into even chunks with the given size per chunk.
	 * 
	 * @param data			The array to split.
	 * @param sizeOfChunk	The size of each chunk.
	 * @return A 2D byte array with all split chunks.
	 */
	public static byte[][] splitArrayEvenly(final byte[] data, final int sizeOfChunk) {
		final int estimatedLen = data.length / sizeOfChunk;
		final byte[][] arr = new byte[estimatedLen][];
		int index = 0;
		
		for (int i = 0; i < data.length; i += sizeOfChunk) {
			byte[] subArr = new byte[sizeOfChunk];
			
			for (int n = 0; n < sizeOfChunk; n++) {
				if (i + n >= data.length) {
					continue;
				}
				
				subArr[n] = data[i + n];
			}
			
			arr[index++] = subArr;
		}
		
		return arr;
	}
	
	public static int getDeltaSize(final int size) {
		return (size * size) + 2 * ((size * size) / 4);
	}
}
