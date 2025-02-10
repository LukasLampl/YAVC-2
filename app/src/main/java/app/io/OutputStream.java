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

package app.io;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.intraprediction.EncodingIntraPredictionBlock;
import app.io.containers.QueueObject;
import app.utils.PixelRaster;

/**
 * The {@code OutputStream} class is the output writer of the YAVC-Encoder.
 * It provides an asynchronous running writer thread which is thread safe.
 * 
 * @author Lukas Lampl
 */
public class OutputStream {
	/**
	 * Time to sleep, when no frame is available to write.
	 * Unit in ms.
	 */
	private static final int SLEEP_TIME = 30;
	
	/**
	 * File in which to write the data to.
	 */
	private File OUTPUT_FILE = null;
	
	/**
	 * Temporary file in which to write the data to.
	 */
	private File TEMP_OUTPUT_FILE = null;
	
	/**
	 * Flag for whether the {@code OutputStream} is allowed to write or not.
	 */
	private boolean canWrite = false;
	
	/**
	 * Flag that the {@code OutputStream} should finish its writing process as
	 * soon as possible.
	 */
	private boolean finishQueue = false;
	
	/**
	 * Queue of all objects to write.
	 */
	private ConcurrentLinkedQueue<QueueObject> queue = new ConcurrentLinkedQueue<QueueObject>();
	
	/**
	 * List of all lengths that is written into the file as a header.
	 */
	private ArrayList<Integer> lengthOfEachPart = new ArrayList<Integer>();
	
	/**
	 * Opens an {@code OutputStream} to the given output file.
	 * 
	 * @param output	File to which to write.
	 */
	public OutputStream(File output) {
		try {			
			File dir = new File(output.getParent());
			
			File tempOut = new File(dir.getAbsolutePath() + "/YAVC_TEMP.yavcv");
			tempOut.createNewFile();
			
			this.OUTPUT_FILE = output;
			this.TEMP_OUTPUT_FILE = tempOut;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the metadata into the output file and truncates existing data.
	 * 
	 * @param dim			Dimension of all frames.
	 * @param filesCount	Number of frames.
	 */
	public void writeMetadata(Dimension dim, int filesCount) {
		try {
			byte[] data = Protocol.getMetadata(dim, filesCount);
			Files.write(Path.of(this.OUTPUT_FILE.getAbsolutePath()), data, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the start frame into the temporary output.
	 * 
	 * @param raster	The start frame.
	 */
	public void writeStartFrame(PixelRaster raster) {
		try {
			byte[] data = Protocol.getStartFrameBytes(raster);
			Files.write(Path.of(this.TEMP_OUTPUT_FILE.getAbsolutePath()), data, StandardOpenOption.TRUNCATE_EXISTING);
			this.lengthOfEachPart.add(data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes a list of intra blocks to the given file.
	 * 
	 * @param file			File in which to write.
	 * @param intraBlocks	Intra blocks to write.
	 */
	private void writeIntraBlock(File file, ArrayList<EncodingIntraPredictionBlock> intraBlocks) {
		try {
			byte[] data = Protocol.getIntraBlockBytes(intraBlocks, true);
			Files.write(Path.of(file.getAbsolutePath()), data, StandardOpenOption.APPEND);
			this.lengthOfEachPart.add(data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes a list of vectors into the given file.
	 * 
	 * @param file	File in which to write.
	 * @param vecs	Vectors to write.
	 */
	private void writeVectors(File file, ArrayList<EncodingVector> vecs) {
		try {
			byte[] data = Protocol.getVectorBytes(vecs, true);
			Files.write(Path.of(file.getAbsolutePath()), data, StandardOpenOption.APPEND);
			this.lengthOfEachPart.add(data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds an object to the writing queue.
	 * 
	 * @param obj	The object to write.
	 */
	public void addObjectToOutputQueue(QueueObject obj) {
		this.queue.add(obj);
	}
	
	/**
	 * Starts the writer thread. First it writes the metadata in the output file,
	 * then the vectors and non-coded blocks into the temporary file. After
	 * finishing the length of each frame part is appended to the output file
	 * and finally the data of the temporary file is transmitted to the output
	 * file as well.
	 * 
	 * @throws IllegalStateException	When the output file is {@code null}.
	 */
	public void activate() throws IllegalStateException {
		if (this.OUTPUT_FILE == null) {
			throw new IllegalStateException("Output is defined as null!");
		}
		
		this.canWrite = true;
		
		Thread writer = new Thread(() -> {
			while (canWrite) {
				if (queue.isEmpty()) {
					if (finishQueue) {
						break;
					}
					
					try {
						Thread.sleep(SLEEP_TIME);
					} catch (InterruptedException e) {}
				} else {
					try {
						QueueObject obj = this.queue.poll();
						writeVectors(this.TEMP_OUTPUT_FILE, obj.getVectors());
						writeIntraBlock(this.TEMP_OUTPUT_FILE, obj.getIntraBlocks());
						obj.discard();
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
			}
			
			writeLengths();
			transferTempFile();
		});
		
		writer.setName("YAVC_Frame_Output_Stream");
		writer.start();
	}
	
	/**
	 * Write the lengths of each frame part into the the output file.
	 */
	private void writeLengths() {
		try {
			byte[] data = Protocol.getLengthBytesOfFrame(this.lengthOfEachPart);
			Files.write(Path.of(this.OUTPUT_FILE.getAbsolutePath()), data, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Transfers the temporary file data into the output file.
	 * After that the temporary file is deleted.
	 */
	private void transferTempFile() {
		byte[] buffer = new byte[65536];
		int bytesRead = 0;
		
		try (FileInputStream fis = new FileInputStream(this.TEMP_OUTPUT_FILE);
				FileOutputStream fos = new FileOutputStream(this.OUTPUT_FILE, true)) {
			while ((bytesRead = fis.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
			}
			
			fos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			this.TEMP_OUTPUT_FILE.delete();
		}
	}
	
	/**
	 * Shuts the {@code OutputStream} down.
	 */
	public void shutdown() {
		this.canWrite = false;
	}
	
	/**
	 * Signals the {@code OutputStream} to finish the writing queue as
	 * soon as possible.
	 */
	public void finishQueue() {
		this.finishQueue = true;
	}
}
