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
aint with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package app.io.coder.cabac;

import app.io.BitReader;
import app.io.BitWriter;
import app.utils.MathUtils;

public class CABAC {
	private static final int PRECISION = 16;
	private static final int PRECISION_MAX = 0xFFFF;
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

	private void calculateMidpoint() {
		final int freq_0 = this.model.getSymbolFrequency(0x00);
		final int freq_1 = this.model.getSymbolFrequency(0x01);
		final int total_freqs = freq_0 + freq_1;
		final int range = this.high - this.low;

		this.mid = this.low + (range * freq_0 / total_freqs);
	}

	public void encode(final BitReader input, final BitWriter output, final BinaryContextModel model) {
		this.model = model;

		while (!input.isFullyRead()) {
			encodeSymbol(input.read());
			normalizeEncoder(output);
		}
		
		flushEncoder(output);
	}

	private void encodeSymbol(final int bit) {
		if (bit == 0x01) {
			this.low = this.mid;
		} else {
			this.high = this.mid;
		}

		this.model.incrementSymbolFrequency(bit);
		calculateMidpoint();
	}

	private void decodeSymbol(final int code, final BitWriter output) {
		calculateMidpoint();

		if (code >= this.low && code <= this.mid) {
			this.high = this.mid;
			this.model.incrementSymbolFrequency(0x00);
			output.write(0x00);
		} else if (code > this.mid && code <= this.high) {
			this.low = this.mid;
			this.model.incrementSymbolFrequency(0x01);
			output.write(0x01);
		}
	}

	private void normalizeEncoder(final BitWriter output) {
		final int quarter = this.mid >> 1;
		final int three_quarter = ((this.high - this.low) - (this.mid)) >> 1;

		while (true) {
			if ((this.high & MSB_MASK) == (this.low & MSB_MASK)) {
				final int msb = (this.high & MSB_MASK) >> (PRECISION - 1);
				this.low -= HALF_RANGE * msb + msb;
				this.high -= HALF_RANGE * msb + msb;
				output.write(msb);
				flushUnderflow(msb, output);
			} else if (this.low > QUARTER_RANGE && this.high < THREE_QUARTER_RANGE) {
				this.low -= QUARTER_RANGE;
				this.high -= QUARTER_RANGE;
				this.underflowCount++;
			} else {
				break;
			}

			this.high = ((this.high << 0x01) & PRECISION_MAX) | 0x01;
			this.low = ((this.low << 0x01) & PRECISION_MAX) | 0x00;
		}
	}

	private int normalizeDecoder(final int code, final BitReader input) {
		int temp = code;

		final int quarter = this.mid >> 1;
		final int three_quarter = ((this.high - this.low) - (this.mid)) >> 1;
		int bit = 0x00;

		while (true) {
			if (this.low > HALF_RANGE) {
				this.low -= HALF_RANGE;
				this.high -= HALF_RANGE;
				temp -= HALF_RANGE;
			} else if (this.low > QUARTER_RANGE && this.high < THREE_QUARTER_RANGE) {
				this.low -= QUARTER_RANGE;
				this.high -= QUARTER_RANGE;
				temp -= QUARTER_RANGE;
			} else {
				break;
			}

			if (!input.isFullyRead()) {
				bit = input.read();
			} else {
				bit = 0x00;
			}

			this.high = ((this.high << 0x01) & PRECISION_MAX) | 0x01;
			this.low = ((this.low << 0x01) & PRECISION_MAX) | 0x00;
			temp = ((temp << 0x01) & PRECISION_MAX) | bit;
		}

		return temp;
	}
	
	private void flushEncoder(final BitWriter output) {
		this.underflowCount++;
		
		if (this.low < QUARTER_RANGE) {
			output.write(0x00);
			flushUnderflow(0x00, output);
		} else {
			output.write(0x01);
			flushUnderflow(0x01, output);
		}
	}

	private void flushUnderflow(int bit, final BitWriter output) {
		bit = bit == 0x00 ? 0x01 : 0x00;

		for (int i = 0; i < this.underflowCount; i++) {
			output.write(bit);
		}

		this.underflowCount = 0;
	}

	public void decode(final int numberOfBits, final BitReader input, final BitWriter output,
			final BinaryContextModel model) {
		this.model = model;
		int code = readBits(MathUtils.min(numberOfBits, PRECISION), input);

		while (!input.isFullyRead()) {
			decodeSymbol(code, output);
			code = normalizeDecoder(code, input);
		}
	}

	private int readBits(final int numberOfBits, final BitReader reader) {
		int code = 0x00;

		for (int i = 0; i < numberOfBits; i++) {
			code = (code << 1) | reader.read();
		}

		return code;
	}
}
