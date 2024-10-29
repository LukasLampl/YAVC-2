package app.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import app.config;
import app.encoder.ThreadLoadManager;
import app.interprediction.Vector;

public class RenderEngine {

	static long totalTime = 0L;
	static long numThreads = 0L;

	public static PixelRaster renderResult(ThreadLoadManager<Vector> vecs, ArrayList<PixelRaster> refs, ThreadLoadManager<MacroBlock> differenceManager, PixelRaster prevFrame) {
		PixelRaster render = prevFrame.copy();
		Dimension dim = prevFrame.getDimension();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		totalTime = 0L;
		numThreads = 0L;
		
		try {
			for (int i = 0; i < differenceManager.getNumberOfChunks(); i++) {
				final ArrayList<MacroBlock> blockList = differenceManager.getLoadOf(i);

				Runnable task = () -> { 
					for (MacroBlock block : blockList) {
						Point pos = block.getPosition();
						int size = block.getSize();
						
						for (int x = 0; x < size; x++) {
							if (pos.x + x < 0 || pos.x + x >= dim.width) continue;
							
							for (int y = 0; y < size; y++) {
								if (pos.y + y < 0 || pos.y + y >= dim.height) continue;
								
								render.setYUV(x + pos.x, y + pos.y, block.getYUV(x, y));
							}
						}
					}
				};
				
				executor.submit(task);
			}
						
			if (vecs != null) {
				for (int i = 0; i < vecs.getNumberOfChunks(); i++) {
					final int index = i;
					
					Runnable task = () -> {
						ArrayList<Vector> vecList = vecs.getLoadOf(index);
						long localTotalTime = 0;
						long area = 0;
						
						for (Vector v : vecList) {
							PixelRaster cache = v.getReference() == -1 ? null : refs.get(config.MAX_REFERENCES - v.getReference());
							Point pos = v.getPosition();
							int EndX = pos.x + v.getSpanX();
							int EndY = pos.y + v.getSpanY();
							int size = v.getSize();
							long t_start = System.currentTimeMillis();
							double[][][] reconstructedColor = reconstructColors(v.getIDCTCoefficientsOfAbsoluteColorDifference(false), cache.getPixelBlock(pos, size, null), size);
							long t_end = System.currentTimeMillis();
							totalTime += (t_end - t_start);
							localTotalTime += (t_end - t_start);
							area += v.getSize() * v.getSize();
							
							for (int x = 0; x < size; x++) {
								if (EndX + x < 0 || EndX + x >= dim.width) continue;
								if (pos.x + x < 0 || pos.x + x >= dim.width) continue;
								int subSX = x / 2;
								
								for (int y = 0; y < size; y++) {
									if (EndY + y < 0 || EndY + y >= dim.height) continue;
									if (pos.y + y < 0 || pos.y + y >= dim.height) continue;
									int subSY = y / 2;
									double[] YUV = new double[] {reconstructedColor[0][x][y], reconstructedColor[1][subSX][subSY], reconstructedColor[2][subSX][subSY]};
									render.setYUV(x + EndX, y + EndY, YUV);
								}
							}
						}
						
						System.out.println(String.format("Thread: %3d | Time: %6dms | List size: %5d | Total area: %8dpx", index, localTotalTime, vecList.size(), area));
					};
				
					executor.submit(task);
				}
			}
			
			executor.shutdown();
			while (!executor.awaitTermination(250, TimeUnit.MICROSECONDS)) {}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("- Total rendering time: " + totalTime + "ms");
		if (numThreads > 0L)
			System.out.println("- Avg. rendering time: " + ((double)totalTime/(double)numThreads) + "ms (" + numThreads + " threads)");
		return render;
	}
	
	private static double[][][] reconstructColors(double[][][] differenceOfColor, double[][][] referenceColor, int size) {
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
	
	public static BufferedImage[] renderQuadtree(ArrayList<MacroBlock> leaveNodes, Dimension dim) {
		BufferedImage[] render = new BufferedImage[3];
		render[0] = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		render[1] = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g2d1 = (Graphics2D)render[0].createGraphics();
		Graphics2D g2d2 = (Graphics2D)render[1].createGraphics();
		g2d1.setColor(Color.RED);
		
		for (MacroBlock leaf : leaveNodes) {
			Point pos = leaf.getPosition();
			int size = leaf.getSize();
			g2d1.drawRect(pos.x, pos.y, size, size);
			g2d1.drawLine(pos.x, pos.y, pos.x + size, pos.y + size);
			
			int[] rgb = leaf.getMeanColor();
			g2d2.setColor(new Color(rgb[0], rgb[1], rgb[2]));
			g2d2.fillRect(pos.x, pos.y, size, size);
		}
		
		g2d1.dispose();
		g2d2.dispose();
		
		return render;
	}
	
	/**
	 * <p>This function provides a good debugging base. It draws all vectors in the
	 * ArrayList to an image and returns it.</p>
	 * 
	 * <p>For better visualization the vectors have different colors:
	 * <br><br><table border="1">
	 * <tr>
	 * <td>Reference</td> <td>Assigned color</td>
	 * </tr><tr>
	 * <td>0</td> <td>Color.Orange</td>
	 * </tr><tr>
	 * <td>1</td> <td>Color.Yellow</td>
	 * </tr><tr>
	 * <td>2</td> <td>Color.Blue</td>
	 * </tr><tr>
	 * <td>3</td> <td>Color.Red</td>
	 * </tr>
	 * </table>
	 * </p>
	 * 
	 * @return Image with all vectors drawn on it
	 * 
	 * @param vecs	Vectors to draw
	 * @param dim	Dimension of the frame
	 * 
	 * @see app.interprediction.Vector
	 * @see java.awt.Color
	 */
	public static BufferedImage renderVectors(ArrayList<Vector> vecs, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)render.createGraphics();
		g2d.setColor(Color.RED);
		
		for (Vector v : vecs) {
			Point pos = v.getPosition();
			int x1 = pos.x;
			int y1 = pos.y;
			int x2 = pos.x + v.getSpanX();
			int y2 = pos.y + v.getSpanY();
			
			switch (v.getReference()) {
			case -1:
				g2d.setColor(Color.GREEN); break;
			case 0:
				g2d.setColor(Color.ORANGE); break;
			case 1:
				g2d.setColor(Color.YELLOW); break;
			case 2:
				g2d.setColor(Color.BLUE); break;
			case 3:
				g2d.setColor(Color.RED); break;
			}
			
			g2d.drawLine(x1, y1, x2, y2);
		}
		
		g2d.dispose();
		return render;
	}

	public BufferedImage renderDifferences(ArrayList<MacroBlock> leaves, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		
		for (MacroBlock b : leaves) {
			for (int x = 0; x < b.getSize(); x++) {
				for (int y = 0; y < b.getSize(); y++) {
					if (x + b.getPosition().x >= dim.width
						|| x + b.getPosition().x < 0
						|| y + b.getPosition().y >= dim.height
						|| y + b.getPosition().y < 0) {
						continue;
					}
					
					render.setRGB(x + b.getPosition().x, y + b.getPosition().y, ColorManager.convertYUVToRGB(b.getYUV(x, y)));
				}
			}
		}
		
		return render;
	}
}
