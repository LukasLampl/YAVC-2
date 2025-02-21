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

public class CABAC {
	private static final int PRECISION = 16;
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
	
	private void calculateMidpoint() {
		final int freq_0 = this.model.getSymbolFrequency(0x00);
		final int freq_1 = this.model.getSymbolFrequency(0x01);
		final int total_freqs = freq_0 + freq_1;
		final int range = this.high - this.low;
		
		this.mid = this.low + (range * freq_0 / total_freqs);
	}
	
	public void encode(final BitReader input, final BitWriter output) {
		while (!input.isFullyRead()) {
			encodeSymbol(input.read(), output);
		}
	}
	
	private void encodeSymbol(final int bit, final BitWriter output) {
		if (bit == 0x01) {
			this.low = this.mid;
		} else {
			this.high = this.mid;
		}
		
		calculateMidpoint();

		while (true) {
			if (this.low > this.mid && this.high > this.mid) {
				this.low = ((this.low - this.mid) << 1) & PRECISION_MAX;
				this.high = ((this.high - this.mid) << 1) & PRECISION_MAX;
				output.write(0x01);
			} else if (this.low < this.mid && this.high < this.mid) {
				this.low = (this.low << 1) & PRECISION_MAX;
				this.high = (this.high << 1) & PRECISION_MAX;
				output.write(0x00);
			} else {
				break;
			}
		}
	}
}
