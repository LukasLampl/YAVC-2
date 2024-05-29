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

package YAVC.Encoder;

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
import java.util.concurrent.TimeUnit;

import YAVC.Main.config;
import YAVC.Utils.MacroBlock;
import YAVC.Utils.PixelRaster;
import YAVC.Utils.Vector;

/**
 * <p>The class {@code YAVC.Encoder.VectorEngine} contains all functions
 * for the inter-prediction process in the YAVC video compressor.</p>
 * <p>First hexagonal search is executed followed by exhaustive search.</p>
 * 
 * <p><strong>Performance warning:</strong> Even though this process is
 * multithreaded, it might impact the overall performance due to increasing
 * amount of data on larger frames.</p>
 * 
 * @author Lukas Lampl
 * @since 1.0
 */

public class VectorEngine {
	
	/**
	 * <p>Variable to store the total MSE of all "best matches".</p>
	 */
	private double TOTAL_MSE = 0;
	
	/**
	 * <p>Calculates all possible movement vectors from the current frame to
	 * an list of reference frames.</p>
	 * <p>Due to the nature of block-matching there is no "100% perfect fit" block,
	 * but in order to restore most of the information as possible without affecting
	 * the overall compression ratio, the differences are stored too.</p>
	 * 
	 * @return An ArrayList filled with all movement vectors.
	 * 
	 * @param blocksToInterpredict	MacroBlocks to search a match for
	 * @param refs	Reference frames that are allowed to use during the search
	 *
	 * @throws NullPointerException	When no MacroBlocks are passed for prediction or
	 * if no references to refer to are available
	 * 
	 * @see YAVC.Utils.Vector
	 */
	public ArrayList<Vector> computeMovementVectors(ArrayList<MacroBlock> blocksToInterpredict, ArrayList<PixelRaster> refs) {
		if (blocksToInterpredict == null || blocksToInterpredict.size() == 0) {
			throw new NullPointerException("No blocks to inter-predict");
		} else if (refs == null || refs.size() == 0) {
			throw new NullPointerException("No reference frame to refere to");
		}
		
		ArrayList<Vector> vecs = new ArrayList<Vector>(blocksToInterpredict.size());
			
		try {
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
						bestMatch = computeExhaustiveSearch(block, bestMatch, refs.get(i));
						
						if (bestMatch != null) {
							bestMatch.setReference(config.MAX_REFERENCES - i);
						}
						
						canidates[index] = bestMatch;
					}
					
					MacroBlock best = evaluateBestGuess(canidates);
					Vector vec = null;
					
					if (best != null) {
						PixelRaster references = refs.get(config.MAX_REFERENCES - best.getReference());
						double[][][] referenceColor = references.getPixelBlock(best.getPosition(), block.getSize(), null);
						double[][][] absoluteColorDifference = getAbsoluteDifferenceOfColors(block.getColors(), referenceColor, block.getSize());
						double newMatchMSE = getMSEOfColors(absoluteColorDifference, block.getColors(), block.getSize(), false);
						this.TOTAL_MSE += newMatchMSE;
						
						vec = new Vector(best.getPosition(), block.getSize());
						vec.setAppendedBlock(block);
						vec.setMostEqualBlock(best);
						vec.setReference(best.getReference());
						vec.setSpanX(block.getPosition().x - best.getPosition().x);
						vec.setSpanY(block.getPosition().y - best.getPosition().y);
						vec.setAbsoluteDifferences(absoluteColorDifference);
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
			while (!executor.awaitTermination(20, TimeUnit.MICROSECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return vecs;
	}
	
	/**
	 * <p>The function evaluates the best guess among the array of "best matches"
	 * from every reference frame.</p>
	 * 
	 * @return The actual "best match" among all other "best matches"
	 * 
	 * @param canidates	Canidates to check
	 */
	private MacroBlock evaluateBestGuess(MacroBlock[] canidates) {
		MacroBlock best = canidates[0];
		
		for (MacroBlock b : canidates) {
			if (b == null) {
				continue;
			} else if (best == null) {
				best = b;
			}
			
			if (b.getMSE() < best.getMSE()) {
				best = b;
			}
		}
		
		return best;
	}
	
	/**
	 * <p>Computes the hexagon-search algorithm for a MacroBlock with one reference. The algorithm is as followed:<br>
	 * <ol>
	 * <li>Move a coordinate system to the Position of the block, so that the origin is a the blocks position.
	 * <li>Span a hexagon with the radius r and get all MSEs from the seven points. Look for the lowest.If the
	 * lowest can be found at the center move to step 4; else to step 3.
	 * <li>Set the new origin to the Point with the lowest MSE and repeat step 2.
	 * <li>Now set the radius to r /= 2 and repeat step 2, until r <= 1. If r <= 1 go to step 5.
	 * <li>Check the points around the "best guess" and get the one with the lowest MSE >> this is the best match.
	 * </ol>
	 * 
	 * @return Best match in the reference image
	 * 
	 * @param ref	Reference image
	 * @param blockToBeSearched	MacroBlock for which a match should be searched
	 */
	private MacroBlock computeHexagonSearch(PixelRaster ref, MacroBlock blockToBeSearched) {
		double lowestMSE = Double.MAX_VALUE;
		int radius = 4;
		int searchWindow = 48;
		int size = blockToBeSearched.getSize();
		int sumOfAllPoints = 2304; //All possible points to search
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
				double MSE = getMSEOfColors(cache, blockToBeSearched.getColors(), size, true);
				
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
			double MSE = getMSEOfColors(cache, blockToBeSearched.getColors(), size, true);
			
			if (MSE < lowestMSE) {
				lowestMSE = MSE;
				mostEqualBlock = new MacroBlock(p, size, cache);
			}
		}
		
		if (mostEqualBlock != null) {
			mostEqualBlock.setMSE(lowestMSE);
		}
		
		return mostEqualBlock;
	}
	
	/**
	 * <p>Computes the exhaustive search algorithm for a MacroBlock, the steps are as followed:<br>
	 * <ol>
	 * <li>Set set start to the origin -searchWindow for x and y.
	 * <li>Calculate the MSE at that position and increment x by 1. Repeat until all pixels are processed.
	 * <li>Find the MacroBlock with the lowest MSE >> Best match.
	 * </ol>
	 * 
	 * @return Best match in the reference image
	 * 
	 * @param ref	Reference image
	 * @param blockToBeSearched	MacroBlock for which a match should be searched
	 * @param bestMatchTillNow	Best matching MacroBlock from the previous hexagonal search
	 */
	private MacroBlock computeExhaustiveSearch(MacroBlock blockToSearch, MacroBlock bestMatchTillNow, PixelRaster ref) {
		if (bestMatchTillNow == null) {
			return null;
		}
		
		Dimension dim = ref.getDimension();
		MacroBlock mostEqualBlock = null;
		double lowestMSE = bestMatchTillNow.getMSE();
		Point pos = blockToSearch.getPosition();
		int searchWindow = 2;
		int size = blockToSearch.getSize();
		double[][][] cache = null;
		
		for (int y = pos.y - searchWindow; y < pos.y + searchWindow; y++) {
			if (y < 0 || y >= dim.height) {
				continue;
			}
			
			for (int x = pos.x - searchWindow; x < pos.x + searchWindow; x++) {
				if (x < 0 || x >= dim.width) {
					continue;
				}
				
				cache = ref.getPixelBlock(new Point(x, y), size, cache);
				double MSE = getMSEOfColors(blockToSearch.getColors(), cache, size, true);
				
				if (MSE < lowestMSE) {
					lowestMSE = MSE;
					mostEqualBlock = new MacroBlock(new Point(x, y), size, cache);
				}
			}
		}
		
		if (mostEqualBlock != null) {
			mostEqualBlock.setMSE(lowestMSE);
		}
		
		return mostEqualBlock == null ? bestMatchTillNow : mostEqualBlock;
	}
	
	/**
	 * <p>Get the six points of a hexagon and the center based
	 * on radius and position.</p>
	 * 
	 * @return Array of all points
	 * 
	 * @param radius	Radius of the hexagon
	 * @param pos	Position of the hexagon
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
	
	/**
	 * <p>Get the differences between the original and best matching block.</p>
	 * <p>Due to the nature of block matching there is no "100% fit", that's
	 * why the difference is stored as well. To get better compression ratios
	 * the difference only counts as a difference, if it exceeds a defined
	 * threshold.</p>
	 * 
	 * @return Array containing the differences.
	 * 
	 * @param col1	First color array
	 * @param col2	Second color array
	 * @param size	Size of the color arrays
	 */
	private double[][][] getAbsoluteDifferenceOfColors(double[][][] col1, double[][][] col2, int size) {
		int halfSize = size / 2;
		double[][] Y = new double[size][size];
		double[][] U = new double[halfSize][halfSize];
		double[][] V = new double[halfSize][halfSize];
		
		double YThreshold = 6.0, UVThreshold = 7.0;
		
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
	
	/**
	 * <p>Calculates the MSE (Mean Square Error) between two YUV color arrays.</p>
	 * <p>The MSE is slightly modified, so Y is punished more strictly than Chroma.
	 * If Alpha occures the MSE is extremely high, due to possible false encodings
	 * and by that should be used as equal as "No other exit".</p>
	 * <p>The color arrays should be ordered like this:
	 * <ul><li>[0] = Y
	 * <li>[1] = U
	 * <li>[2] = V
	 * <li>[3] = A
	 * </ul></p>
	 * 
	 * @return The Mean Square Error between the color arrays
	 * 
	 * @param col1	First color array to compare
	 * @param col2	Second color array to compare
	 * @param size	Size of the color arrays
	 * @param countAlpha	Flag whether the alpha is involved in the calculation or not
	 */
	private double getMSEOfColors(double[][][] col1, double[][][] col2, int size, boolean countAlpha) {
		double resY = 0;
		double resU = 0;
		double resV = 0;
		double resA = 0;
		int halfSize = size / 2;
		
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double deltaY = col1[0][x][y] - col2[0][x][y];
				resY += deltaY * deltaY;
				
				if (countAlpha) {
					double deltaA = col1[3][x][y] - col2[3][x][y];
					resA += deltaA * deltaA;
				}
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
	
	/**
	 * <p>Returns the total MSE of the "best matching" vectors.</p>
	 * @return Total MSE
	 */
	public double getVectorMSE() {
		return this.TOTAL_MSE;
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
	 * @see YAVC.Utils.Vector
	 * @see java.awt.Color
	 */
	public BufferedImage drawVectors(ArrayList<Vector> vecs, Dimension dim) {
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
}
