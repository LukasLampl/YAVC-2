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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import app.rendering.ColorManager;
import app.utils.MacroBlock;
import app.utils.MathUtils;
import app.utils.MeanStructure;

/**
 * The {@code QuadtreeTask} is responsible for subdividing individual MacroBlocks
 * into smaller MacroBlocks. The met condition for splitting is that the standard
 * deviation of the mean color in the block is larger than a specified threshold.
 * 
 * @author Lukas Lampl
 * @since 1.4 [Optimized prototype]
 */
public class QuadtreeTask {
	/**
	 * The structure holding all mean color values and RGB values.
	 */
	private MeanStructure meanOf4x4Blocks = null;
	
	/**
	 * Splits a given origin block into sub-blocks, until the standard deviation
	 * of the block's mean color is below a certain threshold.
	 * 
	 * @param origin			The origin block to split.
	 * @param dim				Dimension of the frame (to keep track of out of bounds blocks).
	 * @param errorThreshold	The standard deviation threshold.
	 */
	public void splitOriginBlock(MacroBlock origin, Dimension dim, double errorThreshold) {
		this.meanOf4x4Blocks = calculate4x4Means(origin);
		splitToOptimalSize(errorThreshold, origin, dim);
	}
	
	/**
	 * <p>Subdivides a MacroBlock into 4 equally sized subblocks using recursion.
	 * The subdivision is determined by the standard deviation of the mean-color
	 * and actual color of the current MacroBlock.</p>
	 * 
	 * <p>The minimum size is 4. When a subdivided block is out of the
	 * PixelRaster, it is destroyed. If a subdivided MacroBlock is at the
	 * boundary, it is split, until it is fully inside.</p>
	 * 
	 * @param errorThreshold	Maximum error, until the block is split.
	 * @param currentBlock		The currentBlock to split to optimum.
	 * @param dim				Dimension of the PixelRaster.
	 * 
	 * @throws NullPointerException	When the mean of 4x4 blocks is null or the
	 * argb array is null.
	 */
	private void splitToOptimalSize(double errorThreshold, MacroBlock currentBlock, Dimension dim) {
		if (this.meanOf4x4Blocks == null) {
			throw new NullPointerException("The MeanStructure is null, can't split with null color!");
		}
		
		double[][][] means = this.meanOf4x4Blocks.get4x4Means();
		double[][][] color = this.meanOf4x4Blocks.getArgbs();
		
		if (means == null) {
			throw new NullPointerException("No 4x4Mean, can't subdivide MacroBlock.");
		} else if (color == null) {
			throw new NullPointerException("No RGB colors, can't subdivide MacroBlock.");
		}
		
		Point bPos = currentBlock.getPosition();
		Point posInParent = currentBlock.getPositionRelativeToParent();
		Point newInnerPos = new Point(posInParent.x, posInParent.y);
		double[] meanYUV = calculateMeanOfBlock(means, newInnerPos, currentBlock.getSize());
		double standardDeviation = computeStandardDeviation(meanYUV, color, newInnerPos, currentBlock.getSize());
		currentBlock.setMeanColor(meanYUV);
		
		if (standardDeviation > errorThreshold
			|| bPos.x + currentBlock.getSize() > dim.width
			|| bPos.y + currentBlock.getSize() > dim.height) {
			currentBlock.subdivide(dim);
		}
		
		if (currentBlock.getNodes() != null) {
			for (MacroBlock block : currentBlock.getNodes()) {
				if (block == null) {
					continue;
				}
				
				splitToOptimalSize(errorThreshold, block, dim);
			}
		}
	}
	
	/**
	 * <p>Calculate all 4x4 sized mean colors of the MacroBlock
	 * and while that an array of all RGB colors is created.</p>
	 * 
	 * <p><strong>Warning:</strong> The process is multithreaded.
	 * Event though it might lead to performance impact if used a lot.</p>
	 * 
	 * @return A structure that contains the 4x4 mean colors and the
	 * RGB array.
	 */
	private MeanStructure calculate4x4Means(MacroBlock block) {
		double[][][] meanYuvs = new double[block.getSize() / 4][block.getSize() / 4][];
		double[][][] yuvs = new double[block.getSize()][block.getSize()][];
		final int fraction = 64;
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		for (int i = 0; i < block.getSize(); i += fraction) {
			for (int j = 0; j < block.getSize(); j += fraction) {
				executor.submit(create4x4MeansFractionTask(i, j, fraction, yuvs, meanYuvs, block));
			}
		}

		executor.shutdown();
		
		try {
			while (!executor.awaitTermination(1, TimeUnit.MILLISECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new MeanStructure(meanYuvs, yuvs);
	}
	
	/**
	 * <p>Creates a task for working a fraction of the means from the MacroBlock down.</p>
	 * <p>This function changes to values in the two provided arrays!</p>
	 * 
	 * @return Runnable that calculates the 4x4 means as well as the RGB color for an area
	 * within the MacroBlock, starting from startX and startY with the size frac.
	 * 
	 * @param startX	Start position X within the MacroBlock.
	 * @param startY	Start position Y within the MacroBlock.
	 * @param frac		Size of the fraction that should be worked down.
	 * @param yuvs		Array for storing all YUV colors in the fraction.
	 * @param meanYuvs	Array for storing all 4x4 means.
	 */
	private Runnable create4x4MeansFractionTask(final int startX, final int startY, final int frac, double[][][] yuvs, double[][][] meanYuvs, MacroBlock block) {
		Runnable task = () -> {
			for (int u = 0; u < frac; u += 4) {
				for (int v = 0; v < frac; v += 4) {
					int sumY = 0;
					int sumU = 0;
					int sumV = 0;
					
					for (int x = 0; x < 4; x++) {
						int iPosX = startX + u + x;
						
						for (int y = 0; y < 4; y++) {
							int iPosY = startY + v + y;
							double[] col = block.getYUV(iPosX, iPosY);
							sumY += col[ColorManager.Y_INDEX];
							sumU += col[ColorManager.U_INDEX];
							sumV += col[ColorManager.V_INDEX];
							yuvs[iPosX][iPosY] = col;
						}
					}
					
					double[] meanColor = new double[] {sumY / 16, sumU / 16, sumV / 16};
					meanYuvs[(startX + u) / 4][(startY + v) / 4] = meanColor;
				}
			}
		};
		
		return task;
	}

	/**
	 * <p>This calculates the mean of a child block.</p>
	 * 
	 * @return An array containing the mean of every RGB component in
	 * the following order:
	 * <ul><li>[0] = Y
	 * <li>[1] = U
	 * <li>[2] = V
	 * </ul>
	 * 
	 * @param meanOf4x4Blocks	Precalculated 4x4 mean colors.
	 * @param pos				Position of the child block within the root MacroBlock.
	 * @param size				Size of the child block.
	 */
	private double[] calculateMeanOfBlock(double[][][] meanOf4x4Blocks, Point pos, int size) {
		double sumY = 0;
		double sumU = 0;
		double sumV = 0;
		int actualSize = size / 4;
		int length = actualSize * actualSize;
		int actualPosX = pos.x / 4;
		int actualPosY = pos.y / 4;
		
		for (int x = 0; x < actualSize; x++) {
			int posX = x + actualPosX;
			
			for (int y = 0; y < actualSize; y++) {
				int posY = y + actualPosY;
				double[] yuv = meanOf4x4Blocks[posX][posY];
				sumY += yuv[ColorManager.Y_INDEX];
				sumU += yuv[ColorManager.U_INDEX];
				sumV += yuv[ColorManager.V_INDEX];
			}
		}

		sumY /= length;
		sumU /= length;
		sumV /= length;
		return new double[] {(int)MathUtils.round(sumY), (int)MathUtils.round(sumU), (int)MathUtils.round(sumV)};
	}

	/**
	 * <p>Computes the standard deviation compared to the orignal
	 * colors.</p>
	 * 
	 * @return The standard deviation of red, green and blue combined.
	 * 
	 * @param mean	Mean color of the MacroBlock.
	 * @param yuvs	YUV color array of the root MacroBlock.
	 * @param pos	Position of the child block within the root MacroBlock.
	 * @param size	Size of the child block.
	 */
	private double computeStandardDeviation(double[] mean, double[][][] yuvs, Point pos, int size) {
		double resY = 0;
		double resU = 0;
		double resV = 0;
		double length = size * size;
		double meanY = mean[ColorManager.Y_INDEX];
		double meanU = mean[ColorManager.U_INDEX];
		double meanV = mean[ColorManager.V_INDEX];
		
		for (int x = 0; x < size; x++) {
			int posX = x + pos.x;
			
			for (int y = 0; y < size; y++) {
				int posY = y + pos.y;
				double deltaY = yuvs[posX][posY][ColorManager.Y_INDEX] - meanY;
				double deltaU = yuvs[posX][posY][ColorManager.U_INDEX] - meanU;
				double deltaV = yuvs[posX][posY][ColorManager.V_INDEX] - meanV;
				resY += deltaY * deltaY;
				resU += deltaU * deltaU;
				resV += deltaV * deltaV;
			}
		}
		
		resY = Math.sqrt((resY * ColorManager.Y_WEIGHT) / length);
		resU = Math.sqrt((resU * ColorManager.U_WEIGHT) / length);
		resV = Math.sqrt((resV * ColorManager.V_WEIGHT) / length);
		return (resY + resU + resV);
	}
}
