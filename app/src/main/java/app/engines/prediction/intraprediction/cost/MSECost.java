package app.engines.prediction.intraprediction.cost;

import app.rendering.ColorManager;
import app.utils.MacroBlock;

public class MSECost implements CostFunction {
	@Override
	public double calcCost(double[][][] origin, MacroBlock block) {
		double[][][] b = block.getColors();
		double sumY = 0, sumU = 0, sumV = 0;
		for (int x = 0; x < block.getSize(); x++) {
			for (int y = 0; y < block.getSize(); y++) {
				double delta = (b[ColorManager.Y_INDEX][x][y] - origin[ColorManager.Y_INDEX][x][y]);
				sumY += delta * delta;
			}
		}
		for (int x = 0; x < block.getSize() / 2; x++) {
			for (int y = 0; y < block.getSize() / 2; y++) {
				double delta = (b[ColorManager.U_INDEX][x][y] - origin[ColorManager.U_INDEX][x][y]);
				sumU += delta * delta;
				delta = (b[ColorManager.V_INDEX][x][y] - origin[ColorManager.V_INDEX][x][y]);
				sumV += delta * delta;
			}
		}

		return (sumY + sumU + sumV)
				/ ((block.getSize() * block.getSize() + 2 * ((block.getSize() / 2) * (block.getSize() / 2)))) / 3;
	}

	@Override
	public boolean betterError(double currentError, double currentBestError) {
		return (currentError < currentBestError);
	}
}
