package app.engines.prediction.intraprediction.cost;

import app.rendering.ColorManager;
import app.utils.MathUtils;
import app.utils.components.MacroBlock;

public class MAECost implements CostFunction {
	@Override
	public double calcCost(double[][][] origin, MacroBlock block) {
		double sumErr = 0;
		final double[][][] data = block.getColors();

		for (int x = 0; x < block.getSize(); x++) {
			for (int y = 0; y < block.getSize(); y++) {
				double delta = MathUtils.abs(data[ColorManager.Y_INDEX][x][y] - origin[ColorManager.Y_INDEX][x][y]);
				sumErr += delta;
			}
		}

		for (int x = 0; x < block.getSize() / 2; x++) {
			for (int y = 0; y < block.getSize() / 2; y++) {
				double deltaU = MathUtils
						.abs(data[ColorManager.U_INDEX][x][y] - origin[ColorManager.U_INDEX][x][y]);
				double deltaV = MathUtils
						.abs(data[ColorManager.V_INDEX][x][y] - origin[ColorManager.V_INDEX][x][y]);
				sumErr += deltaU + deltaV;
			}
		}

		return sumErr / (block.getSize() * block.getSize() + (2 * block.getSize() / 2 * block.getSize() / 2));
	}

	@Override
	public boolean betterError(double currentError, double currentBestError) {
		return currentError < currentBestError;
	}
}
