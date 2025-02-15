package app.io.coder.cabac;

import java.util.logging.Logger;

import app.io.BitReader;
import app.io.BitWriter;

public class CABAC {
	private static final Logger LOGGER = Logger.getLogger(CABAC.class.getName());
	private static final int PRECISION = 0x10;
	private static final int PRECISION_MAX = (1 << PRECISION) - 1;
	private static final int HALF_RANGE = PRECISION_MAX >> 1;
	private static final int QTR_RANGE = HALF_RANGE >> 1;
	private static final int MSB_MASK = (1 << (PRECISION - 1));

	private final int[] history = new int[2];
	private int low = 0, mid = 0, high = 0, e3_count = 0;

	public void reset() {
		low = 0;
		mid = HALF_RANGE;
		high = PRECISION_MAX;
		e3_count = 0;
		history[0] = history[1] = 1;
	}

	private void adjustToModel() {
		long range = (long) high - low + 1;
		mid = low + (int) ((range * history[0] + (history[0] + history[1] - 1) / 2) / (history[0] + history[1])) - 1;
		if (mid < low || mid > high) {
			mid = (low + high) >>> 1;
		}
		LOGGER.fine(() -> String.format("Model adjusted: low=%d, mid=%d, high=%d", low, mid, high));
	}

	private void feedSymbol(byte symbol) {
		adjustToModel();
		if ((symbol & 1) == 1) {
			low = mid + 1;
		} else {
			high = mid;
		}
		if (low > high) {
			throw new IllegalStateException("Range underflow detected: low exceeds high.");
		}
		history[symbol & 1]++;
	}

	private void scale(BitWriter writer) {
		while ((low ^ high) < MSB_MASK) {
			byte bit = (byte) (low >>> (PRECISION - 1));
			writer.write(bit);
			writeInverseBits(writer, bit);
			low = (low << 1) & PRECISION_MAX;
			high = ((high << 1) & PRECISION_MAX) | 1;
		}
	}

	private void writeInverseBits(BitWriter writer, byte bit) {
		byte inverted = (byte) (bit ^ 1);
		for (int i = 0; i < e3_count; i++) {
			writer.write(inverted);
		}
		e3_count = 0;
	}

	private void flush(BitWriter writer) {
		byte finalBit = (byte) ((low <= QTR_RANGE) ? 0 : 1);
		writer.write(finalBit);
		writeInverseBits(writer, finalBit);
		e3_count = 0;
	}

	public byte[] encode(byte[] stream) {
		if (stream == null || stream.length == 0) {
			throw new IllegalArgumentException("Input stream cannot be null or empty.");
		}

		BitReader reader = new BitReader(stream);
		BitWriter writer = new BitWriter();
		reset();

		while (!reader.isFullyRead()) {
			try {
				feedSymbol(reader.read());
				scale(writer);
			} catch (Exception e) {
				LOGGER.severe("Encoding error: " + e.getMessage());
				throw new IllegalStateException("Error during encoding: " + e.getMessage(), e);
			}
		}
		flush(writer);
		return writer.toByteArray();
	}
}
