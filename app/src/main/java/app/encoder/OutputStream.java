package app.encoder;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import app.interprediction.Vector;
import app.utils.MacroBlock;
import app.utils.PixelRaster;
import app.utils.Protocol;
import app.utils.QueueObject;

public class OutputStream {
	private static final int SLEEP_TIME = 30; //ms
	private File OUTPUT_FILE = null;
	private File TEMP_OUTPUT_FILE = null;
	private boolean canWrite = false;
	private boolean finishQueue = false;
	private ConcurrentLinkedQueue<QueueObject> QUEUE = new ConcurrentLinkedQueue<QueueObject>();
	
	private ArrayList<Integer> lengthOfEachPart = new ArrayList<Integer>();
	
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
	
	public void writeMetadata(Dimension dim, int filesCount) {
		try {
			byte[] data = Protocol.getMetadata(dim, filesCount);
			Files.write(Path.of(this.OUTPUT_FILE.getAbsolutePath()), data, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeStartFrame(PixelRaster raster) {
		try {
			byte[] data = Protocol.getStartFrameBytes(raster);
			Files.write(Path.of(this.TEMP_OUTPUT_FILE.getAbsolutePath()), data, StandardOpenOption.TRUNCATE_EXISTING);
			this.lengthOfEachPart.add(data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeRawBlocks(File file, ArrayList<MacroBlock> blocks) {
		try {
			byte[] data = Protocol.getRawBlockBytes(blocks);
			Files.write(Path.of(file.getAbsolutePath()), data, StandardOpenOption.APPEND);
			this.lengthOfEachPart.add(data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeVectors(File file, ArrayList<Vector> vecs) {
		try {
			byte[] data = Protocol.getVectorBytes(vecs);
			Files.write(Path.of(file.getAbsolutePath()), data, StandardOpenOption.APPEND);
			this.lengthOfEachPart.add(data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addObjectToOutputQueue(QueueObject obj) {
		this.QUEUE.add(obj);
	}
	
	public void activate() throws IllegalStateException {
		if (this.OUTPUT_FILE == null) {
			throw new IllegalStateException("Output is defined as null!");
		}
		
		this.canWrite = true;
		
		Thread writer = new Thread(() -> {
			while (canWrite) {
				if (QUEUE.size() == 0) {
					if (finishQueue) {
						break;
					}
					
					try {
						Thread.sleep(SLEEP_TIME);
					} catch (InterruptedException e) {}
				} else {
					QueueObject obj = this.QUEUE.poll();
					writeVectors(this.TEMP_OUTPUT_FILE, obj.getVectors());
					writeRawBlocks(this.TEMP_OUTPUT_FILE, obj.getDifferences());
				}
			}
			
			writeLengths();
			transferVectors();
		});
		
		writer.setName("YAVC_Frame_Output_Stream");
		writer.start();
	}
	
	private void writeLengths() {
		try {
			byte[] data = Protocol.getLengthBytesOfFrame(this.lengthOfEachPart);
			Files.write(Path.of(this.OUTPUT_FILE.getAbsolutePath()), data, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void transferVectors() {
		try {
			Files.write(Path.of(this.OUTPUT_FILE.getAbsolutePath()), Files.readAllBytes(Path.of(this.TEMP_OUTPUT_FILE.getAbsolutePath())), StandardOpenOption.APPEND);
			this.TEMP_OUTPUT_FILE.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void shutdown() {
		this.canWrite = false;
	}
	
	public void finishQueue() {
		this.finishQueue = true;
	}
}
