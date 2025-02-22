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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The {@code BitWriter} class provides basic functionalities
 * for writing bit level arrays that can be turned to a byte array.
 * 
 * @author Lukas Lampl
 * @since 2.1.1 [QT_COMP]
 */
public class BitWriter {
	/**
	 * Holds the output stream in which to write a fully constructed byte.
	 */
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	
	/**
	 * The current written byte.
	 * The byte is written by shifting bits one by one onto the byte.
	 */
	private byte currentByte = 0;
	
	/**
	 * Holds the current bit position in the current byte.
	 */
	private int currentBit = 0;
	
	/**
	 * Counter for the total amount of written bits.
	 */
	private int totalBits = 0;
	
	/**
	 * Writes a single bit into the BitWriter.
	 * Before Writing the bit is masked using {@code 0x01} to ensure that
	 * the value is a single bit.
	 * 
	 * @param bit	The bit to write.
	 */
	public void write(byte bit) {
		bit &= 0x01;
		this.currentByte <<= 1;
		this.currentByte |= bit;
		this.currentBit++;
		this.totalBits++;
		
		if (this.currentBit == Byte.SIZE) {
			flush();
		}
	}
	
	/**
	 * Writes a single bit into the BitWriter.
	 * 
	 * @param bit	The bit to write.
	 * @see #write(byte)
	 */
	public void write(final int bit) {
		write((byte)bit);
	}
	
	/**
	 * Writes a whole byte into the BitWriter.
	 * 
	 * @param b	The byte to write.
	 */
	public void writeByte(final byte b) {
		for (int i = 0; i < Byte.SIZE; i++) {
			write((b >> i) & 0x01);
		}
	}
	
	/**
	 * Writes the remaining bits to the output stream
	 * to ensure that all fed in bits are present.
	 */
	private void flush() {
		if (this.currentBit > 0) {
			this.baos.write(this.currentByte);
			this.currentBit = 0;
			this.currentByte = 0x00;
		}
	}
	
	/**
	 * Converts the written bits into a byte array form.
	 * 
	 * @return The converted bits in byte array form.
	 */
	public byte[] toByteArray() {
		flush();
		
		try {
			this.baos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return this.baos.toByteArray();
	}
	
	/**
	 * Gets the first byte of the written bits.
	 * 
	 * @return The first byte of the written bits.
	 */
	public byte getFirstByte() {
		return toByteArray()[0];
	}
	
	/**
	 * Gets the total amount of written bits in the BitWriter.
	 * 
	 * @return The number of written bits.
	 */
	public int getTotalBits() {
		return this.totalBits;
	}
}
