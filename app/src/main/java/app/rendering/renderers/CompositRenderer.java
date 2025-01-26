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

package app.rendering.renderers;

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import app.engines.prediction.interprediction.Vector;
import app.engines.prediction.intraprediction.IntraDecoder;
import app.engines.prediction.intraprediction.IntraPredictionBlock;
import app.engines.quadtree.QuadtreeBase;
import app.managers.LoadDistributor;
import app.managers.ReferenceFrameManager;
import app.rendering.ColorManager;
import app.utils.MacroBlock;
import app.utils.PixelRaster;

public class CompositRenderer {
	public static PixelRaster renderComposit(LoadDistributor<Vector> vecs, ReferenceFrameManager refs, LoadDistributor<IntraPredictionBlock> intraBlocks,
			boolean allowModToAbsDiff) {
		long sRT = System.currentTimeMillis();
		PixelRaster render = refs.getLastFrame().copy();
		Dimension dim = refs.getLastFrame().getDimension();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		try {
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
				IntraDecoder.computeAngularIntraPredictionBlock(b, block.getVertical(), block.getHorizontal(), block.getAngle(), dim);
				double[][][] deltas = block.getIDCTCoefficientsDelta(false);
				
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
			double[][][][] pixelBlockCache = new double[QuadtreeBase.NUMBER_OF_SIZES][][][];
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
				final double[][][] selectedCache = pixelBlockCache[QuadtreeBase.getIndexBySize(size)];
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
}
