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

/**
 * The {@code BitReader} class provides basic functionalities for reading a byte
 * array bit by bit.
 * 
 * @author Lukas Lampl
 * @since 2.1.1 [QT_COMP]
 */
public class BitReader {
	/**
	 * Holds the maximum bit shifting size of a byte.
	 */
	private final static int BYTE_SHIFT_SIZE = Byte.SIZE - 1;
	
	/**
	 * Holds the stream to read bit by bit.
	 */
	private final byte[] stream;
	
	/**
	 * Holds the stream length in bytes.
	 */
	private final int streamLength;
	
	/**
	 * The stream length in bits.
	 */
	private final int streamLengthBits;
	
	/**
	 * Index of the current byte from which a bit is acquired.
	 */
	private int currentByte = 0;
	
	/**
	 * Index of the bit to read in the current byte.
	 */
	private int currentBit = 0;
	
	/**
	 * Counter for how many bits were already read.
	 * This is used for keeping track whether the stream has
	 * been fully read or not.
	 */
	private int totalReadBits = 0;
	
	/**
	 * Creates a new {@code BitReader} with the given byte.
	 * Using this constructor the read can only provide one
	 * byte of data.
	 * 
	 * @param stream	The byte to read bit by bit.
	 */
	public BitReader(final byte stream) {
		this.stream = new byte[] {stream};
		this.streamLength = 1;
		this.streamLengthBits = Byte.SIZE;
	}
	
	/**
	 * Creates a new {@code BitReader} with the given stream.
	 * Using this constructor the maximum bits that can be read
	 * will be {@code streamLength * Byte.SIZE}.
	 * 
	 * @param stream	The stream to read bit by bit.
	 */
	public BitReader(final byte[] stream) {
		this.stream = stream;
		this.streamLength = stream.length;
		this.streamLengthBits = this.streamLength * Byte.SIZE;
	}
	
	/**
	 * Creates a new {@code BitReader} with the given stream.
	 * Using this constructor the maximum bits will be provided as
	 * a parameter.
	 * 
	 * @param stream	The stream to read bit by bit.
	 * @param totalBits	Number of bits to read in the stream.
	 */
	public BitReader(final byte[] stream, final int totalBits) {
		this.stream = stream;
		this.streamLength = stream.length;
		
		if (totalBits < 0) {
			throw new IllegalArgumentException("Can't read negative bits.");
		} else if (totalBits > (this.streamLength * Byte.SIZE)) {
			throw new IllegalArgumentException("Reading more bits than provided by byte stream is not possible.");
		}
		
		this.streamLengthBits = totalBits;
	}
	
	/**
	 * Reads a single bit out of the stream.
	 * 
	 * @return The read bit.
	 * 
	 * @throws IllegalStateException	When the stream has been fully read.
	 */
	public byte read() {
		if (!hasRemainingBits()) {
			throw new IllegalStateException("Stream is already fully read!");
		}
		
		byte bit = (byte)((this.stream[this.currentByte]
				>> (BYTE_SHIFT_SIZE - this.currentBit))
				& 0x01);
		this.currentBit++;
		this.totalReadBits++;
		
		if (this.currentBit == Byte.SIZE) {
			this.currentByte++;
			this.currentBit = 0;
		}
		
		return bit;
	}
	
	/**
	 * Returns whether the stream has been fully read or not.
	 * 
	 * @return
	 * <ul>
	 * <li>{@code true} - When there are bits left to read.
	 * <li>{@code false} - When all bits were read.
	 * </ul>
	 */
	public boolean hasRemainingBits() {
		return this.totalReadBits < this.streamLengthBits;
	}
}
