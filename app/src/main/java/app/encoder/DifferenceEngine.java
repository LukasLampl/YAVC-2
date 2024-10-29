package app.encoder;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import app.quadtree.QuadtreeEngine;
import app.utils.MacroBlock;
import app.utils.PixelRaster;

public class DifferenceEngine {
	private static final double DELTA_Y = 1.4;
	private static final double DELTA_U = 3.2;
	private static final double DELTA_V = 3.2;
	
	public ThreadLoadManager computeDifferences(PixelRaster prevFrame, ThreadLoadManager threadLoadManager) {
		int predictedSize = threadLoadManager.getLoadNumber() / 2;
		int totalDiffSize = 0;
		ThreadLoadManager diffManager = new ThreadLoadManager();
		ArrayList<ArrayList<MacroBlock>> diffs = new ArrayList<ArrayList<MacroBlock>>();
		ArrayList<Future<ArrayList<MacroBlock>>> futureDiffs = new ArrayList<Future<ArrayList<MacroBlock>>>(predictedSize);
		
		try {
			int threads = Runtime.getRuntime().availableProcessors();
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			
			for (int i = 0; i < threadLoadManager.getNumberOfChunks(); i++) {
				final int index = i;
				
				Callable<ArrayList<MacroBlock>> task = () -> {
					ArrayList<MacroBlock> blockList = threadLoadManager.getLoadOf(index);
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
								double deltaY = refCols[0][x][y] - curCols[0][x][y];
								double deltaU = refCols[1][subSX][subSY] - curCols[1][subSX][subSY];
								double deltaV = refCols[2][subSX][subSY] - curCols[2][subSX][subSY];
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
					
					for (MacroBlock block : blockList) {
						diffManager.setBlock(block);
						totalDiffSize += block.getSquaredSize();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			executor.shutdown();
			while (!executor.awaitTermination(250, TimeUnit.MICROSECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		diffManager.compute(totalDiffSize);
		return diffManager;
	}
}
