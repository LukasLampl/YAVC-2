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

package app.io;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import app.ArgumentProcessor;
import app.exceptions.CorruptedFileException;
import app.interprediction.Vector;
import app.interprediction.VectorConverter;
import app.rendering.ColorManager;
import app.utils.ListManager;
import app.utils.MacroBlock;
import app.utils.PixelRaster;

/**
 * The {@code Protocol} class is responsible for converting data into bytes and back
 * using a specified protocol.
 * 
 * @author Lukas Lampl
 * @since 1.1
 */
public class Protocol {
	/**
	 * The size of an integer in bytes.
	 */
	public static final int SIZE_OF_INT = 4;
	
	/**
	 * The size of the metadata in bytes.
	 */
	public static final int META_DATA_LEN = 3 * SIZE_OF_INT;
	
	/**
	 * The length of a vector header in bytes.
	 */
	public static final int VECTOR_HEADER_LENGTH = 7;

	/**
	 * The length of a size in bytes.
	 */
	public static final int SIZE_LENGTH = 3;
	
	/**
	 * The length of the vector size checksum.
	 */
	public static final int VECTOR_SIZE_CHECK_LENGTH = SIZE_LENGTH;
	
	/**
	 * Length of the raw block header in bytes.
	 */
	public static final int RAW_BLOCK_HEADER_LENGTH = 5;
	
	/**
	 * The length of the raw block checksum.
	 */
	public static final int RAW_BLOCK_SIZE_CHECK_LENGTH = SIZE_OF_INT;
	
	/**
	 * Get the DCT coefficient in form of a byte.
	 * 
	 * @param coeff	The coefficient to convert.
	 * @return The byte representative of the coefficient.
	 */
	public static byte getDCTCoeffByte(double coeff) {
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
	public static final double getDCTCoeff(byte coeff) {
		int result = coeff & 0x7F;
		return (coeff & 0x80) != 0 ? -result : result;
	}
	
	/**
	 * Get a the span of a vector in form of two bytes.
	 * 
	 * @param spanX	The span to the X direction to convert.
	 * @param spanY	The span to the Y direction to convert.
	 * @return A byte array containing the spanX and spanY in byte form:
	 * <ul>
	 * <li>[0] - The spanX representative.
	 * <li>[1] - The spanY representative.
	 * </ul>
	 * 
	 * @throws IllegalArgumentException	When either the spanX or spanY is out of bounds: -127 <= span <= 127
	 */
	public static byte[] getVectorSpanBytes(int spanX, int spanY) throws IllegalArgumentException {
		if (spanX > 127 || spanY > 127
			|| spanX < -127 || spanY < -127) {
			throw new IllegalArgumentException("Span has to be in this boundary: -127 <= span <= 127.");
		}
		
		byte bytespany = (byte)((int)Math.abs(spanY) & 0x7F);
		byte bytespanx = (byte)((int)Math.abs(spanX) & 0x7F);
		
		if (spanY < 0) {
			bytespany = (byte)((1 << 7) | bytespany);
		}
		
		if (spanX < 0) {
			bytespanx = (byte)((1 << 7) | bytespanx);
		}
		
		return new byte[] {(byte)(bytespanx & 0xFF), (byte)(bytespany & 0xFF)};
	}
	
	/**
	 * Converts a single vector span back to integer representation.
	 * 
	 * @param span	The span byte to convert.
	 * @return The converted span.
	 */
	public static int getVectorSpanInt(byte span) {
		int res = span & 0x7F;
		return (span & 0x80) != 0 ? -res : res;
	}
	
	/**
	 * Get the byte that holds the size and reference of the vector.
	 * 
	 * @param reference	The number of frames to go back until the actual reference frame.
	 * @param size		The size of the vector (block size).
	 * @return A byte spit in two, that contains:
	 * <ul>
	 * <li>Bytes 0 to 4 - Frames to go back until the reference frame.
	 * <li>Bytes 4 to 8 - Size of the vector.
	 * </ul>
	 */
	public static byte getReferenceAndSizeByte(int reference, int size) {
		if (reference > 7 || reference < -7) {
			throw new IllegalArgumentException("Reference out of range (-7 to 7)");
		}
		
		byte res = 0;
		
		if (reference < 0) {
			res = (byte)(((1 << 7) | Math.abs(reference)) << 4);
		} else {
			res = (byte)(Math.abs(reference) << 4);
		}
		
		switch (size) {
			case 128:
				res |= 6;
				break;
			case 64:
				res |= 5;
				break;
			case 32:
				res |= 4;
				break;
			case 16:
				res |= 3;
				break;
			case 8:
				res |= 2;
				break;
			case 4:
				res |= 1;
				break;
			default:
				throw new IllegalArgumentException("Size: " + size + " not supported by YAVC");
		}
		
		return (byte)(res & 0xFF);
	}
	
	/**
	 * Converts a byte that holds reference and size back to the integer form.
	 * 
	 * @param refAndSize	The byte that holds the reference and size.
	 * @return An integer array containing two numbers:
	 * <ul>
	 * <li>[0] - Frames to go back until reference frame.
	 * <li>[1] - Size of the vector.
	 * </ul>
	 * 
	 * @see #getReferenceAndSizeByte(int, int)
	 */
	public static int[] getReferenceAndSizeInt(byte refAndSize) {
		int ref = (refAndSize >> 4) & 0x0F;
		int size = refAndSize & 0x0F;
		
		switch (size) {
			case 6:
				size = 128;
				break;
			case 5:
				size = 64;
				break;
			case 4:
				size = 32;
				break;
			case 3: 
				size = 16;
				break;
			case 2:
				size = 8;
				break;
			case 1:
				size = 4;
				break;
		}
		
		return new int[] {ref, size};
	}
	
	/**
	 * Get the given position (single coordinate) in byte representation.
	 * 
	 * @param pos	The Position coordinate to convert.
	 * @return A byte array containing the position information.
	 * 
	 * @throws IllegalArgumentException	When the coordinate is below 0 or above 65536.
	 */
	public static byte[] getPositionBytes(int pos) {
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
	public static int getPosition(byte c1, byte c2) {
		int res = (c1 & 0xFF) << 8 | (c2 & 0xFF);
		return res;
	}
	
	/**
	 * Calculates an estimated size of the vector length in total.
	 * 
	 * @param vecs	The vectors to write.
	 * @return An estimated size of the total length of all vectors, when they're converted to bytes.
	 */
	public static int calculateSize(List<Vector> vecs) {
		int size = Protocol.VECTOR_SIZE_CHECK_LENGTH;
		
		for (Vector v : vecs) {
			int refSize = v.getSize();
			size += Protocol.VECTOR_HEADER_LENGTH;
			size += (refSize * refSize) + 2 * ((refSize * refSize) / 4);
		}
		
		return size;
	}
	
	/**
	 * Converts the absolute color difference of the vector to a byte matrix which can be then encoded.
	 * 
	 * @param absoluteDifference	The absolute color difference.
	 * @param size					The size of the vector.
	 * @return A matrix representation of the absolute color difference.
	 * 
	 * @throws IllegalArgumentException	When a coefficient is > 127 or < -127.
	 */
	public static byte[][] getVectorAbsoluteColorDifferenceBytes(ArrayList<double[][][]> absoluteDifference, int size) {
		int halfSize = size / 2;
		int frac = size == 4 ? 4 : 8;
		int halfFrac = frac / 2;
		byte[] YBytes = new byte[size * size];
		byte[] UBytes = new byte[halfSize * halfSize];
		byte[] VBytes = new byte[halfSize * halfSize];
		
		int YIndex = 0;
		int UIndex = 0;
		int VIndex = 0;
		
		for (double[][][] coeffGroup : absoluteDifference) {
			for (int x = 0; x < frac; x++) {
				for (int y = 0; y < frac; y++) {
					double value = coeffGroup[0][x][y];
					if (value > 127 || value < -127) {
						double adjustedValue = ArgumentProcessor.autoAdjust ? value < -127 ? -127 : 127 : Double.NaN;
						String autoAdjust = ArgumentProcessor.autoAdjust ? "on" : "off";
						String msg = "The DCT-Coefficient must lie between -127 and 127. You might need to adjust the quantization values. (Automatic adjust: " + autoAdjust + "; from: " + value + "; to: " + adjustedValue + "; size: " + size + "; x: " + x + "; y: " + y + ")";

						if (ArgumentProcessor.autoAdjust) {
							System.err.println(msg);
							value = adjustedValue;
						} else {
							throw new IllegalArgumentException(msg);
						}
					}
					
					YBytes[YIndex++] = getDCTCoeffByte(value);
				}
			}
			
			for (int x = 0; x < halfFrac; x++) {
				for (int y = 0; y < halfFrac; y++) {
					double valueU = coeffGroup[1][x][y];
					double valueV = coeffGroup[1][x][y];
					if (valueU > 127 || valueU < -127) {
						double adjustedValue = ArgumentProcessor.autoAdjust ? valueU < -127 ? -127 : 127 : Double.NaN;
						String autoAdjust = ArgumentProcessor.autoAdjust ? "on" : "off";
						String msg = "The DCT-Coefficient must lie between -127 and 127. You might need to adjust the quantization values. (Automatic adjust: " + autoAdjust + "; from: " + valueU + "; to: " + adjustedValue + "; size: " + size + "; x: " + x + "; y: " + y + ")";

						if (ArgumentProcessor.autoAdjust) {
							System.err.println(msg);
							valueU = adjustedValue;
						} else {
							throw new IllegalArgumentException(msg);
						}
					} else if (valueV > 127 || valueV < -127) {
						double adjustedValue = ArgumentProcessor.autoAdjust ? valueV < -127 ? -127 : 127 : Double.NaN;
						String autoAdjust = ArgumentProcessor.autoAdjust ? "on" : "off";
						String msg = "The DCT-Coefficient must lie between -127 and 127. You might need to adjust the quantization values. (Automatic adjust: " + autoAdjust + "; from: " + valueV + "; to: " + adjustedValue + "; size: " + size + "; x: " + x + "; y: " + y + ")";

						if (ArgumentProcessor.autoAdjust) {
							System.err.println(msg);
							valueV = adjustedValue;
						} else {
							throw new IllegalArgumentException(msg);
						}
					}
					
					UBytes[UIndex++] = getDCTCoeffByte(coeffGroup[1][x][y]);
					VBytes[VIndex++] = getDCTCoeffByte(coeffGroup[2][x][y]);
				}
			}
		}
		
		return new byte[][] {YBytes, UBytes, VBytes};
	}
	
	/**
	 * Converts an integer to a 4 byte long byte array.
	 * 
	 * @param integer	The integer to convert.
	 * @return A byte array containing all information for reconstructing the integer.
	 */
	public static byte[] getIntBytes(int integer) {
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
	public static int getIntFromBytes(byte[] data) {
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
	public static byte[] getSizeBytes(int size) {
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
	public static int getSizeFromBytes(byte[] data) {
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
	public static byte[][] splitArrayEvenly(byte[] data, int sizeOfChunk) {
		int estimatedLen = data.length / sizeOfChunk;
		byte[][] arr = new byte[estimatedLen][];
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
	
	public static byte[] getMetadata(Dimension dimensionOfFrames, int numberOfFrames) {
		byte[] data = new byte[Protocol.META_DATA_LEN];//4 Bytes per integer.
		byte[] width = Protocol.getIntBytes(dimensionOfFrames.width);
		byte[] height = Protocol.getIntBytes(dimensionOfFrames.height);
		byte[] numOfFrames = Protocol.getIntBytes(numberOfFrames);
		writeBytesToByteArray(width, data, 0);
		writeBytesToByteArray(height, data, 4);
		writeBytesToByteArray(numOfFrames, data, 8);
		return data;
	}
	
	public static Metadata setMetadata(byte[] data) {
		if (data.length < Protocol.META_DATA_LEN) {
			throw new IllegalArgumentException("Metadata has to be " + Protocol.META_DATA_LEN + " bytes long.");
		}
		
		byte[][] parts = Protocol.splitArrayEvenly(data, Protocol.SIZE_OF_INT);
		int width = Protocol.getIntFromBytes(parts[0]);
		int height = Protocol.getIntFromBytes(parts[1]);
		int frames = Protocol.getIntFromBytes(parts[2]);
		return new Metadata(frames, new Dimension(width, height));
	}
	
	public static byte[] getVectorBytes(List<Vector> vecs, boolean discard) {
		if (vecs == null) {
			throw new NullPointerException("No vectors were passed for writing.");
		}
		
		int size = Protocol.calculateSize(vecs);
		int currentIndex = 0;
		byte[] data = new byte[size];
		writeBytesToByteArray(Protocol.getSizeBytes(vecs.size()), data, currentIndex);
		currentIndex += Protocol.VECTOR_SIZE_CHECK_LENGTH;
		
		for (Vector v : vecs) {
			currentIndex += writeSingleVectorToByteArray(v, currentIndex, data);
			
			if (discard) {
				v.discard();
			}
		}
		
		return data;
	}
	
	private static int writeSingleVectorToByteArray(Vector v, int startIndex, byte[] data) {
		int index = startIndex;
		byte[] posX = Protocol.getPositionBytes(v.getPosition().x);
		byte[] posY = Protocol.getPositionBytes(v.getPosition().y);
		byte[] span = Protocol.getVectorSpanBytes(v.getSpanX(), v.getSpanY());
		byte refAndSize = Protocol.getReferenceAndSizeByte(v.getReference(), v.getSize());
		byte[][] differences = Protocol.getVectorAbsoluteColorDifferenceBytes(v.getDCTCoefficientsOfAbsoluteColorDifference(), v.getSize());
		
		writeBytesToByteArray(posX, data, index);
		index += posX.length;
		writeBytesToByteArray(posY, data, index);
		index += posY.length;
		writeBytesToByteArray(span, data, index);
		index += span.length;
		data[index] = refAndSize;
		index += 1;
		
		for (int n = 0; n < differences.length; n++) {
			writeBytesToByteArray(differences[n], data, index);
			index += differences[n].length;
		}
		
		return index - startIndex;
	}
	
	/**
	 * Get all vectors out of a data stream by first analyzing their indexes and finally decoding them
	 * asynchronously.
	 * 
	 * @param data					The data chunk that holds the vectors.
	 * @param vectorListManager		A {@code ListManager<T>} that has cached object to be reused.
	 * @param singleThread			Flag for whether the decoding should be single threaded or not.
	 * @throws CorruptedFileException	When the vector size is not equal to the coded length.
	 * 
	 * @see app.utils.ListManager
	 */
	public static void getVectors(byte[] data, ListManager<Vector> vectorListManager, boolean singleThread) throws CorruptedFileException {
		if (data.length <= 1) {
			return;
		}

		byte[] lenOfVecs = {data[0], data[1], data[2]};
		int estimatedLength = Protocol.getSizeFromBytes(lenOfVecs);
		ArrayList<Integer> indexesOfVectors = new ArrayList<Integer>();
		precalculateVectorIndexes(data, indexesOfVectors);
		
		VectorConverter converter = new VectorConverter(data, indexesOfVectors, vectorListManager, singleThread);
		converter.start();
		converter.awaitTermination();
		
		if (vectorListManager.getList().size() != estimatedLength) {
			throw new CorruptedFileException("The amount of the read-in vectors appears to be unequal to the written vectors.");
		}
	}
	
	/**
	 * Calculates the indexes of each vector read from the byte representation.
	 * 
	 * @param data	The data part with all vectors.
	 * @param indexesOfVectors	A List to which to add the indexes to.
	 */
	private static void precalculateVectorIndexes(byte[] data, List<Integer> indexesOfVectors) {
		int i = Protocol.VECTOR_SIZE_CHECK_LENGTH;
		
		while (i < data.length) {
			indexesOfVectors.add(i);
			int[] refAndSize = Protocol.getReferenceAndSizeInt(data[i + 6]);
			int size = refAndSize[1];
			//Length of the vector diffs
			//Original formula: (size * size) + 2 * ((size / 2) * (size / 2)) + Protocol.VECTOR_HEADER_LENGTH
			i += ((size * size) + 2 * ((size * size) / 4)) + Protocol.VECTOR_HEADER_LENGTH;
		}
	}
	
	/**
	 * Convert a list full of lengths to byte representation.
	 * 
	 * @param lengths	The list to convert.
	 * @return A byte array that can represent all lengths.
	 */
	public static byte[] getLengthBytesOfFrame(List<Integer> lengths) {
		int estimatedSize = lengths.size() * Protocol.SIZE_LENGTH + Protocol.SIZE_LENGTH;
		byte[] data = new byte[estimatedSize];
		int currentIndex = 0;
		
		byte[] lenOfIndexes = Protocol.getSizeBytes(lengths.size());
		writeBytesToByteArray(lenOfIndexes, data, currentIndex);
		currentIndex += Protocol.SIZE_LENGTH;
		
		for (int i : lengths) {
			byte[] index = Protocol.getSizeBytes(i);
			writeBytesToByteArray(index, data, currentIndex);
			currentIndex += Protocol.SIZE_LENGTH;
		}
		
		return data;
	}
	
	/**
	 * Get the length of each frame part.
	 * 
	 * @param lengthStream	The data stream that holds all lengths.
	 * @param lengthList	The list to which to add the lengths of the parts.
	 */
	public static void setLengthsOfEachFramePart(byte[] lengthStream, List<Integer> lengthList) {
		byte[][] data = Protocol.splitArrayEvenly(lengthStream, Protocol.SIZE_LENGTH);
		
		for (byte[] byteNum : data) {
			int length = Protocol.getSizeFromBytes(byteNum);
			lengthList.add(length);
		}
	}
	
	/**
	 * Converts a {@link app.utils.PixelRaster} to byte representation.
	 * 
	 * @param raster	The {@code PixelRaster} to convert.
	 * @return The converted PixelRaster.
	 */
	public static byte[] getStartFrameBytes(PixelRaster raster) {
		byte[] data = new byte[raster.getWidth() * raster.getHeight() * 3 + 1];
		double[] YUVCache = new double[3]; //Size of 3, because of 3 channels
		int index = 0;
		
		for (int x = 0; x < raster.getWidth(); x++) {
			for (int y = 0; y < raster.getHeight(); y++) {
				int rgb = ColorManager.convertYUVToRGB(raster.getYUV(x, y, YUVCache));
				byte r = (byte)((rgb >> 16) & 0xFF);
				byte g = (byte)((rgb >> 8) & 0xFF);
				byte b = (byte)(rgb & 0xFF);
				data[index] = r;
				data[index + 1] = g;
				data[index + 2] = b;
				index += 3;
			}
		}
		
		return data;
	}
	
	public static PixelRaster reconstructStartFrame(byte[] data, Dimension dim) {
		PixelRaster render = new PixelRaster(dim);

		for (int x = 0, index = 0; x < dim.width; x++) {
			for (int y = 0; y < dim.height; y++) {
				byte r = data[index];
				byte g = data[index + 1];
				byte b = data[index + 2];
				int rgb = (0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
				double[] YUV = ColorManager.convertRGBToYUV(rgb);
				render.setYUV(x, y, YUV);
				index += 3;
			}
		}
		
		return render;
	}
	
	public static byte[] getRawBlockBytes(List<MacroBlock> blocks) {
		int size = Protocol.RAW_BLOCK_SIZE_CHECK_LENGTH;
		
		for (MacroBlock b : blocks) {
			size += (b.getSquaredSize() * 3) + Protocol.RAW_BLOCK_HEADER_LENGTH;
		}
		
		byte[] data = new byte[size];
		double[] YUVCache = new double[3]; //Size of 3 because of 3 channels
		int currentIndex = 0;
		writeBytesToByteArray(Protocol.getIntBytes(blocks.size()), data, currentIndex);
		currentIndex += Protocol.RAW_BLOCK_SIZE_CHECK_LENGTH;
		
		for (MacroBlock block : blocks) {
			currentIndex += writeSingleRawBlockToByteArray(block, YUVCache, data, currentIndex);
			block.discard();
		}
		
		return data;
	}
	
	private static int writeSingleRawBlockToByteArray(MacroBlock block, double[] YUVCache, byte[] data, int startIndex) {
		int index = startIndex;
		Point pos = block.getPosition();
		byte[] posX = Protocol.getPositionBytes(pos.x);
		byte[] posY = Protocol.getPositionBytes(pos.y);
		byte sizeBytes = Protocol.getReferenceAndSizeByte(0, block.getSize());
		byte[] differences = new byte[block.getSquaredSize() * 3];
		
		for (int y = 0, diffIndex = 0; y < block.getSize(); y++) {
			for (int x = 0; x < block.getSize(); x++) {
				int argb = ColorManager.convertYUVToRGB(block.getYUV(x, y, YUVCache));
				byte r = (byte)((argb >> 16) & 0xFF);
				byte g = (byte)((argb >> 8) & 0xFF);
				byte b = (byte)(argb & 0xFF);
				differences[diffIndex] = r;
				differences[diffIndex + 1] = g;
				differences[diffIndex + 2] = b;
				diffIndex += 3;
			}
		}
		
		writeBytesToByteArray(posX, data, index);
		index += posX.length;
		writeBytesToByteArray(posY, data, index);
		index += posY.length;
		data[index] = sizeBytes;
		index += 1;
		writeBytesToByteArray(differences, data, index);
		index += differences.length;
		return index - startIndex;
	}
	
	public static ArrayList<MacroBlock> getRawBlocks(byte[] data) throws CorruptedFileException {
		ArrayList<MacroBlock> blocks = new ArrayList<MacroBlock>();
		int i = 0;
		byte[] lenOfBlocks = {data[0], data[1], data[2], data[3]};
		int estimatedLength = Protocol.getIntFromBytes(lenOfBlocks);
		i += Protocol.RAW_BLOCK_SIZE_CHECK_LENGTH;
		
		while (i < data.length) {
			i += addSingleRawBlockToList(data, i, blocks);
		}
		
		if (blocks.size() != estimatedLength) {
			throw new CorruptedFileException("The amount of the read-in raw-blocks appears to be unequal to the written raw-blocks.");
		}
		
		return blocks;
	}
	
	private static int addSingleRawBlockToList(byte[] data, int currentIndex, List<MacroBlock> list) {
		int posX = Protocol.getPosition(data[currentIndex], data[currentIndex + 1]);
		int posY = Protocol.getPosition(data[currentIndex + 2], data[currentIndex + 3]);
		int[] sizeBytes = Protocol.getReferenceAndSizeInt(data[currentIndex + 4]);
		int size = sizeBytes[1];
		MacroBlock block = new MacroBlock(posX, posY, size, true);
		int length = block.getSquaredSize() * 3;
		int offset = currentIndex + Protocol.RAW_BLOCK_HEADER_LENGTH;
		int x = 0;
		int y = 0;
		
		for (int n = offset; n < length + offset; n += 3) {
			int r = data[n] & 0xFF;
			int g = data[n + 1] & 0xFF;
			int b = data[n + 2] & 0xFF;
			int argb = (0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
			block.setYUV(x++, y, ColorManager.convertRGBToYUV(argb));
			
			if (x >= size) {
				x = 0;
				y++;
			}
		}
		
		list.add(block);
		return Protocol.RAW_BLOCK_HEADER_LENGTH + length;
	}
	
	/**
	 * Writes a subarray into another array at the specified index.
	 * 
	 * @param bytes	The byte array to write into the other array.
	 * @param arr	The array to write into.
	 * @param index	Index at which to start writing into {@code arr}.
	 * 
	 * @throws ArrayIndexOutOfBoundsException	When the written array + index exceed the array in which to write.
	 */
	private static void writeBytesToByteArray(byte[] bytes, byte[] arr, int index) {
		for (int i = 0; i < bytes.length; i++) {
			arr[index++] = bytes[i];
		}
	}
}
