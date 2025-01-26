package app.engines.prediction.intraprediction.cost;

import app.rendering.ColorManager;
import app.utils.MacroBlock;
import app.utils.MathUtils;

public class SSIMCost implements CostFunction {
	@Override
	public double calcCost(double[][][] origin, MacroBlock block) {
		double SSIM_Y = getSSIMOfChannel(origin[ColorManager.Y_INDEX], block.getColors()[ColorManager.Y_INDEX]);
		double SSIM_U = getSSIMOfChannel(origin[ColorManager.U_INDEX], block.getColors()[ColorManager.U_INDEX]);
		double SSIM_V = getSSIMOfChannel(origin[ColorManager.V_INDEX], block.getColors()[ColorManager.V_INDEX]);
		return (SSIM_Y + SSIM_U + SSIM_V) / 3;
	}

	private static double getSSIMOfChannel(double[][] ch1, double[][] ch2) {
		double ch1_mean = getArrayMean(ch1);
		double ch2_mean = getArrayMean(ch2);
		double arr1Variance = getArrayVariance(ch1, ch1_mean);
		double arr2Variance = getArrayVariance(ch2, ch2_mean);
		double covariance = getArrayCovariance(ch1, ch2, ch1_mean, ch2_mean);
		double dynamicCircumference = Math.pow(2, 8) - 1; // 3 Byte per pixel
		double k1 = 0.01;
		double k2 = 0.03;
		double c1 = Math.pow(k1 * dynamicCircumference, 2);
		double c2 = Math.pow(k2 * dynamicCircumference, 2);

		return ((2 * ch1_mean * ch2_mean + c1) * (2 * covariance + c2))
				/ ((Math.pow(ch1_mean, 2) + Math.pow(ch2_mean, 2) + c1) * (arr1Variance + arr2Variance + c2));
	}

	private static double getArrayCovariance(double[][] arr1, double[][] arr2, double mean1, double mean2) {
		final int size = arr1.length * arr1[0].length;
		double covariance = 0;

		for (int x = 0; x < arr1.length; x++) {
			for (int y = 0; y < arr1[0].length; y++) {
				double deltaY1 = arr1[x][y] - mean1;
				double deltaY2 = arr2[x][y] - mean2;
				covariance += deltaY1 * deltaY2;
			}
		}

		return covariance / (double) size;
	}

	private static double getArrayVariance(double[][] arr, double mean) {
		final int size = arr.length * arr[0].length;
		double variance = 0;

		for (int x = 0; x < arr.length; x++) {
			for (int y = 0; y < arr[0].length; y++) {
				double delta = arr[x][y] - mean;
				variance += delta * delta;
			}
		}

		return variance / (double) size;
	}

	private static double getArrayMean(double[][] arr) {
		double mean = 0;
		int totalPixels = arr.length * arr[0].length;

		for (int x = 0; x < arr.length; x++) {
			for (int y = 0; y < arr[0].length; y++) {
				mean += arr[x][y];
			}
		}

		return mean / (double) totalPixels;
	}

	@Override
	public boolean betterError(double currentError, double currentBestError) {
		if (currentError < 0) {
			return false;
		}

		final double deltaCBE = MathUtils.abs(1.0 - currentBestError);
		final double deltaCE = MathUtils.abs(1.0 - currentError);
		return deltaCE < deltaCBE;
	}
}
