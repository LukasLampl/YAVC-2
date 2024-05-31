package YAVC.Encoder;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import YAVC.Utils.ColorManager;
import YAVC.Utils.MacroBlock;
import YAVC.Utils.PixelRaster;

public class DifferenceEngine {
	public ArrayList<MacroBlock> computeDifferences(PixelRaster prevFrame, ArrayList<MacroBlock> leaveNodes) {
		ArrayList<MacroBlock> diffs = new ArrayList<MacroBlock>(leaveNodes.size() / 2);
		ArrayList<Future<MacroBlock>> futureDiffs = new ArrayList<Future<MacroBlock>>(leaveNodes.size() / 2);

		try {
			int threads = Runtime.getRuntime().availableProcessors();
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			
			for (MacroBlock block : leaveNodes) {
				Callable<MacroBlock> task = () -> {
					int size = block.getSize();
					double[][][] refCols = prevFrame.getPixelBlock(block.getPosition(), size, null);
					double[][][] curCols = block.getColors();
					
					double sumY = 0;
					double sumU = 0;
					double sumV = 0;
					
					for (int x = 0; x < size; x++) {
						for (int y = 0; y < size; y++) {
							int subSX = x / 2;
							int subSY = y / 2;
							double deltaY = refCols[0][x][y] - curCols[0][x][y];
							double deltaU = refCols[1][subSX][subSY] - curCols[1][subSX][subSY];
							double deltaV = refCols[2][subSX][subSY] - curCols[2][subSX][subSY];
							sumY += deltaY * deltaY;
							sumU += deltaU * deltaU;
							sumV += deltaV * deltaV;
						}
					}
					
					sumY /= size * size;
					sumU /= size * size;
					sumV /= size * size;
					
					if (sumY > 1.55 || sumU > 3.6 || sumV > 3.6) {
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
			while (!executor.awaitTermination(20, TimeUnit.NANOSECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return diffs;
	}
	
	public BufferedImage drawDifferences(ArrayList<MacroBlock> leaves, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		ColorManager colManager = new ColorManager();
		
		for (MacroBlock b : leaves) {
			for (int x = 0; x < b.getSize(); x++) {
				for (int y = 0; y < b.getSize(); y++) {
					if (x + b.getPosition().x >= dim.width
						|| x + b.getPosition().x < 0
						|| y + b.getPosition().y >= dim.height
						|| y + b.getPosition().y < 0) {
						continue;
					}
					
					render.setRGB(x + b.getPosition().x, y + b.getPosition().y, colManager.convertYUVToRGB(b.getYUV(x, y)));
				}
			}
		}
		
		return render;
	}
}
