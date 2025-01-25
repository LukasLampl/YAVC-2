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


package app.interprediction;

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import java.util.concurrent.RecursiveAction;

import app.config;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.LoadDistributor;
import app.utils.MacroBlock;
import app.utils.PixelRaster;
import app.utils.ReferenceFrameManager;

/**
 * <p>The class {@code VectorPredictionTask} contains all functions
 * for the inter-prediction process in the YAVC video compressor.</p>
 * <p>First hexagonal search is executed followed by exhaustive search.</p>
 * 
 * <p><strong>Performance warning:</strong><br> Even though this process is
 * multithreaded, it might impact the overall performance due to increasing
 * amount of data on larger frames.</p>
 * 
 * @author Lukas Lampl
 * @since 1.4.3 [Optimized prototype]
 * @version 1.0 22 November 2024
 */
public class VectorPredictionTask extends RecursiveAction {
	private static final long serialVersionUID = 2694497873490175482L;
	
	/**
	 * The maximum work a task can run.
	 */
	private static final int MAX_WORK = 128 * 128;
	
	/**
	 * Holds the start index of the task.
	 */
	private int start = 0;
	
	/**
	 * Holds the end index of the task.
	 */
	private int end = 0;
	
	/**
	 * The vector manager in which to add the converted vectors.
	 */
	private LoadDistributor<Vector> vectorManager = null;
	
	/**
	 * A list that holds all MacroBlocks that should be converted to vectors.
	 */
	private List<MacroBlock> blocksToConvert = null;
	
	/**
	 * Holds all reference frames that the inter-prediction can use.
	 */
	private ReferenceFrameManager referenceManager = null;
	
	/**
	 * Initializes a {@code VectorPredictionTask}.
	 * 
	 * @param vectorManager		The vector manager in which to add the converted vectors.
	 * @param blocksToConvert	All MacroBlocks that should be converted.
	 * @param referenceManager	The reference frames that can be used during the prediction process.
	 * @param start				Start index of the task relative to the {@code blocksToConvert} list.
	 * @param end				End index of the task relative to the {@code blocksToConvert} list.
	 */
	public VectorPredictionTask(LoadDistributor<Vector> vectorManager, List<MacroBlock> blocksToConvert,
			ReferenceFrameManager referenceManager, final int start, final int end) {
		this.referenceManager = referenceManager;
		this.blocksToConvert = blocksToConvert;
		this.vectorManager = vectorManager;
		this.start = start;
		this.end = end;
	}
	
	@Override
	protected void compute() {
		int workload = getWorkloadOfThread();
		
		if (workload > MAX_WORK) {
			int middle = (this.start + this.end) / 2;
			VectorPredictionTask tl = new VectorPredictionTask(this.vectorManager, this.blocksToConvert, this.referenceManager, this.start, middle);
			VectorPredictionTask tr = new VectorPredictionTask(this.vectorManager, this.blocksToConvert, this.referenceManager, middle, this.end);
			invokeAll(tl, tr);
		} else {
			process();
		}
	}
	
	/**
	 * Calculates the workload of the current thread if it would be executed.
	 * 
	 * @return The workload in pixels.
	 */
	private int getWorkloadOfThread() {
		int load = 0;
		
		for (int i = this.start; i < this.end; i++) {
			load += this.blocksToConvert.get(i).getSquaredSize();
		}
		
		return load;
	}
	
	/**
	 * The main function which is responsible for converting a list part into vectors.
	 */
	private void process() {
		int maxSize = this.referenceManager.size();
		MacroBlock[] canidates = new MacroBlock[maxSize];
		Vector[] vecs = new Vector[this.end - this.start];
		int vectorIndex = 0;
		
		for (int i = this.start; i < this.end; i++) {
			try {
				MacroBlock block = this.blocksToConvert.get(i);
				int canidate = 0;
				
				for (int n = 0; n < maxSize && n <= config.MAX_REFERENCES; n++) {
					MacroBlock bestMatch = getBestMatchingMacroBlock(this.referenceManager.get(n), block, n);
					canidates[canidate++] = bestMatch;
				}
				
				MacroBlock best = evaluateBestGuess(canidates);
				Vector vec = constructMovementVector(this.referenceManager, best, block);
				vecs[vectorIndex++] = vec;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		for (Vector vec : vecs) {
			if (vec == null) {
				continue;
			}

			this.vectorManager.setObj(vec);
		}
	}
	
	/**
	 * <p>This function searches for the best match of a MacroBlock within a search window.</p>
	 * <p>First of all the hexagonal search pattern is used to get fast and precise results.
	 * After the hexagonal search the exhaustive search pattern is applied to find the absolute
	 * best match in a 4x4 search window.</p>
	 * 
	 * @return Best matching MacroBlock in the reference frame.
	 * 
	 * @param ref				Reference to search the best match in.
	 * @param blockToBeSearched	MaccroBlock that should be matched.
	 * @param referenceNumber	Number of the reference frame.
	 */
	private MacroBlock getBestMatchingMacroBlock(final PixelRaster ref, MacroBlock blockToBeSearched, final int referenceNumber) {
		double[][][] cache = ArrayUtils.get3DArray(blockToBeSearched.getSize(), true);
		MacroBlock bestMatch = computeHexagonSearch(ref, blockToBeSearched, cache);
		bestMatch = computeExhaustiveSearch(blockToBeSearched, bestMatch, ref, cache);
		
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
	 * @return Movement vector with all data to "reconstruct" the frame.
	 * 
	 * @param refs				Reference frames.
	 * @param bestMatch			Best matching MacroBlock.
	 * @param blockToBeSearched	MacroBlock that was searched at the beginning.
	 * 
	 * @see T.Vector
	 */
	private Vector constructMovementVector(final ReferenceFrameManager refs, MacroBlock bestMatch, MacroBlock blockToBeSearched) {
		Vector vec = null;
		
		if (bestMatch != null) {
			int size = blockToBeSearched.getSize();
			blockToBeSearched.setConvertedToVector(true);
			//Re-init reference color since the cache is modified every loop.
			PixelRaster referenceFrame = refs.getByReference(bestMatch.getReference());
			double[][][] col = referenceFrame.getPixelBlock(bestMatch.getPosition(), size, null);
			double[][][] absoluteColorDifference = getAbsoluteDifferenceOfColors(blockToBeSearched.getColors(), col, size);
			
			vec = new Vector(bestMatch.getPositionX(), bestMatch.getPositionY(), size);
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
	 * @return The actual "best match" among all other "best matches".
	 * 
	 * @param canidates	Canidates to check.
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
	 * @return Best match in the reference image.
	 * 
	 * @param ref				Reference image.
	 * @param blockToBeSearched	MacroBlock for which a match should be searched.
	 * @param cache				Cache for color values.
	 */
	private MacroBlock computeHexagonSearch(PixelRaster ref, MacroBlock blockToBeSearched, double[][][] cache) {
		double lowestMSE = Double.MAX_VALUE;
		int radius = 4;
		int searchWindow = 48;
		int size = blockToBeSearched.getSize();
		Dimension dim = ref.getDimension();

		Point blockPos = blockToBeSearched.getPosition();
		Point centerPoint = blockToBeSearched.getPosition();
		MacroBlock mostEqualBlock = new MacroBlock(centerPoint.x, centerPoint.y, size, false);
		
		Point initPos = new Point(0, 0);
		Point[] searchPoints = new Point[7];
		
		while (radius > 1) {
			getHexagonPoints(radius, centerPoint, searchPoints);
			
			for (Point p : searchPoints) {
				if (!isHexagonPointInSearchWindow(blockPos, searchWindow, p, dim)
					|| !isPointInFrame(p, dim)) {
					continue;
				}
				
				cache = ref.getPixelBlock(p, size, cache);
				double MSE = getMSEOfColors(cache, blockToBeSearched.getColors(), size);
				
				if (MSE < lowestMSE) {
					lowestMSE = MSE;
					initPos.setLocation(p.x, p.y);
					mostEqualBlock.setColorComponents(cache);
					mostEqualBlock.moveBlock(p.x, p.y);
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
			if (!isHexagonPointInSearchWindow(blockPos, searchWindow, p, dim)
				|| !isPointInFrame(p, dim)) {
				continue;
			}
			
			cache = ref.getPixelBlock(p, size, cache);
			double MSE = getMSEOfColors(cache, blockToBeSearched.getColors(), size);
			
			if (MSE < lowestMSE) {
				lowestMSE = MSE;
				mostEqualBlock.setColorComponents(cache);
				mostEqualBlock.moveBlock(p.x, p.y);
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
	 * @return Array of points.
	 * 
	 * @param center	Center of the small hexagon.
	 * @param radius	Radius of the hexagon.
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
	 * @param blockPos		Position of the MacroBlock (start position).
	 * @param searchWindow	Search window.
	 * @param edgeOfHexagon	The point to check.
	 * @param dim			Dimension of the frame.
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
	 * Checks if a given Point is in a given Dimension.
	 * 
	 * @param pos	Position of the Point.
	 * @param dim	The Dimension in which the point is estimated to be in.
	 * @return
	 * <ul>
	 * <li>{@code true} - When the Point is in the Dimension.
	 * <li>{@code false} - When the Point is not in the Dimension.
	 * </ul>
	 */
	private boolean isPointInFrame(final Point pos, final Dimension dim) {
		if (pos.x >= dim.width || pos.y >= dim.height
			|| pos.x < 0 || pos.y < 0) {
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
	 * @return Best match in the reference image.
	 * 
	 * @param ref				Reference image.
	 * @param blockToBeSearched	MacroBlock for which a match should be searched.
	 * @param bestMatchTillNow	Best matching MacroBlock from the previous hexagonal search.
	 * @param cache				Cache for storing color values.
	 */
	private MacroBlock computeExhaustiveSearch(MacroBlock blockToSearch, MacroBlock bestMatchTillNow, PixelRaster ref, double[][][] cache) {
		if (bestMatchTillNow == null) {
			return null;
		}
		
		int searchWindow = 2;
		int size = blockToSearch.getSize();
		double lowestMSE = bestMatchTillNow.getMSE();
		Dimension dim = ref.getDimension();
		Point pos = blockToSearch.getPosition();
		MacroBlock mostEqualBlock = bestMatchTillNow;
		final int startY = pos.y - searchWindow;
		final int endY = pos.y + searchWindow;
		final int startX = pos.x - searchWindow;
		final int endX = pos.x + searchWindow;
		
		for (int y = startY; y < endY; y++) {
			if (y < 0 || y >= dim.height) {
				continue;
			}
			
			for (int x = startX; x < endX; x++) {
				if (x < 0 || x >= dim.width) {
					continue;
				}
				
				cache = ref.getPixelBlock(x, y, size, cache);
				double MSE = getMSEOfColors(blockToSearch.getColors(), cache, size);
				
				if (MSE < lowestMSE) {
					lowestMSE = MSE;
					mostEqualBlock.moveBlock(x, y);
					mostEqualBlock.setColorComponents(cache);
				}
			}
		}
		
		if (mostEqualBlock != null) {
			mostEqualBlock.setMSE(lowestMSE);
		}
		
		return mostEqualBlock;
	}
	
	/**
	 * <p>Get the six points of a hexagon and the center based
	 * on radius and position.</p>
	 * 
	 * <p><b>Note:</b><br>
	 * Everything is written into the passed cache!
	 * </p>
	 * 
	 * @param radius	Radius of the hexagon.
	 * @param pos		Position of the hexagon.
	 * @param cache		Cache of previously stored points.
	 */
	private void getHexagonPoints(final int radius, final Point pos, Point[] cache) {
		if (cache == null) {
			cache = new Point[7];
		}
		
		cache[6] = pos;
		
		for (int i = 0; i < 6; i++) {
			double cos = VectorEngine.COS_TABLE_HEXAGON[i];
			double sin = VectorEngine.SIN_TABLE_HEXAGON[i];
			cache[i] = new Point((int)(cos * radius + pos.x), (int)(sin * radius + pos.y));
		}
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
	 * @param col1	First color array.
	 * @param col2	Second color array.
	 * @param size	Size of the color arrays.
	 */
	private double[][][] getAbsoluteDifferenceOfColors(double[][][] col1, double[][][] col2, int size) {
		int halfSize = size / 2;
		double[][] Y = new double[size][size];
		double[][] U = new double[halfSize][halfSize];
		double[][] V = new double[halfSize][halfSize];
		
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double diff = col1[ColorManager.Y_INDEX][x][y] - col2[ColorManager.Y_INDEX][x][y];
				Y[x][y] = diff;
			}
		}
		
		for (int y = 0; y < halfSize; y++) {
			for (int x = 0; x < halfSize; x++) {
				double diffU = col1[ColorManager.U_INDEX][x][y] - col2[ColorManager.U_INDEX][x][y];
				double diffV = col1[ColorManager.V_INDEX][x][y] - col2[ColorManager.V_INDEX][x][y];
				U[x][y] = diffU;
				V[x][y] = diffV;
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
	 * </ul></p>
	 * 
	 * @return The Mean Square Error between the color arrays.
	 * 
	 * @param col1	First color array to compare.
	 * @param col2	Second color array to compare.
	 * @param size	Size of the color arrays.
	 */
	private double getMSEOfColors(double[][][] col1, double[][][] col2, int size) {
		double[][] Y1 = col1[ColorManager.Y_INDEX];
		double[][] Y2 = col2[ColorManager.Y_INDEX];
		double[][] U1 = col1[ColorManager.U_INDEX];
		double[][] U2 = col2[ColorManager.U_INDEX];
		double[][] V1 = col1[ColorManager.V_INDEX];
		double[][] V2 = col2[ColorManager.V_INDEX];
		
		
		double resY = 0;
		double resU = 0;
		double resV = 0;
		int halfSize = size / 2;
		
		for (int x = 0; x < size; x++) {
			double[] lY1 = Y1[x];
			double[] lY2 = Y2[x];
			
			for (int y = 0; y < size; y++) {
				double deltaY = lY1[y] - lY2[y];
				resY += deltaY * deltaY;
			}
		}
		
		for (int x = 0; x < halfSize; x++) {
			double[] lU1 = U1[x];
			double[] lU2 = U2[x];
			double[] lV1 = V1[x];
			double[] lV2 = V2[x];
			
			for (int y = 0; y < halfSize; y++) {
				double deltaU = lU1[y] - lU2[y];
				double deltaV = lV1[y] - lV2[y];
				resU += deltaU * deltaU;
				resV += deltaV * deltaV;
			}
		}
		
		double sizeSQ = size * size;
		double halfSizeSQ = halfSize * halfSize;
		
		resY /= sizeSQ;
		resU /= halfSizeSQ;
		resV /= halfSizeSQ;
		return ((resY + resU + resV) / 3);
	}
}
