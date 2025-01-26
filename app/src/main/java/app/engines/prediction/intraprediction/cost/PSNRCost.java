package app.engines.prediction.intraprediction.cost;

import app.utils.MacroBlock;

public class PSNRCost implements CostFunction {
	private MSECost mseCost = new MSECost();

	@Override
	public double calcCost(double[][][] origin, MacroBlock block) {
		final double MSE = this.mseCost.calcCost(origin, block);
		double PSNR = 20 * Math.log10(Math.pow(255, 3)) - 10 * Math.log10(MSE);
		return PSNR;
	}

	@Override
	public boolean betterError(double currentError, double currentBestError) {
		return currentError < currentBestError;
	}
}
