package utils;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import app.config;

public class RenderEngine {
	public static PixelRaster renderResult(ArrayList<Vector> vecs, ArrayList<PixelRaster> refs, ArrayList<MacroBlock> diffs, PixelRaster prevFrame) {
		PixelRaster render = prevFrame.copy();
		Dimension dim = prevFrame.getDimension();
		ExecutorService executor = Executors.newCachedThreadPool();
		
		try {
			if (diffs != null) {
				for (MacroBlock block : diffs) {
					Runnable task = () -> {
						Point pos = block.getPosition();
						int size = block.getSize();
						
						for (int x = 0; x < size; x++) {
							if (pos.x + x < 0 || pos.x + x >= dim.width) continue;
							
							for (int y = 0; y < size; y++) {
								if (pos.y + y < 0 || pos.y + y >= dim.height) continue;
								
								render.setYUV(x + pos.x, y + pos.y, block.getYUV(x, y));
							}
						}
					};
					
					executor.submit(task);
				}
			}
			
			if (vecs != null) {
				for (Vector v : vecs) {
					Runnable task = () -> {
						PixelRaster cache = v.getReference() == -1 ? null : refs.get(config.MAX_REFERENCES - v.getReference());
						Point pos = v.getPosition();
						int EndX = pos.x + v.getSpanX();
						int EndY = pos.y + v.getSpanY();
						int size = v.getSize();
						double[][][] reconstructedColor = reconstructColors(v.getIDCTCoefficientsOfAbsoluteColorDifference(false), cache.getPixelBlock(pos, size, null), size);
						
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
					};
					
					executor.submit(task);
				}
			}
			
			executor.shutdown();
			while (!executor.awaitTermination(20, TimeUnit.NANOSECONDS)) {}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
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
}
