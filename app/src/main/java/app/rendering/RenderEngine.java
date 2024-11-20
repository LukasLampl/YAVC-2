package app.rendering;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import app.interprediction.Vector;
import app.quadtree.QuadtreeEngine;
import app.utils.LoadDistributor;
import app.utils.MacroBlock;
import app.utils.PixelRaster;
import app.utils.ReferenceFrameManager;

public class RenderEngine {
	public static PixelRaster renderResult(LoadDistributor<Vector> vecs, ReferenceFrameManager refs, LoadDistributor<MacroBlock> differenceManager, boolean allowModToAbsDiff) {
		long sRT = System.currentTimeMillis();
		PixelRaster render = refs.getLastFrame().copy();
		Dimension dim = refs.getLastFrame().getDimension();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		try {
			if (differenceManager != null) {
				for (final List<MacroBlock> blockList : differenceManager.getIterable()) {
					Runnable task = createMacroBlockRenderTask(blockList, dim, render);
					executor.submit(task);
				}
			}

			if (vecs != null) {
				for (final List<Vector> vecList : vecs.getIterable()) {
					Runnable task = createVectorRenderTask(vecList, refs, render, dim, allowModToAbsDiff);
					executor.submit(task);
				}
			}
			
			executor.shutdown();
			while (!executor.awaitTermination(250, TimeUnit.MICROSECONDS)) {}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long rT = (System.currentTimeMillis() - sRT);
		System.out.println(String.format("      >>> Visual render time: %4dms", rT));
		return render;
	}
	
	private static Runnable createMacroBlockRenderTask(List<MacroBlock> blockList, Dimension dim, PixelRaster render) {
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
		
		return task;
	}
	
	private static Runnable createVectorRenderTask(List<Vector> vecList, ReferenceFrameManager refs, PixelRaster render, Dimension dim, boolean allowModToAbsDiff) {
		Runnable task = () -> {
			double[][][][] pixelBlockCache = new double[QuadtreeEngine.NUMBER_OF_SIZES][][][];
			long iT = 0;
			long pBT = 0;
			long iDT = 0;
			long rT = 0;
			long pT = 0;
			
			for (Vector v : vecList) {
				long sIT = System.currentTimeMillis();
				PixelRaster referencedFrame = v.getReference() == -1 ? null : refs.getByReference(v.getReference());
				Point pos = v.getPosition();
				int endX = pos.x + v.getSpanX();
				int endY = pos.y + v.getSpanY();
				int size = v.getSize();
				iT += (System.currentTimeMillis() - sIT);
				long sPBT = System.currentTimeMillis();
				double[][][] selectedCache = pixelBlockCache[QuadtreeEngine.getIndexBySize(size)];
				double[][][] block = referencedFrame.getPixelBlock(pos, size, selectedCache);
				pBT += (System.currentTimeMillis() - sPBT);
				long sIDT = System.currentTimeMillis();
				double[][][] coeffs = v.getIDCTCoefficientsOfAbsoluteColorDifference(allowModToAbsDiff);
				iDT = (System.currentTimeMillis() - sIDT);
				//Use block as cache, because the pixel block is a allocated double[][][] from the image
				//and thus editing it won't change the original frame.
				long srT = System.currentTimeMillis();
				double[][][] reconstructedColor = reconstructColors(coeffs, block, size, block);
				rT += (System.currentTimeMillis() - srT);
				long sPT = System.currentTimeMillis();
				
				for (int x = 0; x < size; x++) {
					int posX = endX + x;
					int subSX = x / 2;
					if (posX < 0 || posX >= dim.width) continue;
					if (pos.x + x < 0 || pos.x + x >= dim.width) continue;
					
					for (int y = 0; y < size; y++) {
						int posY = endY + y;
						int subSY = y / 2;
						if (posY < 0 || posY >= dim.height) continue;
						if (pos.y + y < 0 || pos.y + y >= dim.height) continue;
						
						double Y = reconstructedColor[ColorManager.Y_INDEX][x][y];
						double U = reconstructedColor[ColorManager.U_INDEX][subSX][subSY];
						double V = reconstructedColor[ColorManager.V_INDEX][subSX][subSY];
						render.setYUV(posX, posY, Y, U, V);
					}
				}
				pT += (System.currentTimeMillis() - sPT);
			}
			System.out.println(String.format("      > Init time: %4dms | PixelBlock grab time: %4dms | IDCT time: %4dms | Reconstruction time: %4dms | Put time: %4dms", iT, pBT, iDT, rT, pT));
		};
		
		return task;
	}
	
	private static double[][][] reconstructColors(double[][][] differenceOfColor, double[][][] referenceColor, int size, double[][][] cache) {
		int halfSize = size / 2;
		double[][][] reconstructedColor = cache;
		
		if (reconstructedColor == null) {
			reconstructedColor = new double[3][][];
			reconstructedColor[ColorManager.Y_INDEX] = new double[size][size];
			reconstructedColor[ColorManager.U_INDEX] = new double[halfSize][halfSize];
			reconstructedColor[ColorManager.V_INDEX] = new double[halfSize][halfSize];
		}
		
		//Reconstruct Y-Comp
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				reconstructedColor[ColorManager.Y_INDEX][x][y] = referenceColor[ColorManager.Y_INDEX][x][y]
																+ differenceOfColor[ColorManager.Y_INDEX][x][y];
			}
		}
		
		//Reconstruct U,V-Comp
		for (int x = 0; x < halfSize; x++) {
			for (int y = 0; y < halfSize; y++) {
				reconstructedColor[ColorManager.U_INDEX][x][y] = referenceColor[ColorManager.U_INDEX][x][y]
																+ differenceOfColor[ColorManager.U_INDEX][x][y];
				reconstructedColor[ColorManager.U_INDEX][x][y] = referenceColor[ColorManager.U_INDEX][x][y]
																+ differenceOfColor[ColorManager.V_INDEX][x][y];
			}
		}
		
		return reconstructedColor;
	}
	
	public static BufferedImage[] renderQuadtree(List<MacroBlock> leaveNodes, Dimension dim) {
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
	 * @see app.interprediction.T
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
		double[] YUVCache = new double[3]; //Size of 3 because of 3 channels
		
		for (MacroBlock b : leaves) {
			for (int x = 0; x < b.getSize(); x++) {
				for (int y = 0; y < b.getSize(); y++) {
					if (x + b.getPosition().x >= dim.width
						|| x + b.getPosition().x < 0
						|| y + b.getPosition().y >= dim.height
						|| y + b.getPosition().y < 0) {
						continue;
					}
					
					int argb = ColorManager.convertYUVToRGB(b.getYUV(x, y, YUVCache));
					render.setRGB(x + b.getPosition().x, y + b.getPosition().y, argb);
				}
			}
		}
		
		return render;
	}
}
