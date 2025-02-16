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

package app.io.coder.cabac;

import app.io.BitReader;
import app.io.BitWriter;
import app.utils.MathUtils;

public class BinaryArithmeticDecoder extends BinaryArithmeticCoder {
	private BitWriter output = new BitWriter();
	private BitReader input = null;
	
	private Interval currentInterval = new Interval(0, Integer.MAX_VALUE);
	
	private int value = 0;
	
	public void decode(final byte[] stream, final int numOfBits) {
		this.input = new BitReader(stream);
		this.value = readBits(MathUtils.min(numOfBits, Integer.SIZE - 1));
		int stepCounter = 0;
		
		while (!this.input.isFullyRead() && stepCounter < numOfBits) {
//			if (c % Byte.SIZE == 0) {
//				System.out.println("Bytes read: " + (c / Byte.SIZE));
//			}
			
			decode();
			stepCounter++;
		}
		
		for (int c = stepCounter; c < numOfBits; c++) {
			decode();
		}
	}
	
	private void decode() {
		decodeSymbol(0.5);
		normalize();
	}
	
	private int readBits(int numberOfBits) {
		int buffer = 0x00;
		
		for (int i = 0; i < numberOfBits; i++) {
			buffer = (buffer << 1) | this.input.read();
		}
		
		return buffer;
	}
	
	private void decodeSymbol(final double probabilityOfZero) {
		final Interval[] subdivisions = this.currentInterval.subdivide(probabilityOfZero);
		
		for (int i = 0; i < subdivisions.length; i++) {
			if (!subdivisions[i].isNElementOf(this.value)) {
				continue;
			}
			
			this.output.write(i);
			this.currentInterval = subdivisions[i];
		}
	}
	
	private void normalize() {
		if (this.currentInterval.isRangeReadyForOutput()) {
			this.currentInterval.shift();
			this.value <<= 1;
			
			if (!this.input.isFullyRead()) {
				this.value |= this.input.read();
			}
			
			this.value &= Interval.MSB_MASK;
		}
	}
	
	private void finish() {
		for (int i = 0; i < Integer.BYTES; i++) {
			byte MSB = this.currentInterval.getLowMSB();
			this.output.writeByte(MSB);
			this.currentInterval.shift();
		}
	}
	
	public byte[] getOutput() {
		finish();
		return this.output.toByteArray();
	}
}
