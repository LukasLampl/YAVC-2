package app.io;

public class BitReader {
	private final static int BYTE_SHIFT_SIZE = Byte.SIZE - 1;
	
	private final byte[] stream;
	private final int streamLength;
	private final int streamLengthBits;
	
	private int currentByte = 0;
	private int currentBit = 0;
	private int totalReadBits = 0;
	
	public BitReader(final byte[] stream) {
		this.stream = stream;
		this.streamLength = stream.length;
		this.streamLengthBits = this.streamLength * Byte.SIZE;
	}
	
	public BitReader(final byte[] stream, final int totalBits) {
		this.stream = stream;
		this.streamLength = stream.length;
		this.streamLengthBits = totalBits;
	}
	
	public byte read() {
		if (isFullyRead()) {
			throw new IllegalStateException("Stream is already fully read!");
		}
		
		byte bit = (byte)((this.stream[this.currentByte]
				>> (BYTE_SHIFT_SIZE - this.currentBit))
				& 0x01);
		this.currentBit++;
		this.totalReadBits++;
		
		if (this.currentBit == Byte.SIZE) {
			this.currentByte++;
			this.currentBit = 0;
		}
		
		return bit;
	}
	
	public boolean isFullyRead() {
		return this.totalReadBits >= this.streamLengthBits;
	}
}
