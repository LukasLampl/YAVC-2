/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2024  Lukas Nian En Lampl

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import app.utils.LoadDistributor;
import app.utils.MacroBlock;
import app.utils.PixelRaster;

/**
 * The {@code DifferenceEngine} filters out MacroBlocks that resemble old ones.
 * Blocks are only filtered, when the colors are below a certain threshold.
 * 
 * @author Lukas Lampl
 * @since 1.2
 */
public class DifferenceEngine {
	/**
	 * The Y threshold value.
	 */
	private static final double DELTA_Y = 0.7; //1.4;
	
	/**
	 * The U threshold value.
	 */
	private static final double DELTA_U = 1.6; //3.2;
	
	/**
	 * The V threshold value.
	 */
	private static final double DELTA_V = 1.6; //3.2;
	
	/**
	 * Computes the difference by iterating over every MacroBlock, calculating
	 * the delta color of each channel and decides based on the thresholds whether
	 * the block should be processed or not.
	 * 
	 * @param prevFrame				The frame to use as a color reference.
	 * @param threadLoadManager		The LoadDistributor with all MacroBlocks.
	 * @return A list of all MacroBlocks that should be further processed.
	 */
	public List<MacroBlock> computeDifferences(PixelRaster prevFrame, LoadDistributor<MacroBlock> threadLoadManager) {
		int predictedSize = threadLoadManager.getNumberOfObjects() / 2;
		List<MacroBlock> differences = new ArrayList<MacroBlock>();
		ArrayList<Future<ArrayList<MacroBlock>>> futureDiffs = new ArrayList<Future<ArrayList<MacroBlock>>>(predictedSize);
		
		try {
			int threads = Runtime.getRuntime().availableProcessors();
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			
			for (final List<MacroBlock> blockList : threadLoadManager.getIterable()) {
				Callable<ArrayList<MacroBlock>> task = () -> {
					ArrayList<MacroBlock> blocksToReturn = new ArrayList<MacroBlock>();
					
					for (MacroBlock block : blockList) {
						int size = block.getSize();
						int squaredSize = block.getSquaredSize();
						double[][][] refCols = prevFrame.getPixelBlock(block.getPosition(), size, null);
						double[][][] curCols = block.getColors();
						
						double sumY = 0;
						double sumU = 0;
						double sumV = 0;
						
						for (int x = 0; x < size; x++) {
							int subSX = x / 2;
							
							for (int y = 0; y < size; y++) {
								int subSY = y / 2;
								double deltaY = refCols[ColorManager.Y_INDEX][x][y] - curCols[ColorManager.Y_INDEX][x][y];
								double deltaU = refCols[ColorManager.U_INDEX][subSX][subSY] - curCols[ColorManager.U_INDEX][subSX][subSY];
								double deltaV = refCols[ColorManager.V_INDEX][subSX][subSY] - curCols[ColorManager.V_INDEX][subSX][subSY];
								sumY += deltaY * deltaY;
								sumU += deltaU * deltaU;
								sumV += deltaV * deltaV;
							}
						}
						
						sumY /= squaredSize;
						sumU /= squaredSize;
						sumV /= squaredSize;
						
						if (sumY > DELTA_Y || sumU > DELTA_U || sumV > DELTA_V) {
							blocksToReturn.add(block);
						}
					}
					
					return blocksToReturn;
				};
				
				futureDiffs.add(executor.submit(task));
			}
			
			for (Future<ArrayList<MacroBlock>> diff : futureDiffs) {
				try {
					ArrayList<MacroBlock> blockList = diff.get();
					
					if (blockList == null) {
						continue;
					} else if (blockList.isEmpty()) {
						continue;
					}
					
					differences.addAll(blockList);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			executor.shutdown();
			while (!executor.awaitTermination(250, TimeUnit.MICROSECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return differences;
	}
}
