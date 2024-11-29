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

package app.quadtree;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import app.rendering.ColorManager;
import app.utils.LoadDistributor;
import app.utils.MacroBlock;
import app.utils.PixelRaster;

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
 * @see app.utils.MacroBlock
 * @see app.utils.PixelRaster
 * 
 * @author Lukas Lampl
 * @since 1.0
 */

public class QuadtreeEngine {
	/**
	 * The start size and maximum size of a Quadtree MacroBlock.
	 */
	private final int MAX_SIZE = 128;
	
	/**
	 * Total amount of sizes.
	 * 4x4, 8x8, 16x16, 32x32, 64x64 and 128x128
	 */
	public static final int NUMBER_OF_SIZES = 6;
	
	/**
	 * Index at which to expect 4x4 blocks.
	 */
	public static final int INDEX_4x4 = 0;
	
	/**
	 * Index at which to expect 8x8 blocks.
	 */
	public static final int INDEX_8x8 = 1;
	
	/**
	 * Index at which to expect 16x16 blocks.
	 */
	public static final int INDEX_16x16 = 2;
	
	/**
	 * Index at which to expect 32x32 blocks.
	 */
	public static final int INDEX_32x32 = 3;
	
	/**
	 * Index at which to expect 64x64 blocks.
	 */
	public static final int INDEX_64x64 = 4;
	
	/**
	 * Index at which to expect 128x128 blocks.
	 */
	public static final int INDEX_128x128 = 5;
	
	/**
	 * Entry point of the quadtree construction.
	 * The image is split into 128x128 blocks, that are processed
	 * in an individual subdividing process in the block itself.
	 * 
	 * First the roots are searched and initialized using
	 * {@link app.utils.PixelRaster#getPixelBlock(Point, int, double[][][])}.
	 * After that the block is imaginary split into 4x4 blocks, of which the mean
	 * color is acquired, while also getting the RGB information of the
	 * whole block. Now the standardDeviation is used to determine,
	 * whether the block is already good enough without splitting, while
	 * preserving quality. If not or the size is to big, the block gets
	 * split. For further details on the splitting section
	 * 
	 * @see app.utils.MacroBlock#subdivide(Dimension)
	 * 
	 * @return All QuadtreeRoots
	 * 
	 * @param currentFrame PixelRaster to "convert" to Quadtree.
	 * 
	 * @throws NullPointerException	When the passed frame is {@code null}.
	 */
	public ArrayList<MacroBlock> constructQuadtree(PixelRaster currentFrame) {
		if (currentFrame == null) {
			throw new NullPointerException("PixelRaster \"currentFrame\" == NULL!");
		}
		
		ArrayList<MacroBlock> roots = new ArrayList<MacroBlock>();
		
		try {
			final int errorThreshold = 45;
			int width = currentFrame.getWidth();
			int height = currentFrame.getHeight();
			int threads = Runtime.getRuntime().availableProcessors();
			
			ArrayList<Future<MacroBlock>> futureRoots = new ArrayList<Future<MacroBlock>>();
			ExecutorService executor = Executors.newFixedThreadPool(threads);
			
			for (int x = 0; x < width; x += this.MAX_SIZE) {
				for (int y = 0; y < height; y += this.MAX_SIZE) {
					Callable<MacroBlock> task = createQuadtreeConstructionTask(new Point(x, y), currentFrame, errorThreshold);
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
			while (!executor.awaitTermination(1, TimeUnit.MILLISECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
			
		return roots;
	}
	
	/**
	 * <p>Creates a subdivision task for a single root.</p>
	 * 
	 * @return Runnable task for subdividing a root.
	 * 
	 * @param pos				Position of the root.
	 * @param frame				Current frame.
	 * @param errorThreshold	Maximum error before subdivision.
	 */
	private Callable<MacroBlock> createQuadtreeConstructionTask(final Point pos, final PixelRaster frame, final int errorThreshold) {
		Callable<MacroBlock> task = () -> {
			MacroBlock origin = new MacroBlock(pos.x, pos.y, this.MAX_SIZE, false);
			double[][][] comps = frame.getPixelBlock(new Point(pos.x, pos.y), origin.getSize(), null);
			origin.setColorComponents(comps[ColorManager.Y_INDEX], comps[ColorManager.U_INDEX], comps[ColorManager.V_INDEX]);
			
			QuadtreeTask treeTask = new QuadtreeTask();
			treeTask.splitOriginBlock(origin, frame.getDimension(), errorThreshold);
			return origin;
		};
		
		return task;
	}
	
	/**
	 * Get all leave nodes of the quadtree roots.
	 * The leaves are recursively searched.
	 * 
	 * @return All leaf nodes
	 * 
	 * @param roots	Roots from which to get the leaves from.
	 * @see #getLeaves(MacroBlock)
	 * 
	 * @throws NullPointerException	When no root is provided.
	 */
	public LoadDistributor<MacroBlock> getLeaveNodes(ArrayList<MacroBlock> roots) {
		if (roots == null) {
			throw new NullPointerException("No QuadtreeRoots to process.");
		}
		
		LoadDistributor<MacroBlock> loadManager = new LoadDistributor<MacroBlock>();
		
		try {
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
					
					if (nodes == null) {
						continue;
					}
					
					for (MacroBlock block : nodes) {
						loadManager.setObj(block);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			executor.shutdown();
			while (!executor.awaitTermination(1, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return loadManager;
	}
	
	/**
	 * Get the index in an array with all MacroBlock sizes represented based
	 * on the given size.
	 * 
	 * @param size	The size to convert to an index.
	 * @return The index.
	 */
	public static int getIndexBySize(int size) {
		switch (size) {
		case 128:
			return INDEX_128x128;
		case 64:
			return INDEX_64x64;
		case 32:
			return INDEX_32x32;
		case 16:
			return INDEX_16x16;
		case 8:
			return INDEX_8x8;
		case 4:
			return INDEX_4x4;
		default:
			throw new IllegalArgumentException("The size " + size + " is currently no supported.");
		}
	}

	/**
	 * Get the leaves of the current block, till
	 * the blocks are the leaves of the quadtree itself.
	 * 
	 * @return The leave nodes.
	 * 
	 * @param block	Block to go down recursively.
	 */
	private ArrayList<MacroBlock> getLeaves(MacroBlock block) {
		if (block == null) {
			return null;
		}
		
		ArrayList<MacroBlock> blocks = new ArrayList<MacroBlock>(4);
		
		if (block.isSubdivided()) {
			if (block.getNodes() == null) {
				return blocks;
			}
			
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
