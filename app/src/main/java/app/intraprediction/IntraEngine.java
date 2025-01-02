package app.intraprediction;

import java.util.ArrayList;
import java.util.List;

import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.MacroBlock;
import app.utils.MathUtils;
import app.utils.PixelRaster;

public class IntraEngine {

	interface CostFunction {
		double calcCost(final double[][][] origin, final MacroBlock block);

		boolean bestError(final double currentError, final double currentBestError);
	}

	class MSECost implements CostFunction {

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
					/ ((block.getSize() * block.getSize() + 2 * ((block.getSize() / 2)
							* (block.getSize() / 2))))
					/ 3;
		}

		@Override
		public boolean bestError(double currentError, double currentBestError) {
			return (currentError < currentBestError);
		}
	}

	class SSIMCost implements CostFunction {

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
		public boolean bestError(double currentError, double currentBestError) {
			if (currentError < 0) {
				return false;
			}

			final double deltaCBE = MathUtils.abs(1.0 - currentBestError);
			final double deltaCE = MathUtils.abs(1.0 - currentError);
			return deltaCE < deltaCBE;
		}
	}

	class PSNRCost implements CostFunction {
		private MSECost mseCost = new IntraEngine().new MSECost();

		@Override
		public double calcCost(double[][][] origin, MacroBlock block) {
			final double MSE = this.mseCost.calcCost(origin, block);
			double PSNR = 20 * Math.log10(Math.pow(255, 3)) - 10 * Math.log10(MSE);
			return PSNR;
		}

		@Override
		public boolean bestError(double currentError, double currentBestError) {
			return currentError < currentBestError;
		}
	}

	class SAECost implements CostFunction {

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

			return sumErr;
		}

		@Override
		public boolean bestError(double currentError, double currentBestError) {
			return currentError < currentBestError;
		}

	}

	class MAECost implements CostFunction {

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
		public boolean bestError(double currentError, double currentBestError) {
			return currentError < currentBestError;
		}

	}

	static MSECost costFnc = new IntraEngine().new MSECost();
	private final static int YUV_HORIZONTAL_INDEX = 0;
	private final static int YUV_VERTICAL_INDEX = 1;
	
	public IntraEngine() {
	}

	public List<IntraPredictionBlock> computeIntraPrediction(final List<MacroBlock> predictionList, final PixelRaster curFrame) {
		List<IntraPredictionBlock> predictedBlocks = new ArrayList<IntraPredictionBlock>(predictionList.size());
		predictionList.forEach(p -> predictedBlocks.add(computeIntraPredictionBlock(p, curFrame, costFnc)));
		return predictedBlocks;
	}

	private IntraPredictionBlock computeIntraPredictionBlock(final MacroBlock predictionBlock, final PixelRaster curFrame,
			final CostFunction cost) {
		if (predictionBlock.getPositionX() == 0 || predictionBlock.getPositionY() == 0) {
			return null;
		}

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
		double [][][] AYUV = computeAverageIntraPredictionBlock(predictionBlock, curFrame);
		double err = cost.calcCost(copy, predictionBlock);
		if (cost.bestError(err, error)) {
			error = err;
			data = predictionBlock.getColors();
			ArrayUtils.copy2DArray(data[ColorManager.Y_INDEX], 0, 0, temp[ColorManager.Y_INDEX], 0, 0, predictionBlock.getSize(), predictionBlock.getSize());
			ArrayUtils.copy2DArray(data[ColorManager.U_INDEX], 0, 0, temp[ColorManager.U_INDEX], 0, 0, predictionBlock.getSize() / 2, predictionBlock.getSize() / 2);
			ArrayUtils.copy2DArray(data[ColorManager.V_INDEX], 0, 0, temp[ColorManager.V_INDEX], 0, 0, predictionBlock.getSize() / 2, predictionBlock.getSize() / 2);
		}
		for (int angle = 0; angle <= 180; angle += 5) {
			double [][][] TYUV = computeAngularIntraPredictionBlock(predictionBlock, curFrame, angle);
			err = cost.calcCost(copy, predictionBlock);
			if (cost.bestError(err, error)) {
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
				AYUV = TYUV;
			}
		}
		predictionBlock.setColorComponents(temp);
		return computeDelta(predictionBlock, bestAngle, AYUV, temp, copy);
	}

	private double[][][] computeAngularIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame,
			final float angle) {
		if (predictionBlock.getPositionX() <= 0 || predictionBlock.getPositionY() <= 0
				|| predictionBlock.getPositionX() + predictionBlock.getSize() >= curFrame.getWidth()
				|| predictionBlock.getPositionY() + predictionBlock.getSize() >= curFrame.getHeight()) {
			return null;
		}
		if (angle == 0 || MathUtils.abs(angle) == 180) {
			return computeVerticalIntraPredictionBlock(predictionBlock, curFrame);
		} else if (MathUtils.abs(angle) == 90 || MathUtils.abs(angle) == 270) {
			return computeHorizontalIntraPredictionBlock(predictionBlock, curFrame);
		}

		double [][][] AYUV = new double[2][predictionBlock.getSize()][];
		double tan = Math.tan(Math.toRadians(angle));
		for (int t = 0; t < predictionBlock.getSize(); t++) {
//			double YUVver[] = (t % 1 == 0) ? ColorManager.convertRGBToYUV(Color.ORANGE.getRGB()) : ColorManager.convertRGBToYUV(Color.WHITE.getRGB());
//			double YUVhor[] = (t % 1 == 0) ? ColorManager.convertRGBToYUV(Color.BLUE.getRGB()) : ColorManager.convertRGBToYUV(Color.WHITE.getRGB());
			double YUVver[];
			double YUVhor[];
			if (angle > 0 && angle <= 90) {
				YUVver = curFrame.getYUV(predictionBlock.getPositionX() + t, predictionBlock.getPositionY() - 1);
				YUVhor = curFrame.getYUV(predictionBlock.getPositionX() - 1, predictionBlock.getPositionY() + t);
				if (t == 0) {
					bresenham(0, t, t, tan, predictionBlock, YUVhor);
					continue;
				}
				bresenham(t, 0, t, tan, predictionBlock, YUVver);
				bresenham(0, t, t, tan, predictionBlock, YUVhor);
				AYUV[YUV_HORIZONTAL_INDEX][t] = YUVhor;
				AYUV[YUV_VERTICAL_INDEX][t] = YUVver;
			} else {
				YUVver = curFrame.getYUV(predictionBlock.getPositionX() + (predictionBlock.getSize() - t), predictionBlock.getPositionY() - 1);
				YUVhor = curFrame.getYUV(predictionBlock.getSize() + predictionBlock.getPositionX(), predictionBlock.getPositionY() + t);
				if (t == 0) {
					bresenhamR(predictionBlock.getSize() - 1, t, t, tan, predictionBlock, YUVhor);
					continue;
				}
				bresenhamR(predictionBlock.getSize() - t - 1, 0, t, tan, predictionBlock, YUVver);
				bresenhamR(predictionBlock.getSize() - 1, t, t, tan, predictionBlock, YUVhor);
				AYUV[YUV_VERTICAL_INDEX][predictionBlock.getSize() - t - 1] = YUVver;
				AYUV[YUV_HORIZONTAL_INDEX][t] = YUVhor;
			}
		}
		return AYUV;
	}

	private void bresenham(final int x, final int y, final int t, final double m, final MacroBlock block, final double[] YUV) {
		int x1 = x;
		int x2 = x + 100000;
		int y1 = y;
		int y2 = MathUtils.round(m * x2) + y;
		int dx = (int) MathUtils.abs(x2 - x1);
		int dy = (int) -MathUtils.abs(y2 - y1);
		int sx = (x1 < x2) ? 1 : -1;
		int sy = (y1 < y2) ? 1 : -1;
		int err = dx + dy;
		int e2;
		for (int i = 0; i < block.getSize(); i++) {
			if (x1 < 0 || x1 >= block.getSize() || y1 < 0 || y1 >= block.getSize()) {
				break;			
			}
			block.setYUV(x1, y1, YUV);
			e2 = err + err;
			if (e2 > dy) {
				err += dy;
				x1 += sx;

			}
			if (e2 < dx) {
				err += dx;
				y1 += sy;
			}
		}
	}

	private void bresenhamR(final int x, final int y, final int t, final double m, final MacroBlock block, final double[] YUV) {
		int x1 = x;
		int x2 = x - 100000;
		int y1 = y; 
		int y2 = MathUtils.round(m * x2) + t;
		int dx = (int) MathUtils.abs(x2 - x1);
		int dy = (int) -MathUtils.abs(y2 - y1);
		int sx = (x1 < x2) ? 1 : -1;
		int sy = (y1 < y2) ? 1 : -1;
		int err = dx + dy;
		int e2;
		for (int i = 0; i < block.getSize(); i++) {
			if (x1 < 0 || x1 >= block.getSize() || y1 < 0 || y1 >= block.getSize()) {
				break;			
			}
			block.setYUV(x1, y1, YUV);
			e2 = err + err;
			if (e2 > dy) {
				err += dy;
				x1 += sx;

			}
			if (e2 < dx) {
				err += dx;
				y1 += sy;
			}
		}
	}

	private double [][][] computeVerticalIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame) {
		if (predictionBlock.getPositionY() <= 0) {
			throw new IllegalArgumentException("Cannot intrapredict border blocks.");
		}
		double [][][] AYUV = new double[2][predictionBlock.getSize()][];
		for (int x = 0; x < predictionBlock.getSize(); x++) {
			double[] YUV = curFrame.getYUV(predictionBlock.getPositionX() + x, predictionBlock.getPositionY() - 1);
			for (int y = 0; y < predictionBlock.getSize(); y++) {
				predictionBlock.setYUV(x, y, YUV);
			}
			AYUV[YUV_VERTICAL_INDEX][x] = YUV;
			AYUV[YUV_HORIZONTAL_INDEX][x] = null;
		}
		return AYUV;
	}

	private double [][][] computeHorizontalIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame) {
		if (predictionBlock.getPositionX() <= 0) {
			throw new IllegalArgumentException("Cannot intrapredict border blocks.");
		}
		double [][][] AYUV = new double[2][predictionBlock.getSize()][];
		for (int y = 0; y < predictionBlock.getSize(); y++) {
			double[] YUV = curFrame.getYUV(predictionBlock.getPositionX() - 1, predictionBlock.getPositionY() + y);
			for (int x = 0; x < predictionBlock.getSize(); x++) {
				predictionBlock.setYUV(x, y, YUV);
			}
			AYUV[YUV_VERTICAL_INDEX][y] = null;
			AYUV[YUV_HORIZONTAL_INDEX][y] = YUV;
		}
		return AYUV;
	}

	private double[][][] computeAverageIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame) {
		double[] YUV = predictionBlock.getMeanColor();
		double [][][] AYUV = new double[2][predictionBlock.getSize()][];
		for (int y = 0; y < predictionBlock.getSize(); y++) {
			for (int x = 0; x < predictionBlock.getSize(); x++) {
				predictionBlock.setYUV(x, y, YUV);
			}
			AYUV[YUV_VERTICAL_INDEX][y] = YUV;
			AYUV[YUV_HORIZONTAL_INDEX][y] = YUV;
 		}
		return AYUV;
	}
	
	private IntraPredictionBlock computeDelta(final MacroBlock predictionBlock, final float angle, final double[][][] ayuv, final double[][][] predicted, final double[][][] origin) {
		IntraPredictionBlock intra = new IntraPredictionBlock();
		intra.setSize(predictionBlock.getSize());
		intra.setPosX(predictionBlock.getPositionX());
		intra.setPosY(predictionBlock.getPositionY());
		intra.setAngle((int) angle);
		intra.setHorizontal(ayuv[YUV_HORIZONTAL_INDEX]);
		intra.setVertical(ayuv[YUV_VERTICAL_INDEX]);
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
