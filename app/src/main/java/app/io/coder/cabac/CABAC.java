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

public class CABAC {
	private static final int PRECISION = 8;
	private static final int PRECISION_MAX = (0x1 << PRECISION) - 1;
	private static final int HALF_RANGE = PRECISION_MAX >> 1;
	private static final int QUARTER_RANGE = HALF_RANGE >> 1;
	private static final int THREE_QUARTER_RANGE = 3 * QUARTER_RANGE;
	private static final int MSB_MASK = 0x1 << (PRECISION - 1);

	private int underflowCount = 0;
	private BinaryContextModel model = null;

	private int low = 0x00;
	private int high = PRECISION_MAX;
	private int mid = HALF_RANGE;

	public CABAC() {
		reset();
	}

	public void reset() {
		this.low = 0x00;
		this.mid = HALF_RANGE;
		this.high = PRECISION_MAX;
		this.underflowCount = 0;
		this.model = null;
	}

	private void resolveModel() {
		int range = this.high - this.low;
		int freq_0 = this.model.getSymbolFrequency(0x00);
		int freq_1 = this.model.getSymbolFrequency(0x01);

		int midRange = range * freq_0 / (freq_0 + freq_1);
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

	private void decodeSymbol(final int value, final BitWriter output) {
		resolveModel();

		if (value >= this.low && value <= this.mid) {
			this.high = this.mid;
			this.model.incrementSymbolFrequency(0x00);
			output.write(0x00);
		} else if (value > this.mid && value <= this.high) {
			this.low = this.mid + 1;
			this.model.incrementSymbolFrequency(0x01);
			output.write(0x01);
		} else {
			throw new IllegalStateException("Unknown state for CABAC.");
		}
	}

	private void flushInverseBits(int bit, final BitWriter output) {
		bit = ~bit;

		for (int i = 0; i < this.underflowCount; i++) {
			output.write(bit);
		}

		this.underflowCount = 0;
	}

	private void resolveEncodeScaling(final BitWriter output) {
		while (true) {
			if ((this.high & MSB_MASK) == (this.low & MSB_MASK)) {
				final int MSB = (int) (this.high & MSB_MASK) >> (PRECISION - 1);
				this.low -= HALF_RANGE * MSB + MSB;
				this.high -= HALF_RANGE * MSB + MSB;
				output.write(MSB);
				flushInverseBits(MSB, output);
			} else if (this.high <= THREE_QUARTER_RANGE && this.low > QUARTER_RANGE) {
				this.high -= QUARTER_RANGE + 1;
				this.low -= QUARTER_RANGE + 1;
				this.underflowCount++;
			} else {
				break;
			}

			this.high = ((this.high << 0x01) & PRECISION_MAX) | 0x01;
			this.low = ((this.low << 0x01) & PRECISION_MAX) | 0x00;
		}
	}

	private int resolveDecodeScaling(final int value, final BitReader input) {
		int bit = 0x00;
		int temp = value;

		while (true) {
			if (this.high <= HALF_RANGE) {
			} else if (this.low > HALF_RANGE) {
				this.high -= HALF_RANGE + 1;
				this.low -= HALF_RANGE + 1;
				temp -= HALF_RANGE + 1;
			} else if (this.high <= THREE_QUARTER_RANGE && this.low > QUARTER_RANGE) {
				this.high -= QUARTER_RANGE + 1;
				this.low -= QUARTER_RANGE + 1;
				temp -= QUARTER_RANGE + 1;
			} else {
				break;
			}

			if (input.hasRemainingBits()) {
				bit = input.read();
			}

			this.high = ((this.high << 0x01) & PRECISION_MAX) | 0x01;
			this.low = ((this.low << 0x01) & PRECISION_MAX) | 0x00;
			temp = ((temp << 0x01) & PRECISION_MAX) | bit;
		}

		return temp;
	}

	private void flushEncoder(final BitWriter output) {
		this.underflowCount += PRECISION - 1;

		if (this.low < QUARTER_RANGE) {
			output.write(0x00);
			flushInverseBits(0x00, output);
		} else {
			output.write(0x01);
			flushInverseBits(0x01, output);
		}
//		System.out.println(this.low);
//		String low_str = "";
//		String flush_str = "";
//		
//		for (int i = Integer.SIZE - 1; i >= 0; i--) {
//			int bit = this.low >> i & 0x01;
//			low_str += String.valueOf(bit);
//			
//			if (i % 8 == 0) {
//				low_str += " ";
//			}
//		}
//		
//		System.out.println("Bin of Low: " + low_str);
//		
//		final int offset = Integer.numberOfLeadingZeros(this.low);
//		
//		for (int i = Integer.SIZE - offset; i >= 0; i--) {
//			final int bit = (this.low >> i) & 0x01;
//			flush_str += String.valueOf(bit);
//			output.write(bit);
//		}
//		
//		System.out.println("Flush: " + flush_str);
	}

	public void encode(final BitReader input, final BitWriter output, final BinaryContextModel model) {
		this.model = model;

		while (input.hasRemainingBits()) {
			final int bit = input.read();
			encodeSymbol(bit);
			resolveEncodeScaling(output);
		}

		flushEncoder(output);
	}

	public void encode(final int bit, final BitWriter output, final BinaryContextModel model) {
		this.model = model;
		encodeSymbol(bit);
		resolveEncodeScaling(output);
		flushEncoder(output);
	}

	public void decode(final int numberOfBits, final BitReader input, final BitWriter output,
			final BinaryContextModel model) {
		this.model = model;
		int value = readBits(input, PRECISION);

		for (int i = 0; i < numberOfBits; i++) {
			decodeSymbol(value, output);
			value = resolveDecodeScaling(value, input);
		}
	}

	private int readBits(final BitReader input, final int numberOfBits) {
		int value = 0x00;
		int bit = 0x00;

		for (int i = 0; i < numberOfBits; i++) {
			if (input.hasRemainingBits()) {
				bit = input.read();
			}

			value <<= 0x01;
			value |= bit;
		}

		return value;
	}
}
