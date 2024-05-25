package YAVC.Encoder;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import YAVC.Main.config;
import YAVC.Utils.ColorManager;
import YAVC.Utils.MacroBlock;
import YAVC.Utils.PixelRaster;
import YAVC.Utils.QueueObject;
import YAVC.Utils.Vector;

public class OutputStream {
	private File OUTPUT_FILE = null;
	private boolean isWriterActive = false;
	private ArrayList<QueueObject> QUEUE = new ArrayList<QueueObject>();
	private int fileCounter = 0;
	
	private ColorManager COLOR_MANAGER = new ColorManager();
	
	public OutputStream(File file) {
		File out = new File(file.getAbsolutePath() + "/YAVC-Res.yavc.part");
		out.mkdir();
		this.OUTPUT_FILE = out;
	}
	
	//DEBUG ONLY!
	public void writeStartFrame(File file, PixelRaster raster) {
		StringBuilder img = new StringBuilder(raster.getWidth() * raster.getHeight());
		
		for (int x = 0; x < raster.getWidth(); x++) {
			for (int y = 0; y < raster.getHeight(); y++) {
				img.append(this.COLOR_MANAGER.convertYUVToRGB(raster.getYUV(x, y)));
				img.append(".");
			}
		}
		
		try {
			File output = new File(file.getAbsolutePath() + "/SF.yavc");
			output.createNewFile();
			
			Files.write(Path.of(output.getAbsolutePath()), img.toString().getBytes(), StandardOpenOption.WRITE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeVectors(File file, ArrayList<Vector> vecs) {
		ArrayList<Byte> bytesOfVectors = new ArrayList<Byte>();
		bytesOfVectors.add(config.VECTOR_START);
		int offset = config.CODING_OFFSET;
		
		if (vecs != null) {
			for (Vector v : vecs) {
				byte[] posX = getPositionByte(v.getPosition().x, offset);
				byte[] posY = getPositionByte(v.getPosition().y, offset);
				byte[] span = getVectorSpanBytes(v.getSpanX(), v.getSpanY(), offset);
				byte refAndSize = getReferenceAndSizeByte(v.getReference(), v.getSize(), offset);
//				byte[] differences = getVectorAbsoluteColorDifferenceBytes(v.getDCTCoefficientsOfAbsoluteColorDifference(), v.getSize());
				addByteToArrayList(bytesOfVectors, posX);
				addByteToArrayList(bytesOfVectors, posY);
				addByteToArrayList(bytesOfVectors, span);
				bytesOfVectors.add(refAndSize);
//				addByteToArrayList(bytesOfVectors, differences);
			}
		}
		
		int size = bytesOfVectors.size();
		byte[] result = new byte[size];
		for (int i = 0; i < size; i++) result[i] = bytesOfVectors.get(i);
			
		try {
			FileOutputStream outStream = new FileOutputStream(file);
			outStream.write(result);
			outStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void addByteToArrayList(ArrayList<Byte> byteList, byte[] array) {
		for (int i = 0; i < array.length; i++) {
			byteList.add(array[i]);
		}
	}
	
	private byte[] getPositionByte(int pos, int offset) {
		pos += offset;
		return new byte[] {(byte)((pos >> 8) & 0xFF), (byte)(pos)};
	}
	
	private byte getReferenceAndSizeByte(int reference, int size, int offset) {
		if (reference > 7 || reference < -7) {
			System.err.println("Reference to high! > Can't write into single byte!");
		}
		
		byte res = 0;
		
		if (reference < 0) {
			res = (byte)(((1 << 3) | Math.abs(reference)) << 4);
		}
		
		if (size == 128) {
			res |= 1;
		} else if (size == 64) {
			res |= 2;
		} else if (size == 32) {
			res |= 3;
		} else if (size == 16) {
			res |= 4;
		} else if (size == 8) {
			res |= 5;
		} else if (size == 4) {
			res |= 6;
		}
		
		return res;
	}
	
	private byte[] getVectorSpanBytes(int spanX, int spanY, int offset) {
		byte bytespany = (byte)Math.abs(spanY);
		byte bytespanx = (byte)Math.abs(spanX);
		
		if (bytespany > 127 || bytespanx > 127) System.err.println("Span to big (|Span| > 127)!");
		
		if (spanY < 0) {
			bytespany = (byte)((1 << 7) | bytespany);
		}
		
		if (spanX < 0) {
			bytespanx = (byte)((1 << 7) | bytespanx);
		}
		
		return new byte[] {(byte)(bytespanx + offset), (byte)(bytespany + offset)};
	}
	
//	private byte[] getVectorAbsoluteColorDifferenceBytes(double[][][][] absoluteDifference, int size) {
//		int halfSize = size / 2;
//		byte[] differenceBytes = new byte[size * size + 2 * halfSize * halfSize];
//		byte[] VBytes = new byte[halfSize * halfSize];
//		int index = 0, indexV = 0;
//		
//		for (double[][][] arr : absoluteDifference) {
//			for (int x = 0; x < size; x++) {
//				for (int y = 0; y < size; y++) {
//					differenceBytes[index++] = getDCTCoeffByte(arr[0][x][y], size, (x == 0 && y == 0) ? true : false);
//				}
//			}
//		}
//		
//		for (double[][][] arr : absoluteDifference) {
//			for (int x = 0; x < halfSize; x++) {
//				for (int y = 0; y < halfSize; y++) {
//					differenceBytes[index++] = getDCTCoeffByte(arr[1][x][y], size, (x == 0 && y == 0) ? true : false);
//					differenceBytes[indexV++] = getDCTCoeffByte(arr[2][x][y], size, (x == 0 && y == 0) ? true : false);
//				}
//			}
//		}
//			
//		System.arraycopy(VBytes, 0, differenceBytes, index, halfSize * halfSize);
//		return differenceBytes;
//	}

	private byte getDCTCoeffByte(double coeff, int size, boolean cord0x0) {
		byte result = 0;
		
		if (cord0x0 == false) {
			result = (byte)Math.abs(coeff);
			
			if (coeff < 0) {
				result |= (1 << 7);
			}
		} else {
			result = (byte)Math.round(Math.abs(coeff / (size * size)));
			
			if (coeff < 0) {
				result |= (1 << 7);
			}
		}
		
		return result;
	}
	
	//ONLY FOR DEBUG!
	private void writeDifferences(File file, ArrayList<MacroBlock> blocks) {
		StringBuilder differences = new StringBuilder();
		differences.append(config.MACRO_BLOCK_START);
		
		if (blocks != null) {
			for (MacroBlock b : blocks) {
				int size = b.getSize();
				
				for (int x = 0; x < size; x++) {
					for (int y = 0; y < size; y++) {
						differences.append(this.COLOR_MANAGER.convertYUVToRGB(b.getYUV(x, y)));
						differences.append(".");
					}
				}
				
				differences.append("$");
				differences.append((char)(size));
				differences.append((char)(b.getPosition().x));
				differences.append((char)(b.getPosition().y));
				differences.append(";");
			}
		}
		
		try {
			Files.write(Path.of(file.getAbsolutePath()), differences.toString().getBytes(), StandardOpenOption.WRITE);
		} catch (IOException e) {
			e.printStackTrace();
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
		
		this.isWriterActive = true;
		
		Thread writer = new Thread(() -> {
			try {
				while (isWriterActive == true) {
					if (QUEUE.size() == 0) {
						Thread.sleep(50);
					} else {
						QueueObject obj = QUEUE.get(0);
						File f = new File(OUTPUT_FILE.getAbsolutePath() + "/F" + fileCounter++ + ".yavcf");
						f.createNewFile();

						writeDifferences(f, obj.getDifferences());
						writeVectors(f, obj.getVectors());
						
						QUEUE.remove(0);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		writer.setName("YAVC_OutputStream");
		writer.start();
	}
	
	public void shutdown() {
		this.isWriterActive = false;
	}
	
	public PixelRaster renderResult(ArrayList<Vector> vecs, ArrayList<PixelRaster> refs, ArrayList<MacroBlock> diffs, PixelRaster prevFrame) {
		PixelRaster render = prevFrame.copy();
		Dimension dim = prevFrame.getDimension();
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		if (diffs != null) {
			for (MacroBlock block : diffs) {
				Callable<Void> task = () -> {
					Point pos = block.getPosition();
					int size = block.getSize();
					
					for (int x = 0; x < size; x++) {
						if (pos.x + x < 0 || pos.x + x >= dim.width) continue;
						
						for (int y = 0; y < size; y++) {
							if (pos.y + y < 0 || pos.y + y >= dim.height) continue;
							
							render.setYUV(x + pos.x, y + pos.y, block.getYUV(x, y));
						}
					}
					
					return null;
				};
				
				executor.submit(task);
			}
		}
		
		if (vecs != null) {
			for (Vector v : vecs) {
				Callable<Void> task = () -> {
					PixelRaster cache = v.getReference() == -1 ? null : refs.get(config.MAX_REFERENCES - v.getReference());
					Point pos = v.getPosition();
					int EndX = pos.x + v.getSpanX(), EndY = pos.y + v.getSpanY();
					int size = v.getSize();
					double[][][] reconstructedColor = reconstructColors(v.getIDCTCoefficientsOfAbsoluteColorDifference(), cache.getPixelBlock(pos, size, null), size);
					
					for (int x = 0; x < size; x++) {
						if (EndX + x < 0 || EndX + x >= dim.width) continue;
						if (pos.x + x < 0 || pos.x + x >= dim.width) continue;
						
						for (int y = 0; y < size; y++) {
							if (EndY + y < 0 || EndY + y >= dim.height) continue;
							if (pos.y + y < 0 || pos.y + y >= dim.height) continue;
							int subSX = x / 2, subSY = y / 2;
							double[] YUV = new double[] {reconstructedColor[0][x][y], reconstructedColor[1][subSX][subSY], reconstructedColor[2][subSX][subSY]};
							render.setYUV(x + EndX, y + EndY, YUV);
						}
					}
					
					return null;
				};
				
				executor.submit(task);
			}
		}
		
		executor.shutdown();
		return render;
	}
	
	private double[][][] reconstructColors(double[][][] differenceOfColor, double[][][] referenceColor, int size) {
		int halfSize = size / 2;
		double[][][] reconstructedColor = new double[3][][];
		reconstructedColor[0] = new double[size][size];
		reconstructedColor[1] = new double[halfSize][halfSize];
		reconstructedColor[2] = new double[halfSize][halfSize];
		
		//Reconstruct Y-Comp
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				reconstructedColor[0][x][y] = referenceColor[0][x][y] + differenceOfColor[0][x][y];
			}
		}
		
		//Reconstruct U,V-Comp
		for (int x = 0; x < halfSize; x++) {
			for (int y = 0; y < halfSize; y++) {
				reconstructedColor[1][x][y] = referenceColor[1][x][y] + differenceOfColor[1][x][y];
				reconstructedColor[2][x][y] = referenceColor[2][x][y] + differenceOfColor[2][x][y];
			}
		}
		
		return reconstructedColor;
	}
}
