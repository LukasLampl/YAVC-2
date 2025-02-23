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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import app.utils.MathUtils;

public class TestBitReader {
	private Random random = new Random();
	
	@Test
	public void test001() {
		final byte[] stream = {0x7F, 0x01};
		final BitReader reader = new BitReader(stream);
		
		for (final byte b : stream) {
			final byte read = readFullByte(reader);
			assertEquals(b, read);
		}
	}
	
	@Test
	public void test002() {
		final byte[] stream = {0x00, (byte)0xFF};
		final BitReader reader = new BitReader(stream);
		
		for (final byte b : stream) {
			final byte read = readFullByte(reader);
			assertEquals(b, read);
		}
	}
	
	@Test
	public void test003() {
		final byte[] stream = {0x00, 0x69};
		final BitReader reader = new BitReader(stream);
		
		for (final byte b : stream) {
			final byte read = readFullByte(reader);
			assertEquals(b, read);
		}
	}
	
	@Test
	public void test004() {
		final BitWriter writer = new BitWriter();
		writer.write(0x00);
		writer.write(0x00);
		writer.write(0x01);
		
		final byte[] stream = writer.toByteArray();
		
		final BitReader reader = new BitReader(stream);
		
		final int bit_1 = reader.read();
		final int bit_2 = reader.read();
		final int bit_3 = reader.read();
		assertEquals(0x00, bit_1);
		assertEquals(0x00, bit_2);
		assertEquals(0x01, bit_3);
	}
	
	@Test
	public void test005() {
		final int steps = 65535;
		
		for (int i = 0; i < steps; i++) {
			final byte[] stream = generateRandomBytes(MathUtils.round(this.random.nextDouble() * 4096));
			final BitReader reader = new BitReader(stream);
			
			for (final byte b : stream) {
				final byte read = readFullByte(reader);
				assertEquals(b, read);
			}
		}
	}
	
	private byte readFullByte(final BitReader reader) {
		byte b = 0x00;
		
		for (int i = 0; i < Byte.SIZE; i++) {
			b = (byte)((b << 1) | reader.read());
		}
		
		return b;
	}
	
	private byte[] generateRandomBytes(final int length) {
		byte[] bytes = new byte[length];
		this.random.nextBytes(bytes);
		return bytes;
	}
}
