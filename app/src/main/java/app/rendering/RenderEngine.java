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

package app.rendering;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import app.engines.prediction.interprediction.Vector;
import app.engines.prediction.intraprediction.IntraDecoder;
import app.engines.prediction.intraprediction.IntraPredictionBlock;
import app.engines.quadtree.QuadtreeEngine;
import app.managers.LoadDistributor;
import app.managers.ReferenceFrameManager;
import app.utils.MacroBlock;
import app.utils.PixelRaster;

/**
 * The {@code RenderEngine} is one of the main parts and is responsible
 * for the whole rendering pipeline of the YAVC program. The {@code RenderEngine}
 * provides basic utilities for rendering intermediate YAVC steps, like the
 * differences, the Quadtree, the Vectors and the composit image.
 * 
 * @author Lukas Lampl
 * @since 1.1
 */
public class RenderEngine {
	private static IntraDecoder intraDecoder = new IntraDecoder();
	
	/**
	 * Renders a composited image of all vectors, non-coded blocks and reference
	 * frames used. It runs asynchronously to achieve a higher throughput.
	 * 
	 * @param vecs					The vector to use for the composit.
	 * @param refs					The reference frames used by the vectors.
	 * @param differenceManager		All non-coded MacroBlocks.
	 * @param allowModToAbsDiff		Flag for whether modifications can be made to the vectors absolute color difference or not.
	 * @return A composit PixelRaster with all vectors, non-coded blocks and reference used.
	 */
	public static PixelRaster renderComposit(LoadDistributor<Vector> vecs, ReferenceFrameManager refs,
			LoadDistributor<MacroBlock> differenceManager, LoadDistributor<IntraPredictionBlock> intraBlocks,
			boolean allowModToAbsDiff) {
		long sRT = System.currentTimeMillis();
		PixelRaster render = refs.getLastFrame().copy();
		Dimension dim = refs.getLastFrame().getDimension();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		try {
			if (differenceManager != null) {
				if (differenceManager.hasDistributed()) {
					for (final List<MacroBlock> blockList : differenceManager.getIterable()) {
						Runnable task = createMacroBlockRenderTask(blockList, dim, render);
						executor.submit(task);
					}
				}
			}

			long s_vrT = System.currentTimeMillis();
			if (vecs != null) {
				if (vecs.hasDistributed()) {
					for (final List<Vector> vecList : vecs.getIterable()) {
						Runnable task = createVectorRenderTask(vecList, refs, render, dim, allowModToAbsDiff);
						executor.submit(task);
					}
				}
			}
			
			if (intraBlocks != null) {
				if (intraBlocks.hasDistributed()) {
					for (final List<IntraPredictionBlock> intraBlockList : intraBlocks.getIterable()) {
						Runnable task = createIntrapredictionBlockRenderTask(intraBlockList, dim, render);
						executor.submit(task);
					}
				}
			}
			
			executor.shutdown();
			while (!executor.awaitTermination(250, TimeUnit.MICROSECONDS)) {}
			long e_vrT = System.currentTimeMillis();
			System.out.println(String.format("      >>> Vector render time: %4dms", (e_vrT - s_vrT)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long rT = (System.currentTimeMillis() - sRT);
		System.out.println(String.format("      >>> Visual render time: %4dms", rT));
		return render;
	}
	
	private static Runnable createIntrapredictionBlockRenderTask(List<IntraPredictionBlock> blockList, Dimension dim,
			PixelRaster render) {
		Runnable task = () -> {
			for (IntraPredictionBlock block : blockList) {
				MacroBlock b = new MacroBlock(block.getPosX(), block.getPosY(), block.getSize(), true);
				final int size = b.getSize();
				final Point pos = b.getPosition();
				intraDecoder.computeAngularIntraPredictionBlock(b, block.getVertical(), block.getHorizontal(), block.getAngle(), dim);
				double[][][] deltas = block.getDelta();
				
				for (int x = 0; x < size; x++) {
					if (pos.x + x < 0 || pos.x + x >= dim.width) continue;
					
					for (int y = 0; y < size; y++) {
						if (pos.y + y < 0 || pos.y + y >= dim.height) continue;
						
						double[] YUV = b.getYUV(x, y);
						YUV[ColorManager.Y_INDEX] += deltas[ColorManager.Y_INDEX][x][y];
						YUV[ColorManager.U_INDEX] += deltas[ColorManager.U_INDEX][x / 2][y / 2];
						YUV[ColorManager.V_INDEX] += deltas[ColorManager.V_INDEX][x / 2][y / 2];
						render.setYUV(x + pos.x, y + pos.y, YUV);
					}
				}
			}
		};
		
		return task;
	}
	
	/**
	 * Creates a runnable task for rendering a list of MacroBlocks into a
	 * given PixelRaster.
	 * 
	 * @param blockList	The MacroBlocks to render.
	 * @param dim		The Dimension of the frame.
	 * @param render	Frame in which to render into.
	 * @return A runnable task, executing rendering a list of MacroBlocks.
	 */
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
	
	/**
	 * Creates a runnable task for rendering vectors into a given PixelRaster.
	 * 
	 * @param vecList			The vectors to render.
	 * @param refs				All reference frames used by the vectors.
	 * @param render			The PixelRaster in which to write.
	 * @param dim				Dimension of the PixelRaster.
	 * @param allowModToAbsDiff	Flag for whether the vector absolute color difference data can be modified or not.
	 * @return A runnable task, which renders all given vectors onto the given frame.
	 */
	private static Runnable createVectorRenderTask(List<Vector> vecList, ReferenceFrameManager refs, PixelRaster render, Dimension dim, boolean allowModToAbsDiff) {
		Runnable task = () -> {
			double[][][][] pixelBlockCache = new double[QuadtreeEngine.NUMBER_OF_SIZES][][][];
			long iT = 0;
			long pBT = 0;
			long iDT = 0;
			long rT = 0;
			long pT = 0;
			
			for (final Vector v : vecList) {
				long sIT = System.currentTimeMillis();
				final PixelRaster referencedFrame = v.getReference() == -1 ? null : refs.getByReference(v.getReference());
				Point pos = v.getPosition();
				final int endX = pos.x + v.getSpanX();
				final int endY = pos.y + v.getSpanY();
				final int size = v.getSize();
				try {
				final double[][][] selectedCache = pixelBlockCache[QuadtreeEngine.getIndexBySize(size)];
				iT += (System.currentTimeMillis() - sIT);
				long sPBT = System.currentTimeMillis();
				final double[][][] block = referencedFrame.getPixelBlock(pos, size, selectedCache);
				pBT += (System.currentTimeMillis() - sPBT);
				long sIDT = System.currentTimeMillis();
				double[][][] coeffs = v.getIDCTCoefficientsOfAbsoluteColorDifference(allowModToAbsDiff);
				iDT = (System.currentTimeMillis() - sIDT);
				//Use block as cache, because the pixel block is a allocated double[][][] from the image
				//and thus editing it won't change the original frame.
				long srT = System.currentTimeMillis();
				final double[][][] reconstructedColor = reconstructColors(coeffs, block, size, block);
				rT += (System.currentTimeMillis() - srT);
				long sPT = System.currentTimeMillis();

				for (int x = 0; x < size; x++) {
					final int posX = endX + x;
					final int subSX = x / 2;
					if (posX < 0 || posX >= dim.width) continue;
					if (pos.x + x < 0 || pos.x + x >= dim.width) continue;
					
					for (int y = 0; y < size; y++) {
						final int posY = endY + y;
						final int subSY = y / 2;
						if (posY < 0 || posY >= dim.height) continue;
						if (pos.y + y < 0 || pos.y + y >= dim.height) continue;
						
						final double Y = reconstructedColor[ColorManager.Y_INDEX][x][y];
						final double U = reconstructedColor[ColorManager.U_INDEX][subSX][subSY];
						final double V = reconstructedColor[ColorManager.V_INDEX][subSX][subSY];
						render.setYUV(posX, posY, Y, U, V);
					}
				}
				pT += (System.currentTimeMillis() - sPT);
				} catch (Exception e) {e.printStackTrace(); System.exit(0);}
			}

			System.out.println(String.format("      > Init time: %4dms | PixelBlock grab time: %4dms | IDCT time: %4dms | Reconstruction time: %4dms | Put time: %4dms", iT, pBT, iDT, rT, pT));
		};
		
		return task;
	}
	
	/**
	 * Reconstructs the "original" color by adding the difference back to the
	 * data referenced by the vector.
	 * 
	 * <p><b>Help:</b><br>
	 * Let's say we have 3 variables, {@code a} {@code b} and {@code c}. {@code a}
	 * stands for the color at the exact position as {@code b} in the referenced
	 * frame. But {@code b} is the color that is needed to get the color {@code c},
	 * which is the color in the actual frame. This means to get the difference or
	 * {@code b} we have to solve for {@code b}:<br>
	 * 
	 * <blockquote>
	 *     {@code a} + {@code b} = {@code c} | -{@code a}<br>
	 * <=> {@code b} = {@code c} - {@code a}<br>
	 * </blockquote>
	 * 
	 * To get out color, we want to reconstruct, we just reverse and solve for {@code c}:<br>
	 * 
	 * <blockquote>
	 *     {@code b} = {@code c} - {@code a} | +{@code a}<br>
	 * <=> {@code a} + {@code b} = {@code c}<br>
	 * </blockquote></p>
	 * 
	 * @param differenceOfColor	The color difference of a vector.
	 * @param referenceColor	The original color to which the add the difference to get the "original" in the next frame.
	 * @param size				Size of the difference color.
	 * @param cache				Caching structure. (To reduce GC pressure)
	 * @return An 3D array with the "original" reconstructed colors.
	 */
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
				reconstructedColor[ColorManager.V_INDEX][x][y] = referenceColor[ColorManager.V_INDEX][x][y]
																+ differenceOfColor[ColorManager.V_INDEX][x][y];
			}
		}
		
		return reconstructedColor;
	}
	
	/**
	 * Renders an image of the quadtree with the given leave nodes.
	 * 
	 * @param leaveNodes	The leave nodes of the quadtree.
	 * @param dim			Dimension of the frame.
	 * @return An array of BufferedImages, where at:
	 * <ul>
	 * <li>[0] - The quadtree boxes can be seen.
	 * <li>[1] - The mean color of each MacroBlock can be seen.
	 * <li>[2] - The fused image.
	 * </ul>
	 */
	public static BufferedImage[] renderQuadtree(List<MacroBlock> leaveNodes, Dimension dim, PixelRaster curFrame) {
		int[] colorCache = new int[3];
		BufferedImage[] render = new BufferedImage[3];
		render[0] = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		render[1] = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		render[2] = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g2d1 = (Graphics2D)render[0].createGraphics();
		Graphics2D g2d2 = (Graphics2D)render[1].createGraphics();
		Graphics2D g2d3 = (Graphics2D)render[2].createGraphics();
		g2d1.setColor(Color.RED);
		
		for (MacroBlock leaf : leaveNodes) {
			Point pos = leaf.getPosition();
			int size = leaf.getSize();
			g2d1.drawRect(pos.x, pos.y, size, size);
			g2d1.drawLine(pos.x, pos.y, pos.x + size, pos.y + size);
			
			int[] rgb = ColorManager.convertYUVToRGB_intARR(leaf.getMeanColor(), colorCache);
			g2d2.setColor(new Color(rgb[ColorManager.R_INDEX], rgb[ColorManager.G_INDEX], rgb[ColorManager.B_INDEX]));
			g2d2.fillRect(pos.x, pos.y, size, size);
		}
		
		g2d3.drawImage(curFrame.toBufferedImage(), 0, 0, null);
		g2d3.drawImage(render[0], 0, 0, null);
		
		g2d1.dispose();
		g2d2.dispose();
		g2d3.dispose();
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
	public static BufferedImage renderVectors(List<Vector> vecs, Dimension dim) {
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
			case 0:
				g2d.setColor(Color.ORANGE); break;
			case 1:
				g2d.setColor(Color.YELLOW); break;
			case 2:
				g2d.setColor(Color.BLUE); break;
			case 3:
				g2d.setColor(Color.RED); break;
			case 4:
				g2d.setColor(Color.MAGENTA); break;
			case 5:
				g2d.setColor(Color.GREEN); break;
			case 6:
				g2d.setColor(Color.CYAN); break;
			case 7:
				g2d.setColor(Color.GRAY); break;
			}
			
			g2d.drawLine(x1, y1, x2, y2);
		}
		
		g2d.dispose();
		return render;
	}
	
	/**
	 * Renders the given differences an returns the image.
	 * 
	 * <p><b>Notice:</b><br>
	 * The difference of the frame is meant, this means what color has
	 * changed over the period of time. Thus some parts might be transparent,
	 * if no difference was found.
	 * </p>
	 * 
	 * @param leaves	The differences.
	 * @param dim		Dimension of the render.
	 * @return An image with all differences.
	 */
	public static BufferedImage renderDifferences(List<MacroBlock> leaves, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D) render.getGraphics();
		try {
			g2d.setColor(new Color(255, 0, 0));
			double[] YUVCache = new double[3]; // Size of 3 because of 3 channels

			for (MacroBlock b : leaves) {
				for (int x = 0; x < b.getSize(); x++) {
					for (int y = 0; y < b.getSize(); y++) {
						if (x + b.getPosition().x >= dim.width || x + b.getPosition().x < 0
								|| y + b.getPosition().y >= dim.height || y + b.getPosition().y < 0) {
							continue;
						}

						int argb = ColorManager.convertYUVToRGB(b.getYUV(x, y, YUVCache));
						render.setRGB(x + b.getPosition().x, y + b.getPosition().y, argb);
					}
				}

//				g2d.drawRect(b.getPositionX(), b.getPositionY(), b.getSize(), b.getSize());
			}
		} finally {
			g2d.dispose();
		}
		return render;
	}
	
	public static BufferedImage[] renderIntraPredictionDeltas(LoadDistributor<IntraPredictionBlock> intraPredictedBlocks,
			Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		BufferedImage render2 = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		
		for (List<IntraPredictionBlock> blockList : intraPredictedBlocks.getIterable()) {
			for (IntraPredictionBlock b : blockList) {
				if (b == null) continue;
				
				final double[][][] deltas = b.getDelta();
				
				for (int x = 0; x < b.getSize(); x++) {
					final int imgX = x + b.getPosX();
					
					for (int y = 0; y < b.getSize(); y++) {
						final int imgY = y + b.getPosY();
						final double Y = deltas[ColorManager.Y_INDEX][x][y];
						final double U = deltas[ColorManager.U_INDEX][x / 2][y / 2];
						final double V = deltas[ColorManager.V_INDEX][x / 2][y  / 2];
						render.setRGB(imgX, imgY, ColorManager.convertYUVToRGB(new double[] {Y, U, V}));
					}
				}
			}
		}
		
		for (List<IntraPredictionBlock> blockList : intraPredictedBlocks.getIterable()) {
			for (IntraPredictionBlock b : blockList) {
				if (b == null) continue;
				
				final MacroBlock m = b.getAppendedBlock();
				
				final double[][][] deltas = b.getDelta();
				final double[][][] color = m.getColors();
				
				for (int x = 0; x < b.getSize(); x++) {
					final int imgX = x + b.getPosX();
					
					for (int y = 0; y < b.getSize(); y++) {
						final int imgY = y + b.getPosY();
						
						final double Y = color[ColorManager.Y_INDEX][x][y] + deltas[ColorManager.Y_INDEX][x][y];
						final double U = color[ColorManager.U_INDEX][x / 2][y / 2] + deltas[ColorManager.U_INDEX][x / 2][y / 2];
						final double V = color[ColorManager.V_INDEX][x / 2][y / 2] + deltas[ColorManager.V_INDEX][x / 2][y / 2];
						
						render2.setRGB(imgX, imgY, ColorManager.convertYUVToRGB(new double[] {Y, U, V}));
					}
				}
			}
		}
		
		return new BufferedImage[] {render, render2};
	}
	
	public static BufferedImage renderIntraPrediction(List<MacroBlock> intraBlocks, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)render.getGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
		g2d.setColor(new Color(255, 0, 0, 100));
		double[] YUVCache = new double[3]; //Size of 3 because of 3 channels
		
		for (MacroBlock b : intraBlocks) {
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
		
		for (MacroBlock b : intraBlocks) {
			g2d.drawRect(b.getPositionX(), b.getPositionY(), b.getSize(), b.getSize());
		}
		
		g2d.dispose();
		return render;
	}
}
