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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import app.utils.MathUtils;

public class TestBitWriter {
	private Random random = new Random();
	
	@Test
	public void test001() {
		// BIN: 0100 1101
		byte[] bytes = new byte[] {0x4D};
		
		BitWriter writer = new BitWriter();
		writer.write(0x00);
		writer.write(0x01);
		writer.write(0x00);
		writer.write(0x00);
		writer.write(0x01);
		writer.write(0x01);
		writer.write(0x00);
		writer.write(0x01);
		
		byte[] output = writer.toByteArray();
		
		assertEquals(bytes.length, output.length);
		assertArrayEquals(bytes, output);
	}
	
	@Test
	public void test002() {
		// BIN: 0111 1111 0101 0110
		byte[] bytes = new byte[] {0x7F, 0x56};
		
		BitWriter writer = new BitWriter();
		writer.write(0x00);
		writer.write(0x01);
		writer.write(0x01);
		writer.write(0x01);
		writer.write(0x01);
		writer.write(0x01);
		writer.write(0x01);
		writer.write(0x01);
		
		writer.write(0x00);
		writer.write(0x01);
		writer.write(0x00);
		writer.write(0x01);
		writer.write(0x00);
		writer.write(0x01);
		writer.write(0x01);
		writer.write(0x00);
		
		byte[] output = writer.toByteArray();
		
		assertEquals(bytes.length, output.length);
		assertArrayEquals(bytes, output);
	}
	
	@Test
	public void test003() {
		final int steps = 65535;
		
		for (int i = 0; i < steps; i++) {
			final byte[] bytes = generateRandomBytes(MathUtils.round(this.random.nextDouble() * 4096));
			final byte[] output = generateBitWriter(bytes).toByteArray();
			assertEquals(bytes.length, output.length);
			assertArrayEquals(bytes, output);
		}
	}
	
	private BitWriter generateBitWriter(final byte[] stream) {
		BitWriter writer = new BitWriter();
		
		for (final byte b : stream) {
			for (int i = 0; i < Byte.SIZE; i++) {
				writer.write((b >> ((Byte.SIZE - 1) - i)) & 0x01);
			}
		}
		
		return writer;
	}
	
	private byte[] generateRandomBytes(final int length) {
		byte[] bytes = new byte[length];
		this.random.nextBytes(bytes);
		return bytes;
	}
}
