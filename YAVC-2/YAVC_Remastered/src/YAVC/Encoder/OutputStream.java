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
	private boolean waitForWriterToFinish = false;
	private ArrayList<QueueObject> QUEUE = new ArrayList<QueueObject>();
	private int fileCounter = 0;
	
	private ColorManager COLOR_MANAGER = new ColorManager();
	
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
		StringBuilder img = new StringBuilder(raster.getWidth() * raster.getHeight());
		
		for (int x = 0; x < raster.getWidth(); x++) {
			for (int y = 0; y < raster.getHeight(); y++) {
				img.append(this.COLOR_MANAGER.convertYUVToRGB(raster.getYUV(x, y)));
				img.append(".");
			}
		}
		
		try {
			File output = new File(this.OUTPUT_FILE.getAbsolutePath() + "/SF.yavc");
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
				byte[] differences = getVectorAbsoluteColorDifferenceBytes(v.getDCTCoefficientsOfAbsoluteColorDifference(), v.getSize());
				addByteToArrayList(bytesOfVectors, posX);
				addByteToArrayList(bytesOfVectors, posY);
				addByteToArrayList(bytesOfVectors, span);
				bytesOfVectors.add(refAndSize);
				addByteToArrayList(bytesOfVectors, differences);
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
	
	/*
	 * Purpose: Writes a byte array into a byte ArrayList
	 * Return Type: void
	 * Params: ArrayList<Byte> byteList => List to write the bytes into;
	 * 			byte[] array => Array to write into the list (in order)
	 */
	private void addByteToArrayList(ArrayList<Byte> byteList, byte[] array) {
		if (byteList == null || array == null) throw new NullPointerException();
		
		for (int i = 0; i < array.length; i++) {
			byteList.add(array[i]);
		}
	}
	
	/*
	 * Purpose: Get the bytes for the position of the vector (max. 65536)
	 * Return Type: byte[] => Array of the position in bytes
	 * Params: int pos => position to write;
	 * 			int offset => Coding offset, so the encoded bytes do not collide with coding units
	 * Function: First the offset is added to the original pos, since adding the offset afterwards
	 * 			might exceed the limit of an byte (255). The position gets splitted into two bytes
	 * 			containing the position.
	 * 			Example for position 21201 (BIN: 0101 0010 1101 0001). The number in BIN has more than
	 * 			8 digits, so the last digits are written into a byte. The second part, that is in the
	 * 			front is also put into a byte. To extract the first 8 bits, the number is shifted 8 to
	 * 			the right. To sum it all up, the first byte contains 0101 0010 and the second contains
	 * 			1101 0001 (for this example).
	 */
	private byte[] getPositionByte(int pos, int offset) {
		pos += offset;
		if (pos > 65536) throw new IllegalArgumentException("Position of vector exceeds maximum limit of 65536");
		else if (pos < 0) throw new IllegalArgumentException("Position of vector is smaller than 0 (out of frame)");
		return new byte[] {(byte)((pos >> 8) & 0xFF), (byte)(pos)};
	}
	
	/*
	 * Purpose: Get the byte for reference and size of the vector
	 * Return Type: byte => Byte containing reference and size
	 * Params: int reference => Reference of the vector;
	 * 			int size => Size of the vector (size in px);
	 * 			int offset => Coding offset, so the encoded bytes do not collide with coding units
	 * Function: One byte is splitted into 2 parts, each with 4 bits. The upper part is the storage
	 * 			place for the reference, while the lower part is for the size. The reference cannot
	 * 			exceed 7, since it would get bigger than 4 bits. If the number is negative, a sign is
	 * 			written to the first bit. The size would be too big for 4 bits, thats why the size
	 * 			is only represented by numbers from 1 to 6. If we'd do an example for reference 4
	 * 			and size 64. First reference is written into the upper part of the byte.
	 * 		-> First: 0000 0100 => 0100 0000 (Bitshifting 4 to the left)
	 * 		-> Next: 64 = 5 (BIN: 101)
	 * 		-> Now combine both: 0100 0000 | 0000 0101 => 0100 0101 
	 * 		-> Finally add the Coding offset to the result
	 */
	private byte getReferenceAndSizeByte(int reference, int size, int offset) {
		if (reference > 7 || reference < -7) throw new IllegalArgumentException("Reference out of range (-7 to 7)");
		byte res = 0;
		
		if (reference < 0) {
			res = (byte)(((1 << 3) | Math.abs(reference)) << 4);
		} else {
			res = (byte)(Math.abs(reference) << 4);
		}
		
		switch (size) {
			case 128: res |= 6; break;
			case 64: res |= 5; break;
			case 32: res |= 4; break;
			case 16: res |= 3; break;
			case 8: res |= 2; break;
			case 4: res |= 1; break;
			default: throw new IllegalArgumentException("Size: " + size + " not supported by YAVC");
		}
		
		return (byte)(res + offset);
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
	
	private byte[] getVectorAbsoluteColorDifferenceBytes(ArrayList<double[][][]> absoluteDifference, int size) {
		int halfSize = size / 2, frac = size == 4 ? 4 : 8, halfFrac = frac / 2;
		byte[] YBytes = new byte[size * size];
		byte[] UBytes = new byte[halfSize * halfSize];
		byte[] VBytes = new byte[halfSize * halfSize];
		byte[] res = new byte[YBytes.length + UBytes.length + VBytes.length];
		
		int YIndex = 0, UIndex = 0, VIndex = 0;
		
		for (double[][][] coeffGroup : absoluteDifference) {
			for (int x = 0; x < frac; x++) {
				for (int y = 0; y < frac; y++) {
					YBytes[YIndex++] = getDCTCoeffByte(coeffGroup[0][x][y]);
				}
			}
			
			for (int x = 0; x < halfFrac; x++) {
				for (int y = 0; y < halfFrac; y++) {
					UBytes[UIndex++] = getDCTCoeffByte(coeffGroup[1][x][y]);
					VBytes[VIndex++] = getDCTCoeffByte(coeffGroup[2][x][y]);
				}
			}
		}
		
		System.arraycopy(YBytes, 0, res, 0, YBytes.length);
		System.arraycopy(UBytes, 0, res, YBytes.length, UBytes.length);
		System.arraycopy(VBytes, 0, res, YBytes.length + UBytes.length, VBytes.length);
		return res;
	}

	private byte getDCTCoeffByte(double coeff) {
		byte result = (byte)Math.abs(coeff);
		
		if (coeff < 0) {
			result |= (1 << 7);
		}
		
		return (byte)(result + config.CODING_OFFSET);
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
				while (isWriterActive == true || waitForWriterToFinish == true) {
					if (QUEUE.size() == 0) {
						if (waitForWriterToFinish == true) {
							break;
						}
						
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
	
	public void waitForFinish() {
		this.waitForWriterToFinish = true;
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
