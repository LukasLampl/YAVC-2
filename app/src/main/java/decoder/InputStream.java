package decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import utils.Protocol;

public class InputStream {
	private FileInputStream fis = null;
	
	public InputStream(File file) {
		try {
			this.fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		if (this.fis == null) {
			System.err.println("NO FILE_INPUT_STREAM_ AVAILABLE!");
			System.exit(0);
		}
	}
	
	public byte[] getMetadata() {
		byte[] data = new byte[Protocol.META_DATA_LEN];
		readIn(data);
		return data;
	}
	
	public byte[] getNumberOfIndexes() {
		byte[] data = new byte[Protocol.SIZE_OF_INT];
		readIn(data);
		return data;
	}
	
	public byte[] getIndexes(int length) {
		byte[] data = new byte[length * Protocol.SIZE_OF_INT];
		readIn(data);
		return data;
	}
	
	public byte[] getChunk(int lengthOfData) {
		byte[] data = new byte[lengthOfData];
		readIn(data);
		return data;
	}
	
	private void readIn(byte[] buffer) {
		try {
			this.fis.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
