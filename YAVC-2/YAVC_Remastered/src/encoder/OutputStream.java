package encoder;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import interprediction.Vector;
import utils.ColorManager;
import utils.PixelRaster;
import utils.Protocol;
import utils.QueueObject;

public class OutputStream {
	private File OUTPUT_FILE = null;
	private File TEMP_OUTPUT_FILE = null;
	private boolean canWrite = false;
	private boolean finishQueue = false;
	private ConcurrentLinkedQueue<QueueObject> QUEUE = new ConcurrentLinkedQueue<QueueObject>();
	
	private ArrayList<Integer> indexesOfEachPart = new ArrayList<Integer>();
	
	public OutputStream(File file) {
		try {
			File out = new File(file.getAbsolutePath() + "/YAVC.yavcv");
			out.createNewFile();
			
			File tempOut = new File(file.getAbsolutePath() + "/YAVC_TEMP.yavcv");
			tempOut.createNewFile();
			
			this.OUTPUT_FILE = out;
			this.TEMP_OUTPUT_FILE = tempOut;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeMetadata(Dimension dim, int filesCount) {
		try {
			byte[] data = new byte[3 * 4];//4 Bytes per integer.
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
		byte[] data = new byte[raster.getWidth() * raster.getHeight() * 3];
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
	
	private void writeVectors(File file, ArrayList<Vector> vecs) {
		int size = Protocol.calculateSize(vecs) + 1; //+1 for the VECTOR_START byte
		int currentIndex = 1;
		byte[] data = new byte[size];
		data[0] = Protocol.VECTOR_START;
		
		if (vecs == null) {
			throw new NullPointerException("No vectors were passed for writing.");
		}
		
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
	
	public void activate() {
		if (this.OUTPUT_FILE == null) {
			System.err.println("No output defined!");
			System.exit(0);
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
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					QueueObject obj = this.QUEUE.poll();
					writeVectors(this.TEMP_OUTPUT_FILE, obj.getVectors());
				}
			}
			
			writeLens();
			transferVectors();
		});
		
		writer.setName("YAVC_Frame_Output_Stream");
		writer.start();
	}
	
	private void writeLens() {
		byte[] data = new byte[this.indexesOfEachPart.size() * 4];
		
		for (int i = 0; i < this.indexesOfEachPart.size(); i++) {
			byte[] index = Protocol.getIntBytes(this.indexesOfEachPart.get(i));
			writeBytesToByteArray(index, data, i * 4);
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
