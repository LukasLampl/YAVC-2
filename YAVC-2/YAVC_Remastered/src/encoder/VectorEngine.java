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

package encoder;

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

import app.config;
import utils.MacroBlock;
import utils.PixelRaster;
import utils.Vector;

/**
 * <p>The class {@code VectorEngine} contains all functions
 * for the inter-prediction process in the YAVC video compressor.</p>
 * <p>First hexagonal search is executed followed by exhaustive search.</p>
 * 
 * <p><strong>Performance warning:</strong><br> Even though this process is
 * multithreaded, it might impact the overall performance due to increasing
 * amount of data on larger frames.</p>
 * 
 * @author Lukas Lampl
 * @since 17.0
 * @version 1.0 29 May 2024
 */

public class VectorEngine {
	
	/**
	 * <p>Variable to store the PI radian.</p>
	 */
	private double PI_RAD = Math.PI / 3;
	
	private double[] COS_TABLE_HEXAGON = new double[6];
	private double[] SIN_TABLE_HEXAGON = new double[6];
	
	/**
	 * <p>Variable to store the total MSE of all "best matches".</p>
	 */
	private double TOTAL_MSE = 0;
	
	public VectorEngine() {
		initHexagonValues();
	}
	
	private void initHexagonValues() {
		for (int i = 0; i < 6; i++) {
			double rad = this.PI_RAD * (i + 1);
			this.COS_TABLE_HEXAGON[i] = Math.cos(rad);
			this.SIN_TABLE_HEXAGON[i] = Math.sin(rad);
		}
	}
	
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
	 * @see utils.Vector
	 */
	public ArrayList<Vector> computeMovementVectors(final ArrayList<MacroBlock> blocksToInterpredict, final ArrayList<PixelRaster> refs) {
		if (blocksToInterpredict == null || blocksToInterpredict.size() == 0) {
			throw new NullPointerException("No blocks to inter-predict");
		} else if (refs == null || refs.size() == 0) {
			throw new NullPointerException("No reference frame to refere to");
		}
		
		this.TOTAL_MSE = 0;
		
		ArrayList<Vector> vecs = new ArrayList<Vector>(blocksToInterpredict.size());
		ArrayList<Future<Vector>> futureVecs = new ArrayList<Future<Vector>>(blocksToInterpredict.size());
		
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		
		for (MacroBlock block : blocksToInterpredict) {
			Callable<Vector> searchTask = createVectorSearchTask(refs, block);
			futureVecs.add(executor.submit(searchTask));
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
		
		try {
			while (!executor.awaitTermination(20, TimeUnit.MICROSECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return vecs;
	}
	
	/**
	 * <p>Creates a task for searching a MacroBlock in the provided references.</p>
	 * <p>The task first gets the best matching MacroBlock using {@link #getBestMatchingMacroBlock(PixelRaster, MacroBlock, int)}
	 * and finally evaluates the results with {@link #evaluateBestGuess(MacroBlock[])}.</p>
	 * 
	 * @return Executable task for searching a block in all provided references
	 * 
	 * @param refs	Reference frames
	 * @param blockToBeSearched	MacroBlock that should be searched
	 */
	private Callable<Vector> createVectorSearchTask(final ArrayList<PixelRaster> refs, MacroBlock blockToBeSearched) {
		Callable<Vector> task = () -> {
			int maxSize = refs.size();
			MacroBlock[] canidates = new MacroBlock[maxSize];
			
			for (int i = 0, index = 0; i < maxSize && i <= config.MAX_REFERENCES; i++, index++) {
				MacroBlock bestMatch = getBestMatchingMacroBlock(refs.get(i), blockToBeSearched, i);
				canidates[index] = bestMatch;
			}
			
			MacroBlock best = evaluateBestGuess(canidates);
			Vector vec = constructMovementVector(refs, best, blockToBeSearched);
			
			return vec;
		};
		
		return task;
	}
	
	/**
	 * <p>This function searches for the best match of a MacroBlock within a search window.</p>
	 * <p>First of all the hexagonal search pattern is used to get fast and precise results.
	 * After the hexagonal search the exhaustive search pattern is applied to find the absolute
	 * best match in a 4x4 search window.</p>
	 * 
	 * @return Best matching MacroBlock in the reference frame
	 * 
	 * @param ref	Reference to search the best match in
	 * @param blockToBeSearched	MaccroBlock that should be matched
	 * @param referenceNumber	Number of the reference frame
	 */
	private MacroBlock getBestMatchingMacroBlock(final PixelRaster ref, MacroBlock blockToBeSearched, final int referenceNumber) {
		MacroBlock bestMatch = computeHexagonSearch(ref, blockToBeSearched);
		bestMatch = computeExhaustiveSearch(blockToBeSearched, bestMatch, ref);
		
		if (bestMatch != null) {
			bestMatch.setReference(config.MAX_REFERENCES - referenceNumber);
		}
		
		return bestMatch;
	}
	
	/**
	 * <p>Here the actual vector itself is created using all previously evaluated data.</p>
	 * <p>First the absolute color difference is calculated and set, then the vector is
	 * filled with other important data, like position, size, reference, etc.</p>
	 * 
	 * @return Movement vector with all data to "reconstruct" the frame
	 * 
	 * @param refs	Reference frames
	 * @param bestMatch	Best matching MacroBlock
	 * @param blockToBeSearched	MacroBlock that was searched at the beginning
	 * 
	 * @see utils.Vector
	 */
	private Vector constructMovementVector(final ArrayList<PixelRaster> refs, MacroBlock bestMatch, MacroBlock blockToBeSearched) {
		Vector vec = null;
		
		if (bestMatch != null) {
			int size = blockToBeSearched.getSize();
			
			PixelRaster referenceRaster = refs.get(config.MAX_REFERENCES - bestMatch.getReference());
			double[][][] referenceColor = referenceRaster.getPixelBlock(bestMatch.getPosition(), size, null);
			double[][][] absoluteColorDifference = getAbsoluteDifferenceOfColors(blockToBeSearched.getColors(), referenceColor, size);
			double newMatchMSE = getMSEOfColors(absoluteColorDifference, blockToBeSearched.getColors(), size, false);
			this.TOTAL_MSE += newMatchMSE;
			
			vec = new Vector(bestMatch.getPosition(), size);
			vec.setAppendedBlock(blockToBeSearched);
			vec.setMostEqualBlock(bestMatch);
			vec.setReference(bestMatch.getReference());
			vec.setSpanX(blockToBeSearched.getPosition().x - bestMatch.getPosition().x);
			vec.setSpanY(blockToBeSearched.getPosition().y - bestMatch.getPosition().y);
			vec.setAbsoluteDifferences(absoluteColorDifference);
		}
		
		return vec;
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
		Dimension dim = ref.getDimension();
		HashSet<Point> searchedPoints = new HashSet<Point>(sumOfAllPoints);
		
		Point blockPos = blockToBeSearched.getPosition();
		Point centerPoint = blockToBeSearched.getPosition();
		MacroBlock mostEqualBlock = null;
		
		Point initPos = new Point(0, 0);
		Point[] searchPoints = new Point[7];
		double[][][] cache = null;
		
		while (radius > 1) {
			searchPoints = getHexagonPoints(radius, centerPoint);
			
			for (Point p : searchPoints) {
				if (searchedPoints.contains(p)
					|| !isHexagonPointInSearchWindow(blockPos, searchWindow, p, dim)) {
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
		
		searchPoints = getSmallHexagonSearchPoints(centerPoint, radius);
		
		for (Point p : searchPoints) {
			if (searchedPoints.contains(p)
				|| !isHexagonPointInSearchWindow(blockPos, searchWindow, p, dim)) {
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
	 * <p>Get the edge points of the smallest possible hexagon.</p>
	 * <p>Basically this gets all points around the center position
	 * and the center itself.</p>
	 * 
	 * @return Array of points
	 * 
	 * @param center	Center of the small hexagon
	 * @param radius	Radius of the hexagon
	 */
	private Point[] getSmallHexagonSearchPoints(Point center, int radius) {
		Point[] searchPoints = new Point[9];
		searchPoints[0] = center;
		searchPoints[1] = new Point(center.x + radius, center.y);
		searchPoints[2] = new Point(center.x - radius, center.y);
		searchPoints[3] = new Point(center.x, center.y + radius);
		searchPoints[4] = new Point(center.x, center.y - radius);
		searchPoints[5] = new Point(center.x + radius, center.y + radius);
		searchPoints[6] = new Point(center.x - radius, center.y - radius);
		searchPoints[7] = new Point(center.x - radius, center.y + radius);
		searchPoints[8] = new Point(center.x + radius, center.y - radius);
		
		return searchPoints;
	}
	
	/**
	 * <p>Checks if an edge point of a hexagon is within the boundaries or not.</p>
	 * 
	 * @return Flag if the point is in boundary or not.
	 * <ul><li>true = Point is in boundary
	 * <li>false = Point is out of boundary
	 * </ul>
	 * 
	 * @param blockPos	Position of the MacroBlock (start position)
	 * @param searchWindow	Search window
	 * @param edgeOfHexagon	The point to check
	 * @param dim	Dimension of the frame
	 */
	private boolean isHexagonPointInSearchWindow(final Point blockPos, int searchWindow, final Point edgeOfHexagon, final Dimension dim) {
		if ((edgeOfHexagon.x > blockPos.x + searchWindow)
			|| (edgeOfHexagon.x < blockPos.x - searchWindow)
			|| (edgeOfHexagon.y > blockPos.y + searchWindow)
			|| (edgeOfHexagon.y < blockPos.y - searchWindow)
			|| (edgeOfHexagon.x > dim.width)
			|| (edgeOfHexagon.x < 0)
			|| (edgeOfHexagon.y > dim.height)
			|| (edgeOfHexagon.y < 0)) {
			return false;
		}
		
		return true;
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
		
		int searchWindow = 2;
		int size = blockToSearch.getSize();
		double lowestMSE = bestMatchTillNow.getMSE();
		Dimension dim = ref.getDimension();
		MacroBlock mostEqualBlock = null;
		Point pos = blockToSearch.getPosition();
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
		
		for (int i = 0; i < 6; i++) {
			double cos = this.COS_TABLE_HEXAGON[i];
			double sin = this.SIN_TABLE_HEXAGON[i];
			points[i] = new Point((int)(cos * radius + pos.x), (int)(sin * radius + pos.y));
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
		
		double YThreshold = 1.0, UVThreshold = 2.0;
		
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
	 * If Alpha occurs the MSE is extremely high, due to possible false encodings
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
		
		double sizeSQ = size * size;
		double halfSizeSQ = halfSize * halfSize;
		
		resY = (resY / sizeSQ) * 4;
		resU /= halfSizeSQ;
		resV /= halfSizeSQ;
		
		if (countAlpha) {
			resA = Math.pow(resA, resA);
			return ((resY + resU + resV + resA) / 4);
		}
		
		return ((resY + resU + resV) / 3);
	}
	
	/**
	 * <p>Returns the total MSE of the "best matching" vectors.</p>
	 * @return Total MSE
	 */
	public double getVectorMSE() {
		return this.TOTAL_MSE;
	}
}
