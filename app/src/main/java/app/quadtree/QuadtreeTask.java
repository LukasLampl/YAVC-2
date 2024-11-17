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

public class QuadtreeTask {
	private MeanStructure meanOf4x4Blocks = null;
	
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
	 * @param dim	Dimension of the PixelRaster.
	 * 
	 * @throws NullPointerException	When the mean of 4x4 blocks is null or the
	 * argb array is null.
	 */
	private void splitToOptimalSize(double errorThreshold, MacroBlock currentBlock, Dimension dim) {
		if (this.meanOf4x4Blocks == null) {
			throw new NullPointerException("The MeanStructure is null, can't split with null color!");
		}
		
		int[][] means = this.meanOf4x4Blocks.get4x4Means();
		int[][][] color = this.meanOf4x4Blocks.getArgbs();
		
		if (means == null) {
			throw new NullPointerException("No 4x4Mean, can't subdivide MacroBlock.");
		} else if (color == null) {
			throw new NullPointerException("No ARGB colors, can't subdivide MacroBlock.");
		}
		
		Point bPos = currentBlock.getPosition();
		Point posInParent = currentBlock.getPositionRelativeToParent();
		Point newInnerPos = new Point(posInParent.x, posInParent.y);
		int[] meanRGB = calculateMeanOfBlock(means, newInnerPos, currentBlock.getSize());
		double standardDeviation = computeStandardDeviation(meanRGB, color, newInnerPos, currentBlock.getSize());
		currentBlock.setMeanColor(meanRGB);
		
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
	 * RGB array
	 */
	private MeanStructure calculate4x4Means(MacroBlock block) {
		int[][] meanArgbs = new int[block.getSize() / 4][block.getSize() / 4];
		int[][][] argbs = new int[block.getSize()][block.getSize()][3];
		final int fraction = 64;
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		
		for (int i = 0; i < block.getSize(); i += fraction) {
			for (int j = 0; j < block.getSize(); j += fraction) {
				executor.submit(create4x4MeansFractionTask(i, j, fraction, argbs, meanArgbs, block));
			}
		}

		executor.shutdown();
		
		try {
			while (!executor.awaitTermination(1, TimeUnit.MILLISECONDS));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return new MeanStructure(meanArgbs, argbs);
	}
	
	/**
	 * <p>Creates a task for working a fraction of the means from the MacroBlock down.</p>
	 * <p>This function changes to values in the two provided arrays!</p>
	 * 
	 * @return Runnable that calculates the 4x4 means as well as the RGB color for an area
	 * within the MacroBlock, starting from startX and startY with the size frac.
	 * 
	 * @param startX	Start position X within the MacroBlock
	 * @param startY	Start position Y within the MacroBlock
	 * @param frac		Size of the fraction that should be worked down
	 * @param argbs		Array for storing all RGB colors in the fraction
	 * @param meanArgbs	Array for storing all 4x4 means
	 */
	private Runnable create4x4MeansFractionTask(final int startX, final int startY, final int frac, int[][][] argbs, int[][] meanArgbs, MacroBlock block) {
		Runnable task = () -> {
			for (int u = 0; u < frac; u += 4) {
				for (int v = 0; v < frac; v += 4) {
					int sumR = 0;
					int sumG = 0;
					int sumB = 0;
					
					for (int x = 0; x < 4; x++) {
						int iPosX = startX + u + x;
						
						for (int y = 0; y < 4; y++) {
							int iPosY = startY + v + y;
							int[] col = ColorManager.convertYUVToRGB_intARR(block.getYUV(iPosX, iPosY), null);
							sumR += col[ColorManager.R_INDEX];
							sumG += col[ColorManager.G_INDEX];
							sumB += col[ColorManager.B_INDEX];
							argbs[iPosX][iPosY] = col;
						}
					}
					
					int meanColor = (((sumR / 16) & 0xFF) << 16) | (((sumG / 16) & 0xFF) << 8) | ((sumB / 16) & 0xFF);
					meanArgbs[(startX + u) / 4][(startY + v) / 4] = meanColor;
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
	 * <ul><li>[0] = Red
	 * <li>[1] = Green
	 * <li>[2] = Blue
	 * </ul>
	 * 
	 * @param meanOf4x4Blocks	Precalculated 4x4 mean colors
	 * @param pos	Position of the child block within the root MacroBlock
	 * @param size	Size of the child block
	 */
	private int[] calculateMeanOfBlock(int[][] meanOf4x4Blocks, Point pos, int size) {
		double sumR = 0;
		double sumG = 0;
		double sumB = 0;
		int actualSize = size / 4;
		int length = actualSize * actualSize;
		int actualPosX = pos.x / 4;
		int actualPosY = pos.y / 4;
		
		for (int x = 0; x < actualSize; x++) {
			int posX = x + actualPosX;
			
			for (int y = 0; y < actualSize; y++) {
				int posY = y + actualPosY;
				int argb = meanOf4x4Blocks[posX][posY];
				double r = (argb >> 16) & 0xFF;
				double g = (argb >> 8) & 0xFF;
				double b = argb & 0xFF;
				sumR += r;
				sumG += g;
				sumB += b;
			}
		}

		sumR /= length;
		sumG /= length;
		sumB /= length;
		return new int[] {(int)MathUtils.round(sumR), (int)MathUtils.round(sumG), (int)MathUtils.round(sumB)};
	}

	/**
	 * <p>Computes the standard deviation compared to the orignal
	 * colors.</p>
	 * 
	 * @return The standard deviation of red, green and blue combined.
	 * 
	 * @param mean	Mean color of the MacroBlock
	 * @param argbs	RGB color array of the root MacroBlock
	 * @param pos	Position of the child block within the root MacroBlock
	 * @param size	Size of the child block
	 */
	private double computeStandardDeviation(int[] mean, int[][][] argbs, Point pos, int size) {
		double resR = 0;
		double resG = 0;
		double resB = 0;
		double length = size * size;
		int meanR = mean[0];
		int meanG = mean[1];
		int meanB = mean[2];
		
		for (int x = 0; x < size; x++) {
			int posX = x + pos.x;
			
			for (int y = 0; y < size; y++) {
				int posY = y + pos.y;
				int r = argbs[posX][posY][0] - meanR;
				int g = argbs[posX][posY][1] - meanG;
				int b = argbs[posX][posY][2] - meanB;
				resR += r * r;
				resG += g * g;
				resB += b * b;
			}
		}
		
		resR = Math.sqrt(resR / length);
		resG = Math.sqrt(resG / length);
		resB = Math.sqrt(resB / length);
		return (resR + resG + resB);
	}
}
