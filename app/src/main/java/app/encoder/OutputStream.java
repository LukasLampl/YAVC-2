package app.encoder;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import app.interprediction.Vector;
import app.utils.ColorManager;
import app.utils.MacroBlock;
import app.utils.PixelRaster;
import app.utils.Protocol;
import app.utils.QueueObject;

public class OutputStream {
	private File OUTPUT_FILE = null;
	private File TEMP_OUTPUT_FILE = null;
	private boolean canWrite = false;
	private boolean finishQueue = false;
	private ConcurrentLinkedQueue<QueueObject> QUEUE = new ConcurrentLinkedQueue<QueueObject>();
	
	private ArrayList<Integer> indexesOfEachPart = new ArrayList<Integer>();
	
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
			byte[] data = new byte[Protocol.META_DATA_LEN];//4 Bytes per integer.
			byte[] width = Protocol.getIntBytes(dim.width);
			byte[] height = Protocol.getIntBytes(dim.height);
			byte[] numberOfFrames = Protocol.getIntBytes(filesCount);
			writeBytesToByteArray(width, data, 0);
			writeBytesToByteArray(height, data, 4);
			writeBytesToByteArray(numberOfFrames, data, 8);
			Files.write(Path.of(this.OUTPUT_FILE.getAbsolutePath()), data, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeStartFrame(PixelRaster raster) {
		byte[] data = new byte[raster.getWidth() * raster.getHeight() * 3 + 1];
		int index = 0;
		
		for (int x = 0; x < raster.getWidth(); x++) {
			for (int y = 0; y < raster.getHeight(); y++) {
				int rgb = ColorManager.convertYUVToRGB(raster.getYUV(x, y));
				byte r = (byte)((rgb >> 16) & 0xFF);
				byte g = (byte)((rgb >> 8) & 0xFF);
				byte b = (byte)(rgb & 0xFF);
				data[index] = r;
				data[index + 1] = g;
				data[index + 2] = b;
				index += 3;
			}
		}
		
		try {
			Files.write(Path.of(this.TEMP_OUTPUT_FILE.getAbsolutePath()), data, StandardOpenOption.TRUNCATE_EXISTING);
			this.indexesOfEachPart.add(data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeRawBlocks(File file, ArrayList<MacroBlock> blocks) {
		int size = Protocol.RAW_BLOCK_SIZE_CHECK_LENGTH;
		
		for (MacroBlock b : blocks) {
			size += (b.getSquaredSize() * 3) + Protocol.RAW_BLOCK_HEADER_LENGTH;
		}
		
		byte[] data = new byte[size];
		int currentIndex = 0;
		writeBytesToByteArray(Protocol.getIntBytes(blocks.size()), data, currentIndex);
		currentIndex += Protocol.RAW_BLOCK_SIZE_CHECK_LENGTH;
		
		for (MacroBlock block : blocks) {
			Point pos = block.getPosition();
			byte[] posX = Protocol.getPositionBytes(pos.x);
			byte[] posY = Protocol.getPositionBytes(pos.y);
			byte sizeBytes = Protocol.getReferenceAndSizeByte(0, block.getSize());
			byte[] differences = new byte[block.getSquaredSize() * 3];
			
			for (int y = 0, index = 0; y < block.getSize(); y++) {
				for (int x = 0; x < block.getSize(); x++) {
					int argb = ColorManager.convertYUVToRGB(block.getYUV(x, y));
					byte r = (byte)((argb >> 16) & 0xFF);
					byte g = (byte)((argb >> 8) & 0xFF);
					byte b = (byte)(argb & 0xFF);
					differences[index] = r;
					differences[index + 1] = g;
					differences[index + 2] = b;
					index += 3;
				}
			}
			
			writeBytesToByteArray(posX, data, currentIndex);
			currentIndex += posX.length;
			writeBytesToByteArray(posY, data, currentIndex);
			currentIndex += posY.length;
			data[currentIndex] = sizeBytes;
			currentIndex += 1;
			writeBytesToByteArray(differences, data, currentIndex);
			currentIndex += differences.length;
		}
		
		try {
			Files.write(Path.of(file.getAbsolutePath()), data, StandardOpenOption.APPEND);
			this.indexesOfEachPart.add(data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeVectors(File file, ArrayList<Vector> vecs) {
		if (vecs == null) {
			throw new NullPointerException("No vectors were passed for writing.");
		}
		
		int size = Protocol.calculateSize(vecs);
		int currentIndex = 0;
		byte[] data = new byte[size];
		writeBytesToByteArray(Protocol.getIntBytes(vecs.size()), data, currentIndex);
		currentIndex += Protocol.VECTOR_SIZE_CHECK_LENGTH;
		
		for (Vector v : vecs) {
			byte[] posX = Protocol.getPositionBytes(v.getPosition().x);
			byte[] posY = Protocol.getPositionBytes(v.getPosition().y);
			byte[] span = Protocol.getVectorSpanBytes(v.getSpanX(), v.getSpanY());
			byte refAndSize = Protocol.getReferenceAndSizeByte(v.getReference(), v.getSize());
			byte[][] differences = Protocol.getVectorAbsoluteColorDifferenceBytes(v.getDCTCoefficientsOfAbsoluteColorDifference(), v.getSize());
			
			writeBytesToByteArray(posX, data, currentIndex);
			currentIndex += posX.length;
			writeBytesToByteArray(posY, data, currentIndex);
			currentIndex += posY.length;
			writeBytesToByteArray(span, data, currentIndex);
			currentIndex += span.length;
			data[currentIndex] = refAndSize;
			currentIndex += 1;
			
			for (int n = 0; n < differences.length; n++) {
				writeBytesToByteArray(differences[n], data, currentIndex);
				currentIndex += differences[n].length;
			}
		}

		try {
			Files.write(Path.of(file.getAbsolutePath()), data, StandardOpenOption.APPEND);
			this.indexesOfEachPart.add(data.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeBytesToByteArray(byte[] bytes, byte[] arr, int index) {
		for (int i = 0; i < bytes.length; i++) {
			arr[index++] = bytes[i];
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
						Thread.sleep(50);
					} catch (InterruptedException e) {}
				} else {
					QueueObject obj = this.QUEUE.poll();
					writeVectors(this.TEMP_OUTPUT_FILE, obj.getVectors());
					writeRawBlocks(this.TEMP_OUTPUT_FILE, obj.getDifferences());
				}
			}
			
			writeLens();
			transferVectors();
		});
		
		writer.setName("YAVC_Frame_Output_Stream");
		writer.start();
	}
	
	private void writeLens() {
		byte[] data = new byte[this.indexesOfEachPart.size() * Protocol.SIZE_OF_INT + Protocol.SIZE_OF_INT];
		int currentIndex = 0;
		
		byte[] lenOfIndexes = Protocol.getIntBytes(this.indexesOfEachPart.size());
		writeBytesToByteArray(lenOfIndexes, data, currentIndex);
		currentIndex += Protocol.SIZE_OF_INT;
		
		for (int i : this.indexesOfEachPart) {
			byte[] index = Protocol.getIntBytes(i);
			writeBytesToByteArray(index, data, currentIndex);
			currentIndex += Protocol.SIZE_OF_INT;
		}
		
		try {
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
