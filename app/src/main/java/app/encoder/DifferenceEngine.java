package app.encoder;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import app.utils.ColorManager;
import app.utils.MacroBlock;
import app.utils.PixelRaster;

public class DifferenceEngine {
	private static final double DELTA_Y = 1.4;
	private static final double DELTA_U = 3.2;
	private static final double DELTA_V = 3.2;
	
	public ArrayList<MacroBlock> computeDifferences(PixelRaster prevFrame, ArrayList<ArrayList<MacroBlock>> leaveNodes) {
		ArrayList<MacroBlock> diffs = new ArrayList<MacroBlock>(leaveNodes.size() / 2);
		ArrayList<Future<MacroBlock>> futureDiffs = new ArrayList<Future<MacroBlock>>(leaveNodes.size() / 2);

		try {
			int threads = Runtime.getRuntime().availableProcessors();
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			
			for (MacroBlock block : leaveNodes) {
				Callable<MacroBlock> task = () -> {
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
						return block;
					}
					
					return null;
				};
				
				futureDiffs.add(executor.submit(task));
			}
			
			for (Future<MacroBlock> diff : futureDiffs) {
				try {
					MacroBlock block = diff.get();
					
					if (block != null) {
						diffs.add(block);
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
		
		return diffs;
	}
}
