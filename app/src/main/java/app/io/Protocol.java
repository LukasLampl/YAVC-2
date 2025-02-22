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

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import app.engines.prediction.interprediction.DecodingVector;
import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.interprediction.Vector;
import app.engines.prediction.interprediction.VectorConverterPool;
import app.engines.prediction.intraprediction.DecodingIntraPredictionBlock;
import app.engines.prediction.intraprediction.EncodingIntraPredictionBlock;
import app.engines.prediction.intraprediction.IntraConverterPool;
import app.engines.prediction.intraprediction.IntraPipeline;
import app.engines.quadtree.QuadtreeBase;
import app.exceptions.CorruptedFileException;
import app.exceptions.DCTCoefficientOutOfBoundsException;
import app.io.coder.cabac.CABAC;
import app.io.coder.cabac.ContextModelManager;
import app.io.coder.cabac.ContextModelManager.CodingType;
import app.managers.ListManager;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.MathUtils;
import app.utils.PixelRaster;
import app.utils.components.Component2D;
import app.utils.components.MacroBlock;

/**
 * The {@code Protocol} class is responsible for converting data into bytes and back
 * using a specified protocol.
 * 
 * @author Lukas Lampl
 * @since 1.1
 */
public class Protocol {
	/**
	 * The size of the metadata in bytes.
	 */
	public static final int META_DATA_LEN = 3 * ProtocolBase.SIZE_OF_INT;
	
	/**
	 * The length of a vector header in bytes.
	 */
	public static final int VECTOR_HEADER_LENGTH = 3;
	
	/**
	 * The length of the vector size checksum.
	 */
	public static final int VECTOR_SIZE_CHECK_LENGTH = ProtocolBase.SIZE_LENGTH;
	
	/**
	 * Length of the intra block header in bytes.
	 */
	public static final int INTRA_BLOCK_HEADER_LENGTH = 2;
	
	/**
	 * The length of the intra blocks checksum.
	 */
	public static final int INTRA_BLOCK_SIZE_CHECK_LENGTH = ProtocolBase.SIZE_LENGTH;
	
	/**
	 * The length of the raw block checksum.
	 */
	public static final int RAW_BLOCK_SIZE_CHECK_LENGTH = ProtocolBase.SIZE_OF_INT;
	
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
	public static byte[] getVectorSpanBytes(final int spanX, final int spanY) throws IllegalArgumentException {
		if (spanX > 127 || spanY > 127
			|| spanX < -127 || spanY < -127) {
			throw new IllegalArgumentException("Span has to be in this boundary: -127 <= span <= 127.");
		}
		
		byte bytespany = (byte)((int)MathUtils.abs(spanY) & 0x7F);
		byte bytespanx = (byte)((int)MathUtils.abs(spanX) & 0x7F);
		
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
	public static int getVectorSpanInt(final byte span) {
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
	 * <li>Bits 0 to 4 - Frames to go back until the reference frame.
	 * <li>Bits 4 to 8 - Size of the vector.
	 * </ul>
	 */
	public static byte getReferenceAndSizeByte(final int reference, final int size) {
		if (reference > 7 || reference < -7) {
			throw new IllegalArgumentException("Reference out of range (-7 to 7)");
		}
		
		byte res = 0;
		
		if (reference < 0) {
			res = (byte)(((1 << 7) | (int)MathUtils.abs(reference)) << 4);
		} else {
			res = (byte)((int)MathUtils.abs(reference) << 4);
		}
		
		res |= QuadtreeBase.getIndexBySize(size);
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
	public static int[] getReferenceAndSizeInt(final byte refAndSize) {
		int ref = (refAndSize >> 4) & 0x0F;
		int size = QuadtreeBase.getSizeByIndex(refAndSize & 0x0F);
		return new int[] {ref, size};
	}
	
	/**
	 * Calculates an estimated size of the vector length in total.
	 * 
	 * @param vecs	The vectors to write.
	 * @return An estimated size of the total length of all vectors, when they're converted to bytes.
	 */
	public static int calculateVectorSize(final List<EncodingVector> vecs) {
		int size = Protocol.VECTOR_SIZE_CHECK_LENGTH;
		
		for (final Vector v : vecs) {
			int refSize = v.getSize();
			size += Protocol.VECTOR_HEADER_LENGTH;
			size += (refSize * refSize) + 2 * ((refSize * refSize) / 4);
		}
		
		return size;
	}
	
	/**
	 * Gets the metadata in form of an byte array.
	 * 
	 * @param dimensionOfFrames	Dimension of all frames.
	 * @param numberOfFrames	The number of frames.
	 * @return A byte representation of the metadata.
	 */
	public static byte[] getMetadata(final Dimension dimensionOfFrames, final int numberOfFrames) {
		final byte[] data = new byte[Protocol.META_DATA_LEN];//4 Bytes per integer.
		final byte[] width = ProtocolBase.getIntBytes(dimensionOfFrames.width);
		final byte[] height = ProtocolBase.getIntBytes(dimensionOfFrames.height);
		final byte[] numOfFrames = ProtocolBase.getIntBytes(numberOfFrames);
		writeBytesToByteArray(width, data, 0);
		writeBytesToByteArray(height, data, 4);
		writeBytesToByteArray(numOfFrames, data, 8);
		return data;
	}
	
	/**
	 * Gets the metadata out of an byte array.
	 * 
	 * @param data	The byte array containing the metadata.
	 * @return A {@link app.io.Metadata Metadata} object with data.
	 */
	public static Metadata setMetadata(final byte[] data) {
		if (data.length < Protocol.META_DATA_LEN) {
			throw new IllegalArgumentException("Metadata has to be " + Protocol.META_DATA_LEN + " bytes long.");
		}
		
		final byte[][] parts = ProtocolBase.splitArrayEvenly(data, ProtocolBase.SIZE_OF_INT);
		final int width = ProtocolBase.getIntFromBytes(parts[0]);
		final int height = ProtocolBase.getIntFromBytes(parts[1]);
		final int frames = ProtocolBase.getIntFromBytes(parts[2]);
		return new Metadata(frames, new Dimension(width, height));
	}
	
	/**
	 * Converts a list of vectors into byte form.
	 * 
	 * @param vecs		The vectors to convert.
	 * @param discard	Flag for whether the vectors should be discarded afterwards.
	 * @return An byte array with all vectors.
	 */
	public static byte[] getVectorBytes(final List<EncodingVector> vecs, final boolean discard) {
		if (vecs == null) {
			throw new NullPointerException("No vectors were passed for writing.");
		}
		
		final int size = Protocol.calculateVectorSize(vecs);
		int currentIndex = 0;
		final byte[] data = new byte[size];
		writeBytesToByteArray(ProtocolBase.getSizeBytes(vecs.size()), data, currentIndex);
		currentIndex += Protocol.VECTOR_SIZE_CHECK_LENGTH;
		
		for (final EncodingVector v : vecs) {
			currentIndex += writeSingleVectorToByteArray(v, currentIndex, data);
			
			if (discard) {
				v.discard();
			}
		}
		
		return data;
	}
	
	/**
	 * Writes a single vector into the given byte array.
	 * 
	 * @param v				The vector to convert and write.
	 * @param startIndex	Index at where to start writing the vector.
	 * @param data			Byte array in which to write.
	 * @return The length of the written vector in bytes.
	 */
	private static int writeSingleVectorToByteArray(final EncodingVector v, final int startIndex, final byte[] data) {
		int index = startIndex;
		final byte[] posX = ProtocolBase.getPositionBytes(v.getPosition().x);
		final byte[] posY = ProtocolBase.getPositionBytes(v.getPosition().y);
		final byte[] span = Protocol.getVectorSpanBytes(v.getSpanX(), v.getSpanY());
		final byte refAndSize = Protocol.getReferenceAndSizeByte(v.getReference(), v.getSize());
		byte[][] differences = null;
		
		try {
			differences = ProtocolBase.getDeltaMatrixBytes(v.getYUVDelta(), v.getSize());
		} catch (DCTCoefficientOutOfBoundsException e) {
			System.out.println("Size: " + v.getSize());
			e.printStackTrace();
		}
		
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
	 * @see app.managers.ListManager
	 */
	public static void getVectors(final byte[] data, final ListManager<DecodingVector> vectorListManager,
			final boolean singleThread) throws CorruptedFileException {
		if (data.length <= 1) {
			return;
		}

		final byte[] lenOfVecs = {data[0], data[1], data[2]};
		final int estimatedLength = ProtocolBase.getSizeFromBytes(lenOfVecs);
		ArrayList<Integer> indexesOfVectors = new ArrayList<Integer>();
		precalculateVectorIndexes(data, indexesOfVectors);
		
		VectorConverterPool pool = new VectorConverterPool(indexesOfVectors, data, vectorListManager, singleThread);
		pool.run();
		
		if (vectorListManager.getList().size() != estimatedLength) {
			throw new CorruptedFileException("The amount of the read-in vectors appear to be unequal to the written vectors.");
		}
	}
	
	/**
	 * Calculates the indexes of each vector read from the byte representation.
	 * 
	 * @param data	The data part with all vectors.
	 * @param indexesOfVectors	A List to which to add the indexes to.
	 */
	private static void precalculateVectorIndexes(final byte[] data, final List<Integer> indexesOfVectors) {
		int i = Protocol.VECTOR_SIZE_CHECK_LENGTH;
		
		while (i < data.length) {
			indexesOfVectors.add(i);
			final int[] refAndSize = Protocol.getReferenceAndSizeInt(data[i + 6]);
			final int size = refAndSize[1];
			//Length of the vector diffs
			//Original formula: (size * size) + 2 * ((size / 2) * (size / 2)) + Protocol.VECTOR_HEADER_LENGTH
			i += ((size * size) + 2 * ((size * size) >> 2)) + Protocol.VECTOR_HEADER_LENGTH;
		}
	}
	
	/**
	 * Convert a list full of lengths to byte representation.
	 * 
	 * @param lengths	The list to convert.
	 * @return A byte array that can represent all lengths.
	 */
	public static byte[] getLengthBytesOfFrame(final List<Integer> lengths) {
		final int estimatedSize = lengths.size() * ProtocolBase.SIZE_LENGTH + ProtocolBase.SIZE_LENGTH;
		final byte[] data = new byte[estimatedSize];
		int currentIndex = 0;
		
		final byte[] lenOfIndexes = ProtocolBase.getSizeBytes(lengths.size());
		writeBytesToByteArray(lenOfIndexes, data, currentIndex);
		currentIndex += ProtocolBase.SIZE_LENGTH;
		
		for (int i : lengths) {
			final byte[] index = ProtocolBase.getSizeBytes(i);
			writeBytesToByteArray(index, data, currentIndex);
			currentIndex += ProtocolBase.SIZE_LENGTH;
		}
		
		return data;
	}
	
	/**
	 * Get the length of each frame part.
	 * 
	 * @param lengthStream	The data stream that holds all lengths.
	 * @param lengthList	The list to which to add the lengths of the parts.
	 */
	public static void setLengthsOfEachFramePart(final byte[] lengthStream, final List<Integer> lengthList) {
		final byte[][] data = ProtocolBase.splitArrayEvenly(lengthStream, ProtocolBase.SIZE_LENGTH);
		
		for (byte[] byteNum : data) {
			final int length = ProtocolBase.getSizeFromBytes(byteNum);
			lengthList.add(length);
		}
	}
	
	/**
	 * Converts a {@link app.utils.PixelRaster} to byte representation.
	 * 
	 * @param raster	The {@code PixelRaster} to convert.
	 * @return The converted PixelRaster.
	 */
	public static byte[] getStartFrameBytes(final PixelRaster raster) {
		final byte[] data = new byte[raster.getWidth() * raster.getHeight() * 3 + 1];
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
	
	/**
	 * Reconstructs the start frame based on the given data and dimension.
	 * 
	 * @param data	The data containing pixel information about the start frame.
	 * @param dim	The dimension of the start frame.
	 * @return A reconstructed start frame.
	 */
	public static PixelRaster reconstructStartFrame(final byte[] data, final Dimension dim) {
		PixelRaster render = new PixelRaster(dim);

		for (int x = 0, index = 0; x < dim.width; x++) {
			for (int y = 0; y < dim.height; y++) {
				byte r = data[index];
				byte g = data[index + 1];
				byte b = data[index + 2];
				double[] YUV = ColorManager.convertRGBToYUV(r, g, b);
				render.setYUV(x, y, YUV);
				index += 3;
			}
		}
		
		return render;
	}
	
	/**
	 * Converts a list of IntraPredictionBlocks into an byte array.
	 * 
	 * @param blocks	IntraPredictionBlocks to convert.
	 * @return A byte representation of the IntraPredictionBlock list.
	 */
	public static byte[] getIntraBlockBytes(final List<EncodingIntraPredictionBlock> intraBlocks, boolean discard) {
		if (intraBlocks == null) {
			throw new NullPointerException("No vectors were passed for writing.");
		}
		
		final int size = getSizeOfIntraBlocks(intraBlocks);
		int currentIndex = 0;
		byte[] data = new byte[size];
		writeBytesToByteArray(ProtocolBase.getSizeBytes(intraBlocks.size()), data, currentIndex);
		currentIndex += Protocol.INTRA_BLOCK_SIZE_CHECK_LENGTH;
		
		for (final EncodingIntraPredictionBlock block : intraBlocks) {
			currentIndex += writeSingleIntraPredictionBlockInByteArray(block, currentIndex, data);
			
			if (discard) {
				block.discard();
			}
		}
		
		return data;
	}
	
	private static int getSizeOfIntraBlocks(List<EncodingIntraPredictionBlock> intraBlocks) {
		int size = Protocol.INTRA_BLOCK_SIZE_CHECK_LENGTH;
		
		for (final EncodingIntraPredictionBlock block : intraBlocks) {
			final int blockSize = block.getSize();
			final int halfBlockSize = blockSize >> 1;
			size += Protocol.INTRA_BLOCK_HEADER_LENGTH;
			size += (blockSize * blockSize) + 2 * (halfBlockSize * halfBlockSize);
			size += ((blockSize + blockSize) * ColorManager.CHANNELS); // Pixels at border
		}
		
		return size;
	}
	
	private static final int writeSingleIntraPredictionBlockInByteArray(final EncodingIntraPredictionBlock block,
			final int startIndex, final byte[] data) {
		int index = startIndex;
		final byte[] posX = ProtocolBase.getPositionBytes(block.getPositionX());
		final byte[] posY = ProtocolBase.getPositionBytes(block.getPositionY());
		final byte[] sizeAndAngle = Protocol.getSizeAndAngleByte(block.getSize(), block.getAngle());
		final byte[] borderColors = Protocol.getBorderColorBytes(block.getVertical(), block.getHorizontal(), block.getSize());
		
		byte[][] differences = null;
		
		try {
			differences = ProtocolBase.getDeltaMatrixBytes(block.getYUVDeltas(), block.getSize());
		} catch (DCTCoefficientOutOfBoundsException e) {
			e.printStackTrace();
		}
		
		writeBytesToByteArray(posX, data, index);
		index += posX.length;
		writeBytesToByteArray(posY, data, index);
		index += posY.length;
		writeBytesToByteArray(sizeAndAngle, data, index);
		index += sizeAndAngle.length;
		writeBytesToByteArray(borderColors, data, index);
		index += borderColors.length;
		
		for (int n = 0; n < differences.length; n++) {
			writeBytesToByteArray(differences[n], data, index);
			index += differences[n].length;
		}
		
		return index - startIndex;
	}
	
	public static byte[] getBorderColorBytes(final double[][] verticalYUV,
			final double[][] horizontalYUV, final int blockSize) {
		final int size = 2 * (blockSize * ColorManager.CHANNELS);
		byte[] data = new byte[size];
		int offset_h = 0;
		int offset_v = blockSize * ColorManager.CHANNELS;
		int[] rgb_h = new int[3];
		int[] rgb_v = new int[3];
		
		for (int i = 0; i < blockSize; i++, offset_h += 3, offset_v += 3) {
			rgb_h = ColorManager.convertYUVToRGB_intARR(horizontalYUV[i], rgb_h);
			rgb_v = ColorManager.convertYUVToRGB_intARR(verticalYUV[i], rgb_v);
			
			data[offset_h] = (byte)(rgb_h[ColorManager.R_INDEX] & 0xFF);
			data[offset_h + 1] = (byte)(rgb_h[ColorManager.G_INDEX] & 0xFF);
			data[offset_h + 2] = (byte)(rgb_h[ColorManager.B_INDEX] & 0xFF);
			
			data[offset_v] = (byte)(rgb_v[ColorManager.R_INDEX] & 0xFF);
			data[offset_v + 1] = (byte)(rgb_v[ColorManager.G_INDEX] & 0xFF);
			data[offset_v + 2] = (byte)(rgb_v[ColorManager.B_INDEX] & 0xFF);
		}

		return data;
	}
	
	public static double[][][] getBorderColors(final byte[] data, final int blockSize, final int dataOffset) {
		double[][] vertical = new double[blockSize][ColorManager.CHANNELS];
		double[][] horizontal = new double[blockSize][ColorManager.CHANNELS];
		int offset_h = dataOffset;
		int offset_v = dataOffset + (blockSize * ColorManager.CHANNELS);
		
		for (int i = 0; i < blockSize; i++, offset_h += 3, offset_v += 3) {
			byte r_h = data[offset_h];
			byte g_h = data[offset_h + 1];
			byte b_h = data[offset_h + 2];
			
			byte r_v = data[offset_v];
			byte g_v = data[offset_v + 1];
			byte b_v = data[offset_v + 2];
			
			vertical[i] = ColorManager.convertRGBToYUV(r_v, g_v, b_v);
			horizontal[i] = ColorManager.convertRGBToYUV(r_h, g_h, b_h);
		}
		
		return new double[][][] {vertical, horizontal};
	}
	
	public static byte[] getSizeAndAngleByte(final int size, final int angle) {
		byte angleByte = (byte)(IntraPipeline.getIndexByAngle(angle) & 0xFF);
		byte sizeByte = (byte)(QuadtreeBase.getIndexBySize(size) & 0xFF);
		return new byte[] {sizeByte, angleByte};
	}
	
	public static int[] getSizeAndAngle(final byte sizeByte, final byte angleByte) {
		int angle = IntraPipeline.getAngleByIndex(angleByte);
		int size = QuadtreeBase.getSizeByIndex(sizeByte);
		return new int[] {size, angle};
	}
	
	public static void getIntraBlocks(final byte[] data, final ListManager<DecodingIntraPredictionBlock> intraBlockManager,
			final boolean singleThread) throws CorruptedFileException {
		if (data.length <= 1) {
			return;
		}

		final byte[] lenOfIntraBlocks = {data[0], data[1], data[2]};
		final int estimatedLength = ProtocolBase.getSizeFromBytes(lenOfIntraBlocks);
		ArrayList<Integer> indexesOfIntraBlocks = new ArrayList<Integer>();
		precalculateIntraBlockIndexes(data, indexesOfIntraBlocks);
		
		IntraConverterPool pool = new IntraConverterPool(indexesOfIntraBlocks, data, intraBlockManager, singleThread);
		pool.run();
		
		if (intraBlockManager.getList().size() != estimatedLength) {
			throw new CorruptedFileException("The amount of the read-in intra blocks appear to be unequal to the written inrta blocks.");
		}
	}
	
	/**
	 * Calculates the indexes of each vector read from the byte representation.
	 * 
	 * @param data	The data part with all vectors.
	 * @param indexesOfVectors	A List to which to add the indexes to.
	 */
	private static void precalculateIntraBlockIndexes(final byte[] data, final List<Integer> indexesOfIntraBlocks) {
		int i = Protocol.INTRA_BLOCK_SIZE_CHECK_LENGTH;
		
		while (i < data.length) {
			indexesOfIntraBlocks.add(i);
			final int[] sizeAndAngle = Protocol.getSizeAndAngle(data[i + 4], data[i + 5]);
			final int size = sizeAndAngle[0];
			//Length of the vector diffs
			//Original formula: (size * size) + 2 * ((size / 2) * (size / 2)) + Protocol.INTRA_BLOCK_HEADER_LENGTH
			i += ((size * size) + 2 * ((size * size) / 4))
					+ Protocol.INTRA_BLOCK_HEADER_LENGTH
					+ ((size + size) * 3);
		}
	}
	
	/**
	 * Writes a sub-array into another array at the specified index.
	 * 
	 * @param bytes	The byte array to write into the other array.
	 * @param arr	The array to write into.
	 * @param index	Index at which to start writing into {@code arr}.
	 * 
	 * @throws ArrayIndexOutOfBoundsException	When the written array + index exceed the array in which to write.
	 */
	private static void writeBytesToByteArray(final byte[] bytes, byte[] arr, int index) {
		for (int i = 0; i < bytes.length; i++) {
			arr[index++] = bytes[i];
		}
	}
	
	public static BitWriter binarizeQuadtrees(List<MacroBlock> roots) throws IOException {
		ContextModelManager manager = new ContextModelManager();
		CABAC encoder = new CABAC();
		BitWriter output = new BitWriter();
		
		for (final MacroBlock root : roots) {
			encoder.encode(new BitReader(ProtocolBase.getPositionBytes(root.getPositionX())),
					output, manager.getModel(CodingType.QUADTREE_POSITION_X));
			encoder.encode(new BitReader(ProtocolBase.getPositionBytes(root.getPositionY())),
					output, manager.getModel(CodingType.QUADTREE_POSITION_Y));
			
			binarizeSingleQuadtree(root, encoder, manager, output);
		}
		
		return output;
	}
	
	private static byte[] binarizeSingleQuadtree(final MacroBlock root,
			final CABAC encoder, final ContextModelManager manager,
			final BitWriter output)
					throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		if (root.isSubdivided()) {
			encoder.encode(0x01, output, manager.getModel(CodingType.QUADTREE_SUBDIVISION));
			
			for (final MacroBlock child : root.getNodes()) {
				baos.write(binarizeSingleQuadtree(child, encoder, manager, output));
			}
		} else {
			encoder.encode(0x00, output, manager.getModel(CodingType.QUADTREE_SUBDIVISION));
			
			final Component2D link = root.getLink();
			
			if (link instanceof EncodingIntraPredictionBlock) {
				encoder.encode(0x01, output, manager.getModel(CodingType.PREDICTION_TYPE));
				
				getSingleIntraPredictionBlock((EncodingIntraPredictionBlock)link, encoder, manager, output);
			} else if (link instanceof EncodingVector) {
				encoder.encode(0x01, output, manager.getModel(CodingType.PREDICTION_TYPE));
				
				binarizeVector((EncodingVector)link, encoder, manager, output);
			} else {
				throw new IllegalStateException("Illegal link type: " + link);
			}
		}
		
		return baos.toByteArray();
	}
	
	private static final void getSingleIntraPredictionBlock(final EncodingIntraPredictionBlock block,
			final CABAC encoder, final ContextModelManager manager,
			final BitWriter output) {
		final byte[] sizeAndAngle = Protocol.getSizeAndAngleByte(block.getSize(), block.getAngle());
		final byte[] borderColors = Protocol.getBorderColorBytes(block.getVertical(), block.getHorizontal(), block.getSize());
		
		byte[][] differences = null;
		
		try {
			differences = ProtocolBase.getDeltaMatrixBytes(block.getYUVDeltas(), block.getSize());
		} catch (DCTCoefficientOutOfBoundsException e) {
			e.printStackTrace();
		}
		
		encoder.encode(new BitReader(sizeAndAngle), output, manager.getModel(CodingType.INTRA_PREDICTION_ANGLE));
		
		encoder.encode(new BitReader(borderColors), output, manager.getModel(CodingType.INTRA_BORDER_HORIZONTAL));
		
		encoder.encode(new BitReader(differences[ColorManager.Y_INDEX]),
				output, manager.getModel(CodingType.RESIDUALS_Y));
		encoder.encode(new BitReader(differences[ColorManager.U_INDEX]),
				output, manager.getModel(CodingType.RESIDUALS_U));
		encoder.encode(new BitReader(differences[ColorManager.V_INDEX]),
				output, manager.getModel(CodingType.RESIDUALS_V));
	}
	
	public static DecodingVector debinarizeVector(final CABAC decoder,
			final ContextModelManager manager, final BitReader input,
			final int vectorSize) {
		final int differenceY_Length = vectorSize * vectorSize;
		final int differenceUV_Length = differenceY_Length / 4;
		
		BitWriter spanXWriter = new BitWriter();
		BitWriter spanYWriter = new BitWriter();
		BitWriter referenceAndSizeWriter = new BitWriter();
		BitWriter diffYWriter = new BitWriter();
		BitWriter diffUWriter = new BitWriter();
		BitWriter diffVWriter = new BitWriter();
		
		decoder.decode(Byte.SIZE, input, spanXWriter, manager.getModel(CodingType.VECTOR_SPAN_X));
		decoder.decode(Byte.SIZE, input, spanYWriter, manager.getModel(CodingType.VECTOR_SPAN_Y));
		decoder.decode(Byte.SIZE, input, referenceAndSizeWriter, manager.getModel(CodingType.VECTOR_REFERENCE));
		decoder.decode(differenceY_Length * Byte.SIZE, input, diffYWriter, manager.getModel(CodingType.RESIDUALS_Y));
		decoder.decode(differenceUV_Length * Byte.SIZE, input, diffUWriter, manager.getModel(CodingType.RESIDUALS_U));
		decoder.decode(differenceUV_Length * Byte.SIZE, input, diffVWriter, manager.getModel(CodingType.RESIDUALS_V));
		
		byte[] spanX = spanXWriter.toByteArray();
		byte[] spanY = spanYWriter.toByteArray();
		byte[] deltas = new byte[differenceY_Length + 2 * differenceUV_Length];
		ArrayUtils.copyArray(diffYWriter.toByteArray(), 0, deltas, 0, differenceY_Length);
		ArrayUtils.copyArray(diffUWriter.toByteArray(), 0, deltas, differenceY_Length, differenceUV_Length);
		ArrayUtils.copyArray(diffVWriter.toByteArray(), 0, deltas, differenceY_Length + differenceUV_Length, differenceUV_Length);
		
		final int f_spanX = Protocol.getVectorSpanInt(spanX[0]);
		final int f_spanY = Protocol.getVectorSpanInt(spanY[0]);
		final int[] f_refAndSize = Protocol.getReferenceAndSizeInt(referenceAndSizeWriter.getFirstByte());
		final double[][][] f_deltas = ProtocolBase.getDeltaCoefficientsFromDatastream(deltas, 0, 8);
		
		DecodingVector vec = new DecodingVector(0, 0, 8);
		vec.setSpanX(f_spanX);
		vec.setSpanY(f_spanY);
		vec.setReference(f_refAndSize[0]);
		vec.setYUVDelta(f_deltas);
		return vec;
	}
	
	public static void binarizeVector(final EncodingVector v,
			final CABAC encoder, final ContextModelManager manager,
			final BitWriter output) {
		final byte[] span = Protocol.getVectorSpanBytes(v.getSpanX(), v.getSpanY());
		final byte refAndSize = Protocol.getReferenceAndSizeByte(v.getReference(), v.getSize());
		byte[][] differences = null;
		
		try {
			differences = ProtocolBase.getDeltaMatrixBytes(v.getYUVDelta(), v.getSize());
		} catch (DCTCoefficientOutOfBoundsException e) {
			System.out.println("Size: " + v.getSize());
			e.printStackTrace();
		}
		
		encoder.encode(new BitReader(span[0]), output, manager.getModel(CodingType.VECTOR_SPAN_X));
		
		encoder.encode(new BitReader(span[1]), output, manager.getModel(CodingType.VECTOR_SPAN_Y));
		
		encoder.encode(new BitReader(refAndSize), output, manager.getModel(CodingType.VECTOR_REFERENCE));
		
		encoder.encode(new BitReader(differences[ColorManager.Y_INDEX]),
				output, manager.getModel(CodingType.RESIDUALS_Y));
		encoder.encode(new BitReader(differences[ColorManager.U_INDEX]),
				output, manager.getModel(CodingType.RESIDUALS_U));
		encoder.encode(new BitReader(differences[ColorManager.V_INDEX]),
				output, manager.getModel(CodingType.RESIDUALS_V));
	}
}
