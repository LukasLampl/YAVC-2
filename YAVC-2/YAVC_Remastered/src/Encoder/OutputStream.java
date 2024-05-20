package Encoder;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Main.config;
import Utils.ColorManager;
import Utils.MacroBlock;
import Utils.PixelRaster;
import Utils.QueueObject;
import Utils.Vector;

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
				img.append(this.COLOR_MANAGER.convertYUVToRGB(raster.getYUV(x, y)).getRGB());
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
		StringBuilder vectors = new StringBuilder();
		vectors.append(config.VECTOR_START);
		int offset = config.CODING_OFFSET;
		
		if (vecs != null) {
			for (Vector v : vecs) {
				char posX = (char)(v.getPosition().x + offset), posY = (char)(v.getPosition().y + offset);
				char span = (char)(getVectorSpanChar(v.getSpanX(), v.getSpanY(), offset));
				char refAndSize = (char)((getReferenceChar(v.getReference(), offset)) << 8 | (v.getSize() & 0xFF));
				
				vectors.append(posX);
				vectors.append(posY);
				vectors.append(span);
				vectors.append(refAndSize);
			}
		}
			
		try {
			Files.write(Path.of(file.getAbsolutePath()), vectors.toString().getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int getReferenceChar(int reference, int offset) {
		int res = Math.abs(reference);
		
		if (reference < 0) {
			res = (1 << 7) | res;
		}
		
		return res;
	}
	
	private int getVectorSpanChar(int spanX, int spanY, int offset) {
		int intspany = Math.abs(spanY);
		int intspanx = Math.abs(spanX);
		
		if (spanY < 0) {
			intspany = (1 << 7) | intspany;
		}
		
		if (spanX < 0) {
			intspanx = (1 << 7) | intspanx;
		}
		
		int result = (((intspanx & 0xFF) << 8) | (intspany & 0xFF)) + offset;
		
		if (result > 65536) System.err.println("Coded span is out of bounds!");
		
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
						int col = this.COLOR_MANAGER.convertYUVToRGB(b.getYUV(x, y)).getRGB();
						differences.append(col);
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
					double[][][] reconstructedColor = reconstructColors(v.getAbsoluteColorDifference(), cache.getPixelBlock(pos, size, null), size);
					
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
