package utils;

import java.util.ArrayList;

import interprediction.Vector;

public class Protocol {
	public static final int VECTOR_HEADER_LENGTH = 7;
	public static final byte VECTOR_START = (byte)0x01;
	
	public static byte getDCTCoeffByte(double coeff) {
		byte result = (byte)((int)Math.abs(coeff) & 0x7F);
		
		if (coeff < 0) {
			result |= (1 << 7);
		}
		
		return (byte)(result & 0xFF);
	}
	
	public static double getDCTCoeff(byte coeff) {
		int result = coeff & 0x7F;
		
		if (((coeff >> 7) & 0x01) == 1) {
			result *= -1;
		}
		
		return (double)result;
	}
	
	public static byte[] getVectorSpanBytes(int spanX, int spanY) {
		byte bytespany = (byte)((int)Math.abs(spanY) & 0x7F);
		byte bytespanx = (byte)((int)Math.abs(spanX) & 0x7F);
		
		if (bytespany > 127 || bytespanx > 127) System.err.println("Span to big (|Span| > 127)!");
		
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

		if (((span >> 7) & 0x1) == 1) {
			res *= -1;
		}
		
		return res;
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
		int size = 0;
		
		for (Vector v : vecs) {
			int refSize = v.getSize();
			size += Protocol.VECTOR_HEADER_LENGTH;
			size += (refSize * refSize) + (2 * ((refSize / 2) * (refSize / 2)));
		}
		
		return size;
	}
	
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
}
