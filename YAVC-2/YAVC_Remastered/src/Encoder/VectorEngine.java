package Encoder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Main.config;
import Utils.MacroBlock;
import Utils.PixelRaster;
import Utils.Vector;

public class VectorEngine {
	private double TOTAL_MSE = 0;
	
	/*
	 * Purpose: Calculate all possible movement vectors by the differences from the prevFrame to the curFrame.
	 * 			To achieve that it uses reference images that go 4 images into the past.
	 * Return Type: ArrayList<Vector> => List of movement vectors
	 * Params: ArrayList<MacroBlock> blocksToInterpredict => Blocks to inter-predict;
	 * 			ArrayList<PixelRaster> refs => Reference images;
	 * 			PixelRaster futureFrame => Frame in the future
	 */
	public ArrayList<Vector> computeMovementVectors(ArrayList<MacroBlock> blocksToInterpredict, ArrayList<PixelRaster> refs, int colorSpectrum, PixelRaster futureFrame) {
		if (blocksToInterpredict == null || blocksToInterpredict.size() == 0) {
			System.err.println("No blocks for inter-prediction > Skip process");
			return null;
		} else if (refs == null || refs.size() == 0) {
			System.err.println("Can't compute motion vectors, due to missing references! > Skip");
			return null;
		} else if (colorSpectrum <= 0) {
			System.err.println("An Image can't have 0 or less colors! > Skip");
			return null;
		}
		
		ArrayList<Vector> vecs = new ArrayList<Vector>(blocksToInterpredict.size());
		ArrayList<Future<Vector>> futureVecs = new ArrayList<Future<Vector>>(blocksToInterpredict.size());
		
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		this.TOTAL_MSE = 0;
		
		for (MacroBlock block : blocksToInterpredict) {
			Callable<Vector> task = () -> {
				int maxSize = refs.size();
				MacroBlock[] canidates = new MacroBlock[maxSize + 1];
				
				for (int i = 0, index = 0; i < maxSize; i++, index++) {
					MacroBlock bestMatch = computeHexagonSearch(refs.get(i), block);
					
					if (bestMatch != null) {
						bestMatch.setReference(config.MAX_REFERENCES - i);
					}
					
					canidates[index] = bestMatch;
				}
				
				if (futureFrame != null) {
					MacroBlock futureMatch = computeHexagonSearch(futureFrame, block);
					if (futureMatch != null) futureMatch.setReference(-1);
					canidates[refs.size()] = futureMatch;
				}
				
				MacroBlock best = evaluateBestGuess(canidates);
				Vector vec = null;
				
				if (best != null) {
					this.TOTAL_MSE += best.getMSE();
					
					PixelRaster references = refs.get(config.MAX_REFERENCES - best.getReference());
					double[][][] referenceColor = references.getPixelBlock(best.getPosition(), block.getSize(), null);
					double[][][] absoluteColorDifference = getAbsoluteDifferenceOfColors(block.getColors(), referenceColor, block.getSize(), colorSpectrum);
					
					vec = new Vector(best.getPosition());
					vec.setAbsoluteDifferences(absoluteColorDifference);
					vec.setAppendedBlock(block);
					vec.setMostEqualBlock(best);
					vec.setSize(block.getSize());
					vec.setReference(best.getReference());
					vec.setSpanX(block.getPosition().x - best.getPosition().x);
					vec.setSpanY(block.getPosition().y - best.getPosition().y);
				}
				
				return vec;
			};
			
			futureVecs.add(executor.submit(task));
		}
		
		for (Future<Vector> fvec : futureVecs) {
			try {
				Vector vec = fvec.get();
				
				if (vec != null) {
					vecs.add(vec);
					blocksToInterpredict.remove(vec.getAppendedBlock());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		executor.shutdown();
		
		return vecs;
	}
	
	/*
	 * Purpose: The method evaluates the best guess among an array of "best matches"
	 * Return Type: MacroBlock => Best match to the searched MacroBlock
	 * Params: MacroBlock[] canidates => Candiates to check for the lowest MSE
	 */
	private MacroBlock evaluateBestGuess(MacroBlock[] canidates) {
		MacroBlock best = canidates[0];
		
		for (MacroBlock b : canidates) {
			if (b == null) continue;
			else if (best == null) best = b;
			
			if (b.getMSE() < best.getMSE()) {
				best = b;
			}
		}
		
		return best;
	}
	
	/*
	 * Purpose: Compute the hexagon-search algorithm for a MacroBlock with one reference.
	 * 			The algorithm is as followed:
	 * +---+---------------------------------------------------------------------------------------------------------+
	 * | 1 | Move a coordinate system to the Position of the block, so that the origin is a the blocks position.     |
	 * +---+---------------------------------------------------------------------------------------------------------+
	 * | 2 | Span a hexagon with the radius r and get all MSEs from the seven points. Look for the lowest.           |
	 * |   | If the lowest can be found at the center move to step 4; else to step 3.                                |
	 * +---+---------------------------------------------------------------------------------------------------------+
	 * | 3 | Set the new origin to the Point with the lowest MSE and repeat step 2.                                  |
	 * +---+---------------------------------------------------------------------------------------------------------+
	 * | 4 | Now set the radius to r /= 2 and repeat step 2, until r <= 1. If r <= 1 go to step 5.                   |
	 * +---+---------------------------------------------------------------------------------------------------------+
	 * | 5 | Check the points around the "best guess" and get the one with the lowest MSE >> this is the best match. |
	 * +---+---------------------------------------------------------------------------------------------------------+
	 * 
	 * Return Type: MacroBlock => Best match in the reference image
	 * Params: PixelRaster ref => Reference image, in which to search for the best match;
	 * 			MacroBlock blockToBeSearched => MacroBlock to be searched in the reference image.
	 */
	private MacroBlock computeHexagonSearch(PixelRaster ref, MacroBlock blockToBeSearched) {
		double lowestMSE = Double.MAX_VALUE;
		int radius = 4, searchWindow = 24, size = blockToBeSearched.getSize(), sumOfAllPoints = 2304;
		Point blockPos = blockToBeSearched.getPosition();
		Point centerPoint = blockToBeSearched.getPosition();
		MacroBlock mostEqualBlock = null;
		
		Point initPos = new Point(0, 0);
		Point[] searchPoints = new Point[7];
		HashSet<Point> searchedPoints = new HashSet<Point>(sumOfAllPoints);
		double[][][] cache = null;
		
		while (radius > 1) {
			searchPoints = getHexagonPoints(radius, centerPoint);
			
			for (Point p : searchPoints) {
				if (p.x > blockPos.x + searchWindow
					|| p.x < blockPos.x - searchWindow
					|| p.y > blockPos.y + searchWindow
					|| p.y < blockPos.y - searchWindow
					|| searchedPoints.contains(p)
					|| p.x > ref.getWidth()
					|| p.x < 0
					|| p.y > ref.getHeight()
					|| p.y < 0) {
					continue;
				}
				
				searchedPoints.add(p);
				cache = ref.getPixelBlock(p, size, cache);
				double MSE = getMSEOfColors(cache, blockToBeSearched.getColors(), size);
				
				if (MSE < lowestMSE) {
					lowestMSE = MSE;
					initPos = p;
					mostEqualBlock = new MacroBlock(p, size, cache);
				}
			}
			
			if (initPos.equals(centerPoint)) {
				radius /= 2;
				continue;
			}
			
			centerPoint = initPos;
		}
		
		searchPoints = new Point[9];
		searchPoints[0] = centerPoint;
		searchPoints[1] = new Point(centerPoint.x + radius, centerPoint.y);
		searchPoints[2] = new Point(centerPoint.x - radius, centerPoint.y);
		searchPoints[3] = new Point(centerPoint.x, centerPoint.y + radius);
		searchPoints[4] = new Point(centerPoint.x, centerPoint.y - radius);
		searchPoints[5] = new Point(centerPoint.x + radius, centerPoint.y + radius);
		searchPoints[6] = new Point(centerPoint.x - radius, centerPoint.y - radius);
		searchPoints[7] = new Point(centerPoint.x - radius, centerPoint.y + radius);
		searchPoints[8] = new Point(centerPoint.x + radius, centerPoint.y - radius);
		
		for (Point p : searchPoints) {
			if (p.x > blockPos.x + searchWindow
				|| p.x < blockPos.x - searchWindow
				|| p.y > blockPos.y + searchWindow
				|| p.y < blockPos.y - searchWindow
				|| searchedPoints.contains(p)
				|| p.x > ref.getWidth()
				|| p.x < 0
				|| p.y > ref.getHeight()
				|| p.y < 0) {
				continue;
			}
			
			cache = ref.getPixelBlock(p, size, cache);
			double MSE = getMSEOfColors(cache, blockToBeSearched.getColors(), size);
			
			if (MSE < lowestMSE) {
				lowestMSE = MSE;
				mostEqualBlock = new MacroBlock(p, size, cache);
			}
		}
		
		if (mostEqualBlock != null) mostEqualBlock.setMSE(lowestMSE);
		return mostEqualBlock;
	}
	
	/*
	 * Purpose: Provide the seven Points for the hexagon-search algorithm
	 * Return Type: Point[] => Points from the hexagon
	 * Params: int radius => Defines the radius of the hexagon;
	 * 			Point pos => Defines the position of the hexagon
	 */
	private Point[] getHexagonPoints(int radius, Point pos) {
		Point[] points = new Point[7];
		points[6] = pos;
		double Pirad = Math.PI / 3;
		
		for (int i = 0; i < 6; i++) {
			double rad = Pirad * (i + 1);
			points[i] = new Point((int)(Math.cos(rad) * radius + pos.x), (int)(Math.sin(rad) * radius + pos.y));
		}
		
		return points;
	}
	
	/*
	 * Purpose: Calculates the absolute color differences in the YUV-Colorspace, so the decoder can reconstruct
	 * 			"the original" based on all wrong coded colors during inter-prediction.
	 * Return Type: double[][][] => The deltaValues of the Y ([0]), U ([1]) and V ([2]) components between
	 * 				the first and second color;
	 * Params: double[][][] col1 => The "original" color to compare to the "best match";
	 * 			double[][][] col2 => The "best match" color from which the difference should be calculated;
	 * 			int size => Size of the color components (in width and height);
	 * 			int colors => Number of colors in the current frame (adaptive thresholds)
	 */
	private double[][][] getAbsoluteDifferenceOfColors(double[][][] col1, double[][][] col2, int size, int colors) {
		int halfSize = size / 2;
		double[][] Y = new double[size][size];
		double[][] U = new double[halfSize][halfSize];
		double[][] V = new double[halfSize][halfSize];
		
		double YThreshold = colors / 12000, UVThreshold = colors / 6000;
		
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double diff = col1[0][x][y] - col2[0][x][y];
				
				if (Math.abs(diff) > YThreshold) {
					Y[x][y] = diff;
				}
			}
		}
		
		for (int y = 0; y < halfSize; y++) {
			for (int x = 0; x < halfSize; x++) {
				double diffU = col1[1][x][y] - col2[1][x][y];
				double diffV = col1[2][x][y] - col2[2][x][y];
				
				if (Math.abs(diffU) > UVThreshold) {
					U[x][y] = diffU;
				}
				
				if (Math.abs(diffV) > UVThreshold) {
					V[x][y] = diffV;
				}
			}
		}
		
		return new double[][][] {Y, U, V};
	}
	
	/*
	 * Purpose: Calculates the MSE (Mean Square Error) between two YUV color components.
	 * 			The U and V are both subsampled to 4:2:0.
	 * Return Type: double => MSE between both colors
	 * Params: double[][][] col1 => Color component 1, containing Y at [0], U at [1], V at [2]
	 * 			and A at [3];
	 * 			double[][][] col2 => Color component 2, containing Y at [0], U at [1], V at [2]
	 * 			and A at [3];
	 * 			int size => Size of both color components (in width and height)
	 */
	private double getMSEOfColors(double[][][] col1, double[][][] col2, int size) {
		double resY = 0;
		double resU = 0;
		double resV = 0;
		double resA = 0;
		int halfSize = size / 2;
		
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double deltaY = col1[0][x][y] - col2[0][x][y];
				double deltaA = col1[3][x][y] - col2[3][x][y];
				
				resY += deltaY * deltaY;
				resA += deltaA * deltaA;
			}
		}
		
		for (int y = 0; y < halfSize; y++) {
			for (int x = 0; x < halfSize; x++) {
				double deltaU = col1[1][x][y] - col2[1][x][y];
				double deltaV = col1[2][x][y] - col2[2][x][y];
				
				resU += deltaU * deltaU;
				resV += deltaV * deltaV;
			}
		}
		
		resY *= resY;
		resA = Math.pow(resA, resA);
		return ((resY + resU + resV + resA) / (size * size * 4));
	}
	
	/*
	 * Purpose: Returns the sum of all MSEs from all matching vectors
	 * Return Type: void
	 * Params: void
	 */
	public double getVectorMSE() {
		return this.TOTAL_MSE;
	}
	
	/*
	 * Purpose: This function is DEBUG ONLY and returns a BufferedImage that contains all vectors drawn on it.
	 * 			The Vectors have different colors according to their "origin". Look at the table below
	 * +-----------+-------+--------+--------+-------+-------+
	 * | Reference |  -1   |    0   |    1   |   2   |   3   |
	 * +-----------+-------+--------+--------+-------+-------+
	 * |   Color   | GREEN | ORANGE | YELLOW |  BLUE |  RED  |
	 * +-----------+-------+--------+--------+-------+-------+
	 * Return Type: BufferedImage => Image with all vectors drawn on it
	 * Params: ArrayList<Vector> vecs => Vectors to draw;
	 * 			Dimension dim => Dimension of the video (width x height)
	 */
	public BufferedImage drawVectors(ArrayList<Vector> vecs, Dimension dim) {
		BufferedImage render = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D)render.createGraphics();
		g2d.setColor(Color.RED);
		
		for (Vector v : vecs) {
			Point pos = v.getPosition();
			int x1 = pos.x, y1 = pos.y;
			int x2 = pos.x + v.getSpanX(), y2 = pos.y + v.getSpanY();
			
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
}
