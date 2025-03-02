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

package app.rendering.renderers.composit;

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import app.engines.prediction.interprediction.DecodingVector;
import app.engines.prediction.interprediction.EncodingVector;
import app.engines.prediction.interprediction.Vector;
import app.engines.prediction.intraprediction.DecodingIntraPredictionBlock;
import app.engines.prediction.intraprediction.EncodingIntraPredictionBlock;
import app.engines.prediction.intraprediction.IntraPredictionBlock;
import app.engines.prediction.intraprediction.decoding.IntraDecoder;
import app.managers.LoadDistributor;
import app.managers.ReferenceFrameManager;
import app.rendering.ColorManager;
import app.utils.Mode;
import app.utils.PixelRaster;
import app.utils.components.StaticMacroBlock;

public class CompositRenderer {
	public static PixelRaster renderComposit(LoadDistributor<? extends Vector> vecs, ReferenceFrameManager refs,
			LoadDistributor<? extends IntraPredictionBlock> intraBlocks,
			final Mode mode) {
		long sRT = System.currentTimeMillis();
		PixelRaster render = refs.getLastFrame().copy();
		Dimension dim = refs.getLastFrame().getDimension();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		try {
			long s_vrT = System.currentTimeMillis();
			if (vecs != null) {
				if (vecs.hasDistributed()) {
					for (final List<? extends Vector> vecList : vecs.getIterable()) {
						Runnable task = createVectorRenderTask(vecList, refs, render,
								dim, mode);
						executor.submit(task);
					}
				}
			}
			
			if (intraBlocks != null) {
				if (intraBlocks.hasDistributed()) {
					for (final List<? extends IntraPredictionBlock> intraBlockList : intraBlocks.getIterable()) {
						Runnable task = createIntrapredictionBlockRenderTask(intraBlockList,
								dim, render, mode);
						executor.submit(task);
					}
				}
			}
			
			executor.shutdown();
			while (!executor.awaitTermination(1, TimeUnit.MILLISECONDS)) {}
			long e_vrT = System.currentTimeMillis();
			System.out.println(String.format("      >>> Render time: %4dms", (e_vrT - s_vrT)));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long rT = (System.currentTimeMillis() - sRT);
		System.out.println(String.format("      >>> Visual render time: %4dms", rT));
		return render;
	}
	
	private static Runnable createIntrapredictionBlockRenderTask(List<? extends IntraPredictionBlock> blockList, Dimension dim,
			PixelRaster render, final Mode mode) {
		Runnable task = () -> {
			final StaticMacroBlock tempBlock = new StaticMacroBlock(0, 0, 0, true);
			final double[] YUVCache = new double[ColorManager.CHANNELS];
			
			for (final IntraPredictionBlock block : blockList) {
				final int size = block.getSize();
				final Point pos = block.getPosition();
				tempBlock.setPosition(pos.x, pos.y);
				tempBlock.mockSize(size);
				IntraDecoder.computeAngularIntraPredictionBlock(tempBlock, block.getVertical(), block.getHorizontal(), block.getAngle(), dim);
				final double[][][] deltas = mode == Mode.ENCODE ? ((EncodingIntraPredictionBlock)block).getIDCTOfDeltas()
																: ((DecodingIntraPredictionBlock)block).getYUVDeltas();
				final double[][] delta_Y = deltas[ColorManager.Y_INDEX];
				final double[][] delta_U = deltas[ColorManager.U_INDEX];
				final double[][] delta_V = deltas[ColorManager.V_INDEX];
				
				for (int x = 0; x < size; x++) {
					final int actualPosX = x + pos.x;
					if (actualPosX < 0 || actualPosX >= dim.width) continue;
					
					final int halfX = x >> 1;
					final double[] delta_Y_row = delta_Y[x];
					final double[] delta_U_row = delta_U[halfX];
					final double[] delta_V_row = delta_V[halfX];
					
					for (int y = 0; y < size; y++) {
						final int actualPosY = pos.y + y;
						if (actualPosY < 0 || actualPosY >= dim.height) continue;
						
						final int halfY = y >> 1;
						final double[] YUV = tempBlock.getYUV(x, y, YUVCache);
						YUV[ColorManager.Y_INDEX] += delta_Y_row[y];
						YUV[ColorManager.U_INDEX] += delta_U_row[halfY];
						YUV[ColorManager.V_INDEX] += delta_V_row[halfY];
						render.setYUV(actualPosX, actualPosY, YUV);
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
	 * @param mode				Mode with which the function was called.
	 * @return A runnable task, which renders all given vectors onto the given frame.
	 */
	private static Runnable createVectorRenderTask(List<? extends Vector> vecList, ReferenceFrameManager refs,
			PixelRaster render, Dimension dim, final Mode mode) {
		Runnable task = () -> {
			final StaticMacroBlock tempBlock = new StaticMacroBlock(0, 0, 0, true);
			final double[] YUVCache = new double[ColorManager.CHANNELS];
			
			for (final Vector v : vecList) {
				final PixelRaster referencedFrame = v.getReference() == -1 ? null : refs.getByReference(v.getReference());
				final Point pos = v.getPosition();
				final int endX = pos.x + v.getSpanX();
				final int endY = pos.y + v.getSpanY();
				final int size = v.getSize();
				tempBlock.setColorComponents(referencedFrame.getPixelBlock(pos, size, tempBlock.getColors()));
				double[][][] deltas =  mode == Mode.ENCODE ? ((EncodingVector)v).getIDCTOfDeltas()
															: ((DecodingVector)v).getYUVDeltas();
				final double[][] delta_Y = deltas[ColorManager.Y_INDEX];
				final double[][] delta_U = deltas[ColorManager.U_INDEX];
				final double[][] delta_V = deltas[ColorManager.V_INDEX];
				
				for (int x = 0; x < size; x++) {
					final int posX = endX + x;
					final int actualPosX = x + pos.x;
					if (actualPosX < 0 || actualPosX >= dim.width
							|| posX < 0 || posX > dim.width) continue;
					
					final int halfX = x >> 1;
					final double[] delta_Y_row = delta_Y[x];
					final double[] delta_U_row = delta_U[halfX];
					final double[] delta_V_row = delta_V[halfX];
					
					for (int y = 0; y < size; y++) {
						final int posY = endY + y;
						final int actualPosY = pos.y + y;
						if (actualPosY < 0 || actualPosY >= dim.height
								|| posY < 0 || posY > dim.height) continue;
						
						final int halfY = y >> 1;
						final double[] YUV = tempBlock.getYUV(x, y, YUVCache);
						YUV[ColorManager.Y_INDEX] += delta_Y_row[y];
						YUV[ColorManager.U_INDEX] += delta_U_row[halfY];
						YUV[ColorManager.V_INDEX] += delta_V_row[halfY];
						render.setYUV(posX, posY, YUV);
					}
				}
			}
		};
		
		return task;
	}
}
