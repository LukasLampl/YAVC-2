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

/**
 * The {@code CABAC} class is a simple implementation of a
 * Context Adaptive Binary Arithmetic Coder.
 * 
 * <p><b>Introduction to CABAC:</b><br>
 * <p><b>Arithmetic Coding as base:</b><br>
 * First of all the general idea of CABAC is to extend the Arithmetic Coding concept by
 * adding dynamic probabilities that update based on the read context, which makes CABAC
 * highly efficient.
 * <br>
 * Arithmetic Coding represents data in form of a interval with the range {@code [0; 1)}
 * (includes 0 but excludes 1). Instead of coding bits directly or using a code table
 * the entire sequence is represented as a number within the interval. The intervals are
 * based on the probability distribution of each possible symbol (Here 0 and 1 for binary).
 * <br>
 * Imagine a data sequence, where {@code 0} appear {@code 70%} of the time and {@code 1} occur
 * {@code 30%} of the time. The resulting probabilities will look like this:
 * <ul>
 * <li>{@code P(0)} = {@code 0.7}
 * <li>{@code P(1)} = {@code 0.3}
 * </ul>
 * Now the interval {@code [0; 1)} is subdivided based on these probabilities.
 * <ul>
 * <li>Interval for {@code 0} = {@code [0; 0.7)}
 * <li>Interval for {@code 1} = {@code [0.7, 1)}
 * </ul>
 * If the current bit that should be encoded is {@code 0} the range interval will be
 * narrowed down to {@code [0; 0.7)} and the process repeats until all symbols are encoded.
 * <br>
 * This means if the next symbol is {@code 0} again, the interval {@code [0; 0.7)} will result
 * in {@code [0; 0.49)} and so on.
 * </p>
 * 
 * <p><b>Context adaptability:</b><br>
 * Now after you have a basic idea of how Arithmetic Coding works, the only difference the CABAC is
 * the context adaptability. This means instead of having fixed symbol probability the probabilities
 * change based on the occurring symbols in the data sequence. This makes the whole coding process
 * much more efficient, since the more data the model sees, the higher the probability of a certain
 * symbol and thus a higher compression rate can be achieved.
 * </p>
 * 
 * <p><b>The finite problem:</b><br>
 * As more symbols are encoded, higher precision is required to accurately represent
 * increasingly narrow intervals. However, computers have limited precision, so infinite
 * accuracy isn't achievabl in practice. To address this, the CABAC algorithm uses a finite-precision
 * approach: bits that are guaranteed to be correct are shifted out to the output, while the
 * remaining interval is rescaled to maintain the original precision. This ensures the
 * encoding process remains accurate without requiring infinite computational resources.
 * </p>
 * 
 * <p><b>Working with Ranges:</b><br>
 * CABAC operates with two variables, {@code low} and {@code high}, which define the current range
 * during each iteration. Due to the finite precision limitation, the interval {@code [0, 1)} is
 * scaled to a fixed range, such as {@code [0, 65535)}. By halving the range, you can assign {@code 0}
 * to the lower half and {@code 1} to the upper half, initiating the encoding process.
 * <ul>
 * <li>When both {@code low} and {@code high} fall below {@code HALF_RANGE}, the output is always {@code 0},
 * as the entire interval lies in the {@code 0} range.
 * <li>When both {@code low} and {@code high} are above {@code HALF_RANGE}, the output is always {@code 1},
 * as the entire interval lies in the {@code 1} range.
 * </ul>
 * 
 * Eventually, {@code low} and {@code high} may fall into a state where {@code low} is below
 * {@code HALF_RANGE} and {@code high} is above it. At this point, the interval is divided into quarters,
 * each assigned a unique code. The algorithm then iteratively expands the quarters containing both {@code low}
 * and {@code high} until an interval is found that fits entirely between them. This final interval represents the
 * encoded result.
 * </p>
 * </p>
 * 
 * @author Lukas Lampl
 * @since 2.1.1 [QT_COMP]
 */
public class CABAC {
	/**
	 * The precision used for the finite CABAC coding.
	 */
	private static final int PRECISION = 16;
	
	/**
	 * The highest possible number defined by the precision.
	 */
	private static final int PRECISION_MAX = (0x1 << PRECISION) - 1;
	
	/**
	 * The midpoint of the full range.
	 */
	private static final int HALF_RANGE = PRECISION_MAX >> 1;
	
	/**
	 * Quarter part of the full range.
	 */
	private static final int QUARTER_RANGE = HALF_RANGE >> 1;
	
	/**
	 * Three quarter part of the full range.
	 */
	private static final int THREE_QUARTER_RANGE = 3 * QUARTER_RANGE;
	
	/**
	 * Mask of the most significant bit.
	 */
	private static final int MSB_MASK = 0x1 << (PRECISION - 1);

	/**
	 * Holds the number of underflows (E3 scaling violations).
	 */
	private int underflowCount = 0;
	
	/**
	 * Holds the currently used probability model.
	 */
	private BinaryContextModel model = null;

	/**
	 * The lower bound of the current interval.
	 */
	private int low = 0x00;
	
	/**
	 * The higher bound of the current interval.
	 */
	private int high = PRECISION_MAX;
	
	/**
	 * The mid pint between low and high estimated using the current
	 * probability model.
	 */
	private int mid = HALF_RANGE;

	/**
	 * Creates a new empty CABAC.
	 */
	public CABAC() {
		reset();
	}

	/**
	 * Resets the state of the CABAC encoder/decoder to its initial values.
	 * This includes resetting interval bounds, clearing the underflow count,
	 * and unassigning the current probability model.
	 */
	public void reset() {
		this.low = 0x00;
		this.mid = HALF_RANGE;
		this.high = PRECISION_MAX;
		this.underflowCount = 0;
		this.model = null;
	}

	/**
	 * Calculates the midpoint of the current interval based on the
	 * symbol probabilities from the provided context model. This
	 * midpoint determines how the interval is split for encoding or
	 * decoding the next bit.
	 */
	private void resolveModel() {
		int range = this.high - this.low;
		int freq_0 = this.model.getSymbolFrequency(0x00);
		int freq_1 = this.model.getSymbolFrequency(0x01);

		int midRange = range * freq_0 / (freq_0 + freq_1);
		this.mid = this.low + midRange;
	}

	/**
	 * Encodes a single bit by updating the interval bounds based on the bit value
	 * and the current probability model. The lower or upper bound is adjusted
	 * depending on whether the bit is 0 or 1.
	 * 
	 * @param bit	The bit to encode.
	 */
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

	/**
	 * Decodes a single bit based on the current interval and updates
	 * the interval bounds accordingly. The decoded bit is written
	 * to the output stream.
	 * 
	 * @param value		The current state of the decoder.
	 * @param output	The bit writer to write the decoded bit.
	 */
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

	/**
	 * Handles E3 underflow conditions by writing the inverse bits for
	 * all accumulated underflow events.
	 * 
	 * @param bit		Bit to invert and write.
	 * @param output	The bit writer to output the inverse bits.
	 */
	private void flushInverseBits(int bit, final BitWriter output) {
		bit = ~bit;

		for (int i = 0; i < this.underflowCount; i++) {
			output.write(bit);
		}

		this.underflowCount = 0;
	}

	/**
	 * Manages interval scaling during encoding to handle cases where the
	 * interval shrinks too much. It writes bits to the output and shifts
	 * the interval as needed to maintain precision.
	 * 
	 * @param output	The bit writer for the encoded bits.
	 */
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

	/**
	 * Handles interval scaling during decoding to adjust the state of
	 * the decoder. It reads additional bits as necessary to maintain
	 * precision.
	 * 
	 * @param value		The current state of the decoder.
	 * @param input		The bit reader to fetch new bits.
	 * 
	 * @return The adjusted decoder state.
	 */
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

	/**
	 * Flushes any remaining bits to the output stream after
	 * encoding is complete, ensuring the encoded sequence is
	 * properly terminated.
	 * 
	 * @param output	The bit writer to flush the bits.
	 */
	private void flushEncoder(final BitWriter output) {
		this.underflowCount += PRECISION - 1;

		if (this.low < QUARTER_RANGE) {
			output.write(0x00);
			flushInverseBits(0x00, output);
		} else {
			output.write(0x01);
			flushInverseBits(0x01, output);
		}
	}

	/**
	 * Encodes a complete bit stream using the CABAC algorithm.
	 * 
	 * @param input		The bit reader containing the input bitstream.
	 * @param output	The bit writer for the encoded bitstream.
	 * @param model		The probability model to use for context adaptation.
	 */
	public void encode(final BitReader input, final BitWriter output, final BinaryContextModel model) {
		this.model = model;

		while (input.hasRemainingBits()) {
			final int bit = input.read();
			encodeSymbol(bit);
			resolveEncodeScaling(output);
		}

		flushEncoder(output);
	}

	/**
	 * Encodes a bit using the CABAC algorithm.
	 * 
	 * @param bit		Bit to encode.
	 * @param output	The bit writer for the encoded bitstream.
	 * @param model		The probability model to use for context adaptation.
	 */
	public void encode(final int bit, final BitWriter output, final BinaryContextModel model) {
		this.model = model;
		encodeSymbol(bit);
		resolveEncodeScaling(output);
		flushEncoder(output);
	}

	/**
	 * Decodes an encoded bitstream back to the original sequence.
	 * 
	 * @param numberOfBits	The length of the original bit sequence (in bits).
	 * @param input			The bit reader with the encoded bitstream.
	 * @param output		The bit writer for the decoded bitstream.
	 * @param model			The probability model to use for context adaptation.
	 */
	public void decode(final int numberOfBits, final BitReader input, final BitWriter output,
			final BinaryContextModel model) {
		this.model = model;
		int value = readBits(input, PRECISION);

		for (int i = 0; i < numberOfBits; i++) {
			decodeSymbol(value, output);
			value = resolveDecodeScaling(value, input);
		}
	}

	/**
	 * Reads a specific number of bits from the input bitstream
	 * and returns the resulting integer value.
	 * 
	 * @param input			The bit reader to read bits from.
	 * @param numberOfBits	The number of bits to read.
	 * @return The integer representation of the read bits.
	 */
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
