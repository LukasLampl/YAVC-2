package app.engines.prediction.intraprediction.cost;

import app.utils.components.MacroBlock;

public interface CostFunction {
	double calcCost(final double[][][] origin, final MacroBlock block);

	boolean betterError(final double currentError, final double currentBestError);
}
