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

public class CABAC {
	private static final long PRECISION = 16;
	private static final long PRECISION_MAX = (0x1L << PRECISION) - 1;
	private static final long HALF_RANGE = PRECISION_MAX >> 1;
	private static final long QUARTER_RANGE = HALF_RANGE >> 1;
	private static final long THREE_QUARTER_RANGE = 3 * QUARTER_RANGE;
	private static final long MSB_MASK = 0x1L << (PRECISION - 1);

	private int underflowCount = 0;
	private BinaryContextModel model = null;
	
	private long low = 0x00L;
	private long high = PRECISION_MAX;
	private long mid = HALF_RANGE;
	
	public CABAC() {
		reset();
	}
	
	public void reset() {
		this.low = 0x00L;
		this.mid = HALF_RANGE;
		this.high = PRECISION_MAX;
		this.underflowCount = 0;
		this.model = null;
	}
	
	private void resolveModel() {
		long range = this.high - this.low;
		int freq_0 = this.model.getSymbolFrequency(0x00);
		int freq_1 = this.model.getSymbolFrequency(0x01);
		
		long midRange = range * freq_0 / (freq_0 + freq_1);
		this.mid = this.low + midRange;
	}
	
	private void encodeSymbol(int bit) {
		resolveModel();
		bit &= 0x01;
		
		if (bit == 0x01) {
			this.low = this.mid + 1;
		} else {
			this.high = this.mid;
		}
		
		this.model.incrementSymbolFrequency(bit);
	}
	
	private void decodeSymbol(final long value, final BitWriter output) {
		resolveModel();
		
		if (value >= this.low && value <= this.mid) {
			this.high = this.mid;
			this.model.incrementSymbolFrequency(0x00);
			output.write(0x00);
		} else if (value > this.mid && value <= this.high) {
			this.low = this.mid + 1;
			this.model.incrementSymbolFrequency(0x01);
			output.write(0x01);
		}
	}
	
	private void flushInverseBits(int bit, final BitWriter output) {
		bit = bit == 0x00 ? 0x01 : 0x00;
		
		for (int i = 0; i < this.underflowCount; i++) {
			output.write(bit);
		}
		
		this.underflowCount = 0;
	}
	
	private void resolveEncodeScaling(final BitWriter output) {
		while (true) {
			if ((this.high & MSB_MASK) == (this.low & MSB_MASK)) {
				final int MSB = (int)(this.high & MSB_MASK) >> (PRECISION - 1);
				this.low -= HALF_RANGE * MSB + MSB;
				this.high -= HALF_RANGE * MSB + MSB;
				output.write(MSB);
				flushInverseBits(MSB, output);
			} else if (this.high <= THREE_QUARTER_RANGE
					&& this.low > QUARTER_RANGE) {
				this.high -= QUARTER_RANGE + 1;
				this.low -= QUARTER_RANGE + 1;
				this.underflowCount++;
			} else {
				break;
			}
			
			this.high = ((this.high << 0x01L) & PRECISION_MAX) | 0x01L;
			this.low = ((this.low << 0x01L) & PRECISION_MAX) | 0x00L;
		}
	}
	
	private long resolveDecodeScaling(long value, final BitReader input, final BitWriter output) {
		int bit = 0x00;
		long temp = value;
		
		while (true) {
			if (this.high <= HALF_RANGE) {
			} else if (this.low > HALF_RANGE) {
				this.high -= HALF_RANGE + 1;
				this.low -= HALF_RANGE + 1;
				temp -= HALF_RANGE + 1;
			} else if (this.high <= THREE_QUARTER_RANGE
					&& this.low > QUARTER_RANGE) {
				this.high -= QUARTER_RANGE + 1;
				this.low -= QUARTER_RANGE + 1;
				temp -= QUARTER_RANGE + 1;
			} else {
				break;
			}
			
			if (!input.isFullyRead()) {
				bit = input.read();
			} else {
				bit = 0x00;
			}
			
			this.high = ((this.high << 0x01L) & PRECISION_MAX) | 0x01L;
			this.low = ((this.low << 0x01L) & PRECISION_MAX) | 0x00L;
			temp = ((temp << 0x01L) & PRECISION_MAX) | bit;
		}
		
		return temp;
	}
	
	private void flushEncoder(final BitWriter writer) {
		this.underflowCount++;
		
		if (this.low < QUARTER_RANGE) {
			writer.write(0x00);
			flushInverseBits(0x00, writer);
		} else {
			writer.write(0x01);
			flushInverseBits(0x01, writer);
		}
	}
	
	public void encode(final BitReader input, final BitWriter output,
			final BinaryContextModel model) {
		this.model = model;
		
		while (!input.isFullyRead()) {
			final int bit = input.read();
			encodeSymbol(bit);
			resolveEncodeScaling(output);
		}
		
		flushEncoder(output);
	}
	
	public void encode(final int bit, final BitWriter output,
			final BinaryContextModel model) {
		this.model = model;
		encodeSymbol(bit);
		resolveEncodeScaling(output);
		flushEncoder(output);
	}
	
	public void decode(final int numberOfBits, final BitReader input,
			final BitWriter output, final BinaryContextModel model) {
		this.model = model;
		long value = readBits(input, MathUtils.min(numberOfBits, PRECISION));
		
		for (int i = 0; i < numberOfBits; i++) {
			decodeSymbol(value, output);
			value = resolveDecodeScaling(value, input, output);
		}
	}
	
	private long readBits(final BitReader input, final long numberOfBits) {
		long value = 0x00;
	
		for (int i = 0; i < numberOfBits && !input.isFullyRead(); i++) {
			value <<= 0x01L;
			value |= input.read();
		}
		
		return value;
	}
}
