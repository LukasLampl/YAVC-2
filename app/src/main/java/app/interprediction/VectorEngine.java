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

package app.interprediction;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import app.utils.LoadDistributor;
import app.utils.MacroBlock;
import app.utils.ReferenceFrameManager;

/**
 * <p>The class {@code VectorEngine} is the main distributor class
 * for calculation/predicting motion in a frame using the
 * {@link app.interprediction.VectorPredictionTask VectorPredictionTask}.
 * </p>
 * 
 * <p><strong>Performance warning:</strong><br> Even though this process is
 * multithreaded, it might impact the overall performance due to increasing
 * amount of data on larger frames.</p>
 * 
 * @author Lukas Lampl
 * @since 1.0.0
 * @version 1.0 11 November 2024
 */

public class VectorEngine {
	/**
	 * <p>Variable to store the PI radian.</p>
	 */
	public static double PI_RAD = Math.PI / 3;
	
	/**
	 * <p>Holds a temporary table with all cosines for constructing a hexagon.</p>
	 */
	public static double[] COS_TABLE_HEXAGON = new double[6];
	
	/**
	 * <p>Holds a temporary table with all sines for constructing a hexagon.</p>
	 */
	public static double[] SIN_TABLE_HEXAGON = new double[6];
	
	/**
	 * Counter on how many pixels were processed.
	 */
	private int totalPixelsProcessed = 0;
	
	/**
	 * <p>Variable to store the total MSE of all "best matches".</p>
	 */
	private double TOTAL_MSE = 0;
	
	public VectorEngine() {
		initHexagonValues();
	}
	
	/**
	 * <p>Calculates all possible movement vectors from the current frame to
	 * an list of reference frames.</p>
	 * <p>Due to the nature of block-matching there is no "100% perfect fit" block,
	 * but in order to restore most of the information as possible without affecting
	 * the overall compression ratio, the differences are stored too.</p>
	 * 
	 * @return An VectorEgineResult with the managers.
	 * 
	 * @param differences	MacroBlocks to search a match for
	 * @param refs			Reference frames that are allowed to use during the search
	 *
	 * @throws NullPointerException	When no MacroBlocks are passed for prediction or
	 * if no references to refer to are available
	 * 
	 * @see T.Vector
	 */
	public VectorEngineResult computeMovementVectors(List<MacroBlock> differences, final ReferenceFrameManager refs) {
		if (refs == null || refs.size() == 0) {
			throw new NullPointerException("No reference frame to refer to");
		}
		
		this.totalPixelsProcessed = 0;
		this.TOTAL_MSE = 0;
		
		LoadDistributor<MacroBlock> restBlockManager = new LoadDistributor<MacroBlock>();
		LoadDistributor<Vector> vecManager = new LoadDistributor<Vector>();
		ForkJoinPool executor = ForkJoinPool.commonPool();
		executor.invoke(new VectorPredictionTask(vecManager, differences, refs, 0, differences.size()));
		executor.shutdown();
		executor.close();
		
		if (vecManager.getNumberOfObjects() != differences.size()) {
			int restLoad = 0;
			
			for (MacroBlock block : differences) {
				if (!block.isConvertedToVector()) {
					restLoad += block.getSquaredSize();
					restBlockManager.setObj(block);
				} else {
					this.totalPixelsProcessed += block.getSquaredSize();
				}
			}
			
			restBlockManager.compute(restLoad);
		}
		
		vecManager.compute(this.totalPixelsProcessed);
		return new VectorEngineResult(restBlockManager, vecManager);
	}
	
	/**
	 * Initializes the cosine and sine values in order to remove redundant Math.sin()
	 * and Math.cos() calls.
	 */
	private void initHexagonValues() {
		for (int i = 0; i < 6; i++) {
			double rad = PI_RAD * (i + 1);
			COS_TABLE_HEXAGON[i] = Math.cos(rad);
			SIN_TABLE_HEXAGON[i] = Math.sin(rad);
		}
	}
	
	/**
	 * <p>Returns the total MSE of the "best matching" vectors.</p>
	 * @return Total MSE
	 */
	public double getVectorMSE() {
		return this.TOTAL_MSE;
	}
}
