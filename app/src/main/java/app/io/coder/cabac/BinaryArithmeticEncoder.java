package app.io.coder.cabac;

import app.io.BitReader;
import app.io.BitWriter;

public class BinaryArithmeticEncoder extends BinaryArithmeticCoder {
	private BitWriter output = new BitWriter();
	private Interval currentInterval = new Interval(0, Integer.MAX_VALUE);
	
	public void encode(final byte[] stream) {
		BitReader reader = new BitReader(stream);
		int c = 0;
		
		while (!reader.isFullyRead()) {
//			if (c % Byte.SIZE == 0) {
//				System.out.println("Bytes read: " + (c / Byte.SIZE));
//			}
			
			encodeSymbol(reader.read());
			c++;
		}
	}
	
	public void encodeSymbol(final byte bit) {
		subdivide(bit, 0.5);
		normalize();
	}
	
	private void subdivide(final byte bit, final double probabilityOfZero) {
		Interval[] subdivisions = this.currentInterval.subdivide(probabilityOfZero);
		this.currentInterval = subdivisions[bit];
	}
	
	private void normalize() {
		if (!this.currentInterval.isRangeReadyForOutput()) {
			return;
		}
		
		byte MSB = this.currentInterval.getLowMSB();
		this.output.write(MSB);
		this.currentInterval.shift();
	}
	
	private void finish() {
		final int mid = (this.currentInterval.getHigh() + this.currentInterval.getLow()) / 2;
		
		for (int i = 0; i < Integer.BYTES; i++) {
			byte MSB = (byte)((mid >> (i * Byte.SIZE)) & 0xFF);
			this.output.writeByte(MSB);
			this.currentInterval.shift();
		}
	}
	
	public byte[] getOutput() {
		finish();
		return this.output.toByteArray();
	}
}
