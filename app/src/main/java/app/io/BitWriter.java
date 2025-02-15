package app.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BitWriter {
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	
	private byte currentByte = 0;
	private int currentBit = 0;
	
	public void write(byte bit) {
		bit &= 0x01;
		this.currentByte <<= 1;
		this.currentByte |= bit;
		this.currentBit++;
		
		if (this.currentBit == Byte.SIZE) {
			flush();
		}
	}
	
	public void write(int bit) {
		write((byte)bit);
	}
	
	public void writeByte(byte b) {
		for (int i = 0; i < Byte.SIZE; i++) {
			write((b >> i) & 0x01);
		}
	}
	
	private void flush() {
		if (this.currentBit > 0) {
			this.baos.write(this.currentByte);
			this.currentBit = 0;
			this.currentByte = 0x00;
		}
	}
	
	public byte[] toByteArray() {
		flush();
		
		try {
			this.baos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return this.baos.toByteArray();
	}
}
