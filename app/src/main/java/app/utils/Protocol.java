package app.utils;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import app.decoder.VectorConverter;
import app.exceptions.CorruptedFileException;
import app.exceptions.WrongBlockAssignedException;
import app.interprediction.ListManager;
import app.interprediction.Vector;

public class Protocol {
	public static final int SIZE_OF_INT = 4;
	public static final int META_DATA_LEN = 3 * SIZE_OF_INT;
	
	public static final int VECTOR_HEADER_LENGTH = 7;
	public static final int VECTOR_SIZE_CHECK_LENGTH = SIZE_OF_INT;
	public static final int RAW_BLOCK_HEADER_LENGTH = 5;
	public static final int RAW_BLOCK_SIZE_CHECK_LENGTH = SIZE_OF_INT;
	
	public static final byte VECTOR_INDICATOR = (byte)0xF0;
	public static final byte RAW_BLOCK_INDICATOR = (byte)0xA0;
	
	public static byte getDCTCoeffByte(double coeff) {
		byte result = (byte)((int)Math.abs(coeff) & 0x7F);
		
		if (coeff < 0) {
			result |= (1 << 7);
		}
		
		return (byte)(result & 0xFF);
	}
	
	public static final double getDCTCoeff(byte coeff) {
		int result = coeff & 0x7F;
		return (coeff & 0x80) != 0 ? -result : result;
	}
	
	public static byte[] getVectorSpanBytes(int spanX, int spanY) throws IllegalArgumentException {
		if (spanX > 127 || spanY > 127
			|| spanX < -127 || spanY < -127) {
			throw new IllegalArgumentException("Span has to be in this boundary: -127 >= span <= 127.");
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
	
	public static int getVectorSpanInt(byte span) {
		int res = span & 0x7F;
		return (span & 0x80) != 0 ? -res : res;
	}
	
	/*
	 * Purpose: Get the byte for reference and size of the vector
	 * Return Type: byte => Byte containing reference and size
	 * Params: int reference => Reference of the vector;
	 * 			int size => Size of the vector (size in px)
	 * Function: One byte is splitted into 2 parts, each with 4 bits. The upper part is the storage
	 * 			place for the reference, while the lower part is for the size. The reference cannot
	 * 			exceed 7, since it would get bigger than 4 bits. If the number is negative, a sign is
	 * 			written to the first bit. The size would be too big for 4 bits, thats why the size
	 * 			is only represented by numbers from 1 to 6. If we'd do an example for reference 4
	 * 			and size 64. First reference is written into the upper part of the byte.
	 * 		-> First: 0000 0100 => 0100 0000 (Bitshifting 4 to the left)
	 * 		-> Next: 64 = 5 (BIN: 101)
	 * 		-> Now combine both: 0100 0000 | 0000 0101 => 0100 0101 
	 * 		-> Finally add the Coding offset to the result
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
	
	/*
	 * Purpose: Get the bytes for the position of the vector (max. 65536)
	 * Return Type: byte[] => Array of the position in bytes
	 * Params: int pos => position to write
	 * Function: First the offset is added to the original pos, since adding the offset afterwards
	 * 			might exceed the limit of an byte (255). The position gets splitted into two bytes
	 * 			containing the position.
	 * 			Example for position 21201 (BIN: 0101 0010 1101 0001). The number in BIN has more than
	 * 			8 digits, so the last digits are written into a byte. The second part, that is in the
	 * 			front is also put into a byte. To extract the first 8 bits, the number is shifted 8 to
	 * 			the right. To sum it all up, the first byte contains 0101 0010 and the second contains
	 * 			1101 0001 (for this example).
	 */
	public static byte[] getPositionBytes(int pos) {
		if (pos > 65536) {
			throw new IllegalArgumentException("Position of vector exceeds maximum limit of 65536");
		} else if (pos < 0) {
			throw new IllegalArgumentException("Position of vector is smaller than 0 (out of frame)");
		}
		
		return new byte[] {(byte)((pos >> 8) & 0xFF), (byte)(pos & 0xFF)};
	}
	
	public static int getPosition(byte c1, byte c2) {
		int res = (c1 & 0xFF) << 8 | (c2 & 0xFF);
		return res;
	}
	
	public static int calculateSize(ArrayList<Vector> vecs) {
		int size = Protocol.VECTOR_SIZE_CHECK_LENGTH;
		
		for (Vector v : vecs) {
			int refSize = v.getSize();
			size += Protocol.VECTOR_HEADER_LENGTH;
			size += (refSize * refSize) + (2 * ((refSize / 2) * (refSize / 2)));
		}
		
		return size;
	}
	
	/**
	 * Converts the absolute color difference of the vector to a byte matrix which can be then encoded.
	 * 
	 * <p><b>Important:</b><br>
	 * The function does <u>not</u> check for out of bounds values and thus values > 127 and < -127
	 * are incorrect!</p>
	 * 
	 * @param absoluteDifference	The absolute color difference.
	 * @param size					The size of the vector.
	 * @return A matrix representation of the absolute color difference.
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
					YBytes[YIndex++] = getDCTCoeffByte(coeffGroup[0][x][y]);
				}
			}
			
			for (int x = 0; x < halfFrac; x++) {
				for (int y = 0; y < halfFrac; y++) {
					UBytes[UIndex++] = getDCTCoeffByte(coeffGroup[1][x][y]);
					VBytes[VIndex++] = getDCTCoeffByte(coeffGroup[2][x][y]);
				}
			}
		}
		
		return new byte[][] {YBytes, UBytes, VBytes};
	}
	
	public static byte[] getIntBytes(int integer) {
		byte[] arr = new byte[4];
		arr[0] = (byte)((integer >> 24) & 0xFF);
		arr[1] = (byte)((integer >> 16) & 0xFF);
		arr[2] = (byte)((integer >> 8) & 0xFF);
		arr[3] = (byte)(integer & 0xFF);
		return arr;
	}
	
	public static int getIntFromBytes(byte[] data) {
		int num = 0;
		num |= (data[0] & 0xFF) << 24;
		num |= (data[1] & 0xFF) << 16;
		num |= (data[2] & 0xFF) << 8;
		num |= data[3] & 0xFF;
		return num;
	}
	
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
	
	public static byte[] getVectorBytes(ArrayList<Vector> vecs) {
		if (vecs == null) {
			throw new NullPointerException("No vectors were passed for writing.");
		}
		
		int size = Protocol.calculateSize(vecs);
		int currentIndex = 0;
		byte[] data = new byte[size];
		writeBytesToByteArray(Protocol.getIntBytes(vecs.size()), data, currentIndex);
		currentIndex += Protocol.VECTOR_SIZE_CHECK_LENGTH;
		
		for (Vector v : vecs) {
			currentIndex += writeSingleVectorToByteArray(v, currentIndex, data);
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
	
	public static void getVectors(byte[] data, ListManager<Vector> vectorListManager, boolean singleThread) throws CorruptedFileException, WrongBlockAssignedException {
		if (data.length <= 1) {
			return;
		}
		
		//  LAYOUT:
		//  POSX ⊥ POSY ⊥ SPANX ⊥ SPANY ⊥ REFERENCE << 4 | SIZE ⊥ DIFFERENCE
		// ^_____________________________________________________^
		//                      = 7 Bytes offset
		byte[] lenOfVecs = {data[0], data[1], data[2], data[3]};
		int estimatedLength = Protocol.getIntFromBytes(lenOfVecs);
		ArrayList<Integer> indexesOfVectors = new ArrayList<Integer>();
		precalculateVectorIndexes(data, indexesOfVectors);
		
		VectorConverter converter = new VectorConverter(data, indexesOfVectors, vectorListManager, singleThread);
		converter.start();
		converter.awaitTermination();
		
		if (vectorListManager.getList().size() != estimatedLength) {
			throw new CorruptedFileException("The amount of the read-in vectors appears to be unequal to the written vectors.");
		}
	}
	
	private static void precalculateVectorIndexes(byte[] data, List<Integer> indexesOfVectors) {
		int i = Protocol.VECTOR_SIZE_CHECK_LENGTH;
		
		while (i < data.length) {
			indexesOfVectors.add(i);
			int[] refAndSize = Protocol.getReferenceAndSizeInt(data[i + 6]);
			int size = refAndSize[1];
			//Length of the vector diffs
			i += ((size * size) + 2 * ((size / 2) * (size / 2))) + Protocol.VECTOR_HEADER_LENGTH;
		}
	}
	
	public static byte[] getLengthBytesOfFrame(List<Integer> lengths) {
		int estimatedSize = lengths.size() * Protocol.SIZE_OF_INT + Protocol.SIZE_OF_INT;
		byte[] data = new byte[estimatedSize];
		int currentIndex = 0;
		
		byte[] lenOfIndexes = Protocol.getIntBytes(lengths.size());
		writeBytesToByteArray(lenOfIndexes, data, currentIndex);
		currentIndex += Protocol.SIZE_OF_INT;
		
		for (int i : lengths) {
			byte[] index = Protocol.getIntBytes(i);
			writeBytesToByteArray(index, data, currentIndex);
			currentIndex += Protocol.SIZE_OF_INT;
		}
		
		return data;
	}
	
	public static void setLengthsOfEachFramePart(byte[] lengthStream, List<Integer> lengthList) {
		byte[][] data = Protocol.splitArrayEvenly(lengthStream, Protocol.SIZE_OF_INT);
		
		for (byte[] byteNum : data) {
			int length = Protocol.getIntFromBytes(byteNum);
			lengthList.add(length);
		}
	}
	
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
		MacroBlock block = new MacroBlock(new Point(posX, posY), size, true);
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
	
	private static void writeBytesToByteArray(byte[] bytes, byte[] arr, int index) {
		for (int i = 0; i < bytes.length; i++) {
			arr[index++] = bytes[i];
		}
	}
}
