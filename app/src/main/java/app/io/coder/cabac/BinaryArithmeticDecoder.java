package app.io.coder.cabac;

import app.io.BitReader;
import app.io.BitWriter;

public class BinaryArithmeticDecoder extends BinaryArithmeticCoder {
	private BitWriter output = new BitWriter();
	private BitReader input = null;
	
	private Interval currentInterval = new Interval(0, Integer.MAX_VALUE);
	
	private int value = 0;
	
	public void decode(final byte[] stream, final int totalSteps) {
		this.input = new BitReader(stream);
		this.value = readBits(totalSteps < Integer.SIZE ? totalSteps : Integer.SIZE);
		int stepCounter = 0;
		
		while (!this.input.isFullyRead() && stepCounter < totalSteps) {
//			if (c % Byte.SIZE == 0) {
//				System.out.println("Bytes read: " + (c / Byte.SIZE));
//			}
			
			decode();
			stepCounter++;
		}
		
		for (int c = stepCounter; c < totalSteps; c++) {
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
