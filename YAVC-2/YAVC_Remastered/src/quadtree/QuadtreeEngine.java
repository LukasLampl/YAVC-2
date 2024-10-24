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

package quadtree;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import utils.MacroBlock;
import utils.MeanStructure;
import utils.PixelRaster;

/**
 * <p>
 * The class {@code YAVC.Encoder.QuadtreeEngine} contains basic functions 
 * for construction a quadtree based on mean color. The maximum size
 * is 128x128, which gets splitted down by this order:
 * 128x128 -> 64x64 -> 32x32 -> 16x16 -> 8x8 -> 4x4
 * The splitting itself happens in the blocks, the QuadtreeEngine is
 * just functioning as an entry point.
 * </p>
 * 
 * <p><strong>Performance warning:</strong><br>
 * The Quadtree construction involves
 * getting the RGB value off of every pixel, which might impact
 * performance at larger PixelRasters.<br>
 * Time: O(n)
 * </p>
 * 
 * @see utils.MacroBlock
 * @see utils.PixelRaster
 * 
 * @author Lukas Lampl
 * @since 17.0
 * @version 1.0 29 May 2024
 */

public class QuadtreeEngine {
	private final int MAX_SIZE = 128;
	
	/**
	 * Entry point of the quadtree construction.
	 * The image is split into 128x128 blocks, that are processed
	 * in an individual subdividing process in the block itself.
	 * 
	 * First the roots are searched and initialized using
	 * {@link utils.PixelRaster#getPixelBlock(Point, int, double[][][])}.
	 * After that the block is imaginary split into 4x4 blocks, of which the mean
	 * color is acquired, while also getting the RGB information of the
	 * whole block. Now the standardDeviation is used to determine,
	 * whether the block is already good enough without splitting, while
	 * preserving quality. If not or the size is to big, the block gets
	 * split. For further details on the splitting section
	 * 
	 * @see utils.MacroBlock#subdivide(double, int, int[][], int[][][], Dimension, Point)
	 * 
	 * @return ArrayList<MacroBlock> => All QuadtreeRoots
	 * 
	 * @param PixelRaster currentFrame => PixelRaster to "convert" to
	 * Quadtree
	 * 
	 * @throws NullPointerException, when the passed frame is null
	 */
	public ArrayList<MacroBlock> constructQuadtree(PixelRaster currentFrame) {
		if (currentFrame == null) {
			throw new NullPointerException("PixelRaster \"currentFrame\" == NULL!");
		}
		
		ArrayList<MacroBlock> roots = new ArrayList<MacroBlock>();
		
		try {
			final int errorThreshold = 45;
			int currentOrderNumber = 0;
			int width = currentFrame.getWidth();
			int height = currentFrame.getHeight();
			int threads = Runtime.getRuntime().availableProcessors();
			
			ArrayList<Future<MacroBlock>> futureRoots = new ArrayList<Future<MacroBlock>>();
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			
			for (int x = 0; x < width; x += this.MAX_SIZE) {
				for (int y = 0; y < height; y += this.MAX_SIZE) {
					final int currentOrder = currentOrderNumber++;
					Callable<MacroBlock> task = createQuadtreeConstructionTask(new Point(x, y), currentFrame, errorThreshold, currentOrder);
					futureRoots.add(executor.submit(task));
				}
			}
			
			for (Future<MacroBlock> root : futureRoots) {
				try {
					MacroBlock block = root.get();
					
					if (block != null) {
						roots.add(block);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			executor.shutdown();
			while (!executor.awaitTermination(10, TimeUnit.MICROSECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
			
		return roots;
	}
	
	/**
	 * <p>Creates a subdivision task for a single root.</p>
	 * 
	 * @return Runnable task for subdividing a root
	 * 
	 * @param pos	Position of the root
	 * @param frame	Current frame
	 * @param errorThreshold	Maximum error before subdivision
	 * @param currentOrder	Order of the root
	 */
	private Callable<MacroBlock> createQuadtreeConstructionTask(final Point pos, final PixelRaster frame, final int errorThreshold, final int currentOrder) {
		Callable<MacroBlock> task = () -> {
			MacroBlock origin = new MacroBlock(new Point(pos.x, pos.y), this.MAX_SIZE);
			double[][][] comps = frame.getPixelBlock(new Point(pos.x, pos.y), this.MAX_SIZE, null);
			origin.setColorComponents(comps[0], comps[1], comps[2], comps[3]);
			origin.setOrder(currentOrder);
			
			MeanStructure meanOf4x4BlocksInBlock = origin.calculate4x4Means();
			int[] curMean = origin.calculateMeanOfCurrentBlock(meanOf4x4BlocksInBlock.get4x4Means(), new Point(0, 0), this.MAX_SIZE);
			double originStdDeviation = origin.computeStandardDeviation(curMean, meanOf4x4BlocksInBlock.getArgbs(), new Point(0, 0), this.MAX_SIZE);
			origin.setMeanColor(curMean);
			
			if (originStdDeviation > errorThreshold
				|| pos.x + this.MAX_SIZE > frame.getWidth()
				|| pos.y + this.MAX_SIZE > frame.getHeight()) {
				origin.subdivide(errorThreshold, 0, meanOf4x4BlocksInBlock.get4x4Means(), meanOf4x4BlocksInBlock.getArgbs(), frame.getDimension(), new Point(0, 0));
			}
			
			return origin;
		};
		
		return task;
	}
	
	/**
	 * Get all leave nodes of the quadtree roots.
	 * The leaves are recursively searched.
	 * @see YAVC.Encoder.QuadtreeEngine.getLeaves()
	 * 
	 * @return ArrayList<MacroBlock> => All leaf nodes
	 * 
	 * @param ArrayList<MacroBlock> roots => Roots to get the leaves from
	 * 
	 * @throws NullPointerException, when no root is provided
	 */
	public ArrayList<MacroBlock> getLeaveNodes(ArrayList<MacroBlock> roots) {
		if (roots == null) {
			throw new NullPointerException("No QuadtreeRoots to process");
		}
		
		ArrayList<MacroBlock> leaveNodes = new ArrayList<MacroBlock>();
		ArrayList<Future<ArrayList<MacroBlock>>> futureLeavesList = new ArrayList<Future<ArrayList<MacroBlock>>>();
		int threads = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threads);

		for (MacroBlock root : roots) {
			Callable<ArrayList<MacroBlock>> task = () -> {
				return getLeaves(root);
			};
			
			futureLeavesList.add(executor.submit(task));
		}
		
		for (Future<ArrayList<MacroBlock>> flist : futureLeavesList) {
			try {
				ArrayList<MacroBlock> nodes = flist.get();
				if (nodes != null) leaveNodes.addAll(nodes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		executor.shutdown();
		return leaveNodes;
	}
	
	/**
	 * Get the leaves of the current block, till
	 * the blocks are the leaves of the quadtree itself.
	 * 
	 * @return ArrayList<MacroBlock> => Leave nodes
	 * 
	 * @param MacroBlock block => Block to go down recursively
	 */
	private ArrayList<MacroBlock> getLeaves(MacroBlock block) {
		if (block == null) {
			return null;
		}
		
		ArrayList<MacroBlock> blocks = new ArrayList<MacroBlock>(4);
		
		if (block.isSubdivided()) {
			for (MacroBlock node : block.getNodes()) {
				if (node == null) {
					continue;
				}
				
				blocks.addAll(getLeaves(node));
			}
		} else {
			blocks.add(block);
		}
		
		return blocks;
	}
}
