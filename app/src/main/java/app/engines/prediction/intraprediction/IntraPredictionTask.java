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

package app.engines.prediction.intraprediction;

import java.util.List;
import java.util.concurrent.RecursiveAction;

import app.engines.prediction.intraprediction.cost.CostFunction;
import app.engines.prediction.intraprediction.cost.MSECost;
import app.managers.LoadDistributor;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.MacroBlock;
import app.utils.PixelRaster;

public class IntraPredictionTask extends RecursiveAction {
	private static final long serialVersionUID = 2018983631713349516L;

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
	private LoadDistributor<IntraPredictionBlock> intraBlockManager = null;
	
	private PixelRaster curFrame = null;

	/**
	 * A list that holds all MacroBlocks that should be converted to vectors.
	 */
	private List<MacroBlock> blocksToConvert = null;

	public IntraPredictionTask(LoadDistributor<IntraPredictionBlock> intraBlockManager,
			List<MacroBlock> blocksToConvert, final int start, final int end,
			final PixelRaster curFrame) {
		this.blocksToConvert = blocksToConvert;
		this.intraBlockManager = intraBlockManager;
		this.start = start;
		this.end = end;
		this.curFrame = curFrame;
	}

	@Override
	protected void compute() {
		int workload = getWorkloadOfThread();

		if (workload > MAX_WORK) {
			int middle = (this.start + this.end) / 2;
			IntraPredictionTask tl = new IntraPredictionTask(this.intraBlockManager, this.blocksToConvert, this.start, middle, this.curFrame);
			IntraPredictionTask tr = new IntraPredictionTask(this.intraBlockManager, this.blocksToConvert, middle, this.end, this.curFrame);
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
	
	private void process() {
		for (int i = this.start; i < this.end; i++) {
			final MacroBlock blockToPredict = this.blocksToConvert.get(i);
			final IntraPredictionBlock predictedBlock = computeIntraPredictionBlock(blockToPredict, this.curFrame);
			
			if (predictedBlock != null) {
				this.intraBlockManager.setObj(predictedBlock);
			}
		}
	}
	
	private IntraPredictionBlock computeIntraPredictionBlock(final MacroBlock predictionBlock, final PixelRaster curFrame) {
		if (IntraPipeline.isEdgeBlock(predictionBlock, curFrame.getDimension())) {
			return null;
		}

		final CostFunction cost = new MSECost();
		int bestAngle = -1;
		double[][][] copy = ArrayUtils.get3DArray(predictionBlock.getSize(), true);
		double[][][] temp = ArrayUtils.get3DArray(predictionBlock.getSize(), true);
		double[][][] data = predictionBlock.getColors();
		ArrayUtils.copy2DArray(data[ColorManager.Y_INDEX], 0, 0, copy[ColorManager.Y_INDEX], 0, 0,
				predictionBlock.getSize(), predictionBlock.getSize());
		ArrayUtils.copy2DArray(data[ColorManager.U_INDEX], 0, 0, copy[ColorManager.U_INDEX], 0, 0,
				predictionBlock.getSize() / 2,
				predictionBlock.getSize() / 2);
		ArrayUtils.copy2DArray(data[ColorManager.V_INDEX], 0, 0, copy[ColorManager.V_INDEX], 0, 0,
				predictionBlock.getSize() / 2,
				predictionBlock.getSize() / 2);

		double error = Double.MAX_VALUE;
		double [][][] AYUV = IntraPipeline.computeAverageIntraPredictionBlock(predictionBlock);
		double err = cost.calcCost(copy, predictionBlock);
		if (cost.betterError(err, error)) {
			error = err;
			data = predictionBlock.getColors();
			ArrayUtils.copy2DArray(data[ColorManager.Y_INDEX], 0, 0, temp[ColorManager.Y_INDEX], 0, 0, predictionBlock.getSize(), predictionBlock.getSize());
			ArrayUtils.copy2DArray(data[ColorManager.U_INDEX], 0, 0, temp[ColorManager.U_INDEX], 0, 0, predictionBlock.getSize() / 2, predictionBlock.getSize() / 2);
			ArrayUtils.copy2DArray(data[ColorManager.V_INDEX], 0, 0, temp[ColorManager.V_INDEX], 0, 0, predictionBlock.getSize() / 2, predictionBlock.getSize() / 2);
		}
		for (int angle = IntraPipeline.MIN_ANGLE; angle <= IntraPipeline.MAX_ANGLE; angle += IntraPipeline.ANGLE_STEP) {
			double[][][] borderPixels = getPixels(angle, predictionBlock.getSize(), curFrame, predictionBlock.getPositionX(), predictionBlock.getPositionY());
			IntraPipeline.computeAngularIntraPredictionBlock(predictionBlock, borderPixels[IntraPipeline.YUV_VERTICAL_INDEX],
					borderPixels[IntraPipeline.YUV_HORIZONTAL_INDEX], angle, curFrame.getDimension());
			err = cost.calcCost(copy, predictionBlock);
			if (cost.betterError(err, error)) {
				error = err;
				data = predictionBlock.getColors();
				ArrayUtils.copy2DArray(data[ColorManager.Y_INDEX], 0, 0, temp[ColorManager.Y_INDEX], 0, 0,
						predictionBlock.getSize(), predictionBlock.getSize());
				ArrayUtils.copy2DArray(data[ColorManager.U_INDEX], 0, 0, temp[ColorManager.U_INDEX], 0, 0,
						predictionBlock.getSize() / 2,
						predictionBlock.getSize() / 2);
				ArrayUtils.copy2DArray(data[ColorManager.V_INDEX], 0, 0, temp[ColorManager.V_INDEX], 0, 0,
						predictionBlock.getSize() / 2,
						predictionBlock.getSize() / 2);
				bestAngle = angle;
				AYUV = borderPixels;
			}
		}
		predictionBlock.setColorComponents(temp);
		return computeDelta(predictionBlock, bestAngle, AYUV, temp, copy);
	}
	
	private double[][][] getPixels(final int angle, final int size, final PixelRaster curFrame, final int posX, final int posY) {
		double[][][] borderPixels = new double[2][size][];
		double YUVver[];
		double YUVhor[];
		
		for (int i = 0; i < size; i++) {
			if (angle > 0 && angle < 90) {
				YUVver = curFrame.getYUV(posX + i, posY - 1);
				YUVhor = curFrame.getYUV(posX - 1, posY + i);
				borderPixels[IntraPipeline.YUV_HORIZONTAL_INDEX][i] = YUVhor;
				borderPixels[IntraPipeline.YUV_VERTICAL_INDEX][i] = YUVver;
			} else {
				YUVver = curFrame.getYUV(posX - (size - i), posY - 1);
				YUVhor = curFrame.getYUV(posX + size, posY + i);
				borderPixels[IntraPipeline.YUV_VERTICAL_INDEX][size - i - 1] = YUVver;
				borderPixels[IntraPipeline.YUV_HORIZONTAL_INDEX][i] = YUVhor;
			}
		}
		
		return borderPixels;
	}
	
	private IntraPredictionBlock computeDelta(final MacroBlock predictionBlock, final int angle, final double[][][] ayuv, final double[][][] predicted, final double[][][] origin) {
		IntraPredictionBlock intra = new IntraPredictionBlock();
		intra.setSize(predictionBlock.getSize());
		intra.setPosX(predictionBlock.getPositionX());
		intra.setPosY(predictionBlock.getPositionY());
		intra.setAngle(angle);
		intra.setHorizontal(ayuv[IntraPipeline.YUV_HORIZONTAL_INDEX]);
		intra.setVertical(ayuv[IntraPipeline.YUV_VERTICAL_INDEX]);
		intra.setAppendedBlock(predictionBlock);
		
		final int halfSize = predictionBlock.getSize() / 2;
		final double[][][] deltas = ArrayUtils.get3DArray(predictionBlock.getSize(), true);
		
		for (int x = 0; x < predictionBlock.getSize(); x++) {
			for (int y = 0; y < predictionBlock.getSize(); y++) {
				deltas[ColorManager.Y_INDEX][x][y] = origin[ColorManager.Y_INDEX][x][y] - predicted[ColorManager.Y_INDEX][x][y];
			}
		}
		
		for (int x = 0; x < halfSize; x++) {
			for (int y = 0; y < halfSize; y++) {
				deltas[ColorManager.U_INDEX][x][y] = origin[ColorManager.U_INDEX][x][y] - predicted[ColorManager.U_INDEX][x][y];
				deltas[ColorManager.V_INDEX][x][y] = origin[ColorManager.V_INDEX][x][y] - predicted[ColorManager.V_INDEX][x][y];
			}
		}
		
		intra.setDelta(deltas);
		return intra;
	}
}
