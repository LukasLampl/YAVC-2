package encoder;

import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import app.config;
import utils.ColorManager;
import utils.PixelRaster;
import utils.Protocol;
import utils.QueueObject;
import utils.Vector;

public class OutputStream {
	private File OUTPUT_FILE = null;
	private boolean canWrite = false;
	private boolean finishQueue = false;
	private ArrayList<QueueObject> QUEUE = new ArrayList<QueueObject>();
	private int fileCounter = 0;
	
	public OutputStream(File file) {
		File out = new File(file.getAbsolutePath() + "/YAVC-Res.yavc.part");
		out.mkdir();
		this.OUTPUT_FILE = out;
	}
	
	public void writeMetadata(Dimension dim, int filesCount) {
		try {
			String content = "{DIM:" + dim.width + "," + dim.height + "}" +
							"{FC:" + filesCount + "}";
			File output = new File(this.OUTPUT_FILE.getAbsolutePath() + "/META.yavc");
			output.createNewFile();
			
			Files.write(Path.of(output.getAbsolutePath()), content.getBytes(), StandardOpenOption.WRITE);
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
			File output = new File(this.OUTPUT_FILE.getAbsolutePath() + "/SF.yavc");
			output.createNewFile();
			
			Files.write(Path.of(output.getAbsolutePath()), data, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeVectors(File file, ArrayList<Vector> vecs) {
		int size = Protocol.calculateSize(vecs) + 1; //+1 for the VECTOR_START byte
		int currentIndex = 1;
		byte[] data = new byte[size];
		data[0] = config.VECTOR_START;
		
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
			FileOutputStream outStream = new FileOutputStream(file);
			outStream.write(data);
			outStream.close();
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
					try {
						QueueObject obj = QUEUE.remove(0);
						File f = new File(OUTPUT_FILE.getAbsolutePath() + "/F" + fileCounter++ + ".yavcf");
						
						if (f.createNewFile() == false) {
							throw new IOException("Couldn't create frame-file \"" + f.getName() + "\"!");
						}
						
						writeVectors(f, obj.getVectors());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		writer.setName("YAVC_Frame_Output_Stream");
		writer.start();
	}
	
	public void shutdown() {
		this.canWrite = false;
	}
	
	public void finishQueue() {
		this.finishQueue = true;
	}
}
