/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2024  Lukas Nian En Lampl

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

package app.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The {@code InputStream} class is responsible for reading in the YAVC
 * produces YAVC output file. It reads it in form of bytes which are then
 * further processed.
 * 
 * @author Lukas Lampl
 * @since 1.4
 */
public class InputStream {
	/**
	 * The {@code FileInputStream} reading in the YAVC file.
	 */
	private FileInputStream fis = null;
	
	/**
	 * Opens an {@code InputStream} on the given file.
	 * 
	 * @param file	The YAVC file to read in.
	 * @throws FileNotFoundException	When the specified file is not found.
	 */
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
	
	/**
	 * Gets the metadata out of the YAVC file.
	 * 
	 * @return An byte array containing the metadata.
	 */
	public byte[] getMetadata() {
		byte[] data = new byte[Protocol.META_DATA_LEN];
		readIn(data);
		return data;
	}
	
	/**
	 * Gets the length of the frame part lengths.
	 * 
	 * @return An byte array with the length of the frame part lengths.
	 */
	public byte[] getNumberOfIndexes() {
		byte[] data = new byte[Protocol.SIZE_LENGTH];
		readIn(data);
		return data;
	}
	
	/**
	 * Gets the frame part lengths.
	 * 
	 * @param length	The total frame parts.
	 * @return An byte array with the individual lengths.
	 */
	public byte[] getIndexes(int length) {
		byte[] data = new byte[length * Protocol.SIZE_LENGTH];
		readIn(data);
		return data;
	}
	
	/**
	 * Grabs a chunk of data with the given length out of the file.
	 * 
	 * @param lengthOfData	The length of the data.
	 * @return An byte array with the given length and the data out of the YAVC file.
	 */
	public byte[] getChunk(int lengthOfData) {
		byte[] data = new byte[lengthOfData];
		readIn(data);
		return data;
	}
	
	/**
	 * Reads in the YAVC file and put it into the given byte array.
	 * 
	 * @param buffer	Byte array to put the data into.
	 * @throws IOException	When an I/O error occurs.
	 */
	private void readIn(byte[] buffer) {
		try {
			this.fis.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
