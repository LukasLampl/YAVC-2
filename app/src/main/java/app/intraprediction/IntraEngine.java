package app.intraprediction;

import java.awt.Color;
import java.util.List;

import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.MacroBlock;
import app.utils.MathUtils;
import app.utils.PixelRaster;

public class IntraEngine {
	
	interface CostFunction {
		double calcCost(final double[][][] origin, final MacroBlock block);
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
			for (int x = 0; x < block.getSize()/2; x++) {
				for (int y = 0; y < block.getSize()/2; y++) {
					double delta = (b[ColorManager.U_INDEX][x][y] - origin[ColorManager.U_INDEX][x][y]);
					sumU += delta * delta;
					delta = (b[ColorManager.V_INDEX][x][y] - origin[ColorManager.V_INDEX][x][y]);
					sumV += delta * delta;
				}
			}
			
			return (sumY + sumU + sumV)/((block.getSize()*block.getSize() + 2*((block.getSize()/2) * (block.getSize()/2)))) / 3;
		}	
	}
	static MSECost MSECost = new IntraEngine().new MSECost();
	
	public IntraEngine() {
	}

	public void computeIntraPrediction(final List<MacroBlock> predictionList, final PixelRaster curFrame) {
		predictionList.forEach(p -> computeIntraPredictionBlock(p, curFrame, MSECost));
	}

	private void computeIntraPredictionBlock(final MacroBlock predictionBlock, final PixelRaster curFrame, final CostFunction cost) {
		float kind = Float.MAX_VALUE;
		double [][][] copy = ArrayUtils.get3DArray(predictionBlock.getSize(), true);
		double [][][] temp = ArrayUtils.get3DArray(predictionBlock.getSize(), true);
		double [][][] data = predictionBlock.getColors();
		ArrayUtils.copy2DArray(data[ColorManager.Y_INDEX], 0, 0, copy[ColorManager.Y_INDEX], 0, 0, predictionBlock.getSize(), predictionBlock.getSize());
		ArrayUtils.copy2DArray(data[ColorManager.U_INDEX], 0, 0, copy[ColorManager.U_INDEX], 0, 0, predictionBlock.getSize()/2, predictionBlock.getSize()/2);
		ArrayUtils.copy2DArray(data[ColorManager.V_INDEX], 0, 0, copy[ColorManager.V_INDEX], 0, 0, predictionBlock.getSize()/2, predictionBlock.getSize()/2);
		
		double error = Double.MAX_VALUE;
//		computeAverageIntraPredictionBlock(predictionBlock, curFrame);
		double err = cost.calcCost(copy, predictionBlock);
//		if (err < error) {
//			error = err;
//			data = predictionBlock.getColors();
//			ArrayUtils.copy2DArray(data[ColorManager.Y_INDEX], 0, 0, temp[ColorManager.Y_INDEX], 0, 0, predictionBlock.getSize(), predictionBlock.getSize());
//			ArrayUtils.copy2DArray(data[ColorManager.U_INDEX], 0, 0, temp[ColorManager.U_INDEX], 0, 0, predictionBlock.getSize()/2, predictionBlock.getSize()/2);
//			ArrayUtils.copy2DArray(data[ColorManager.V_INDEX], 0, 0, temp[ColorManager.V_INDEX], 0, 0, predictionBlock.getSize()/2, predictionBlock.getSize()/2);
//			kind = 500;
//		}
		for (float angle = (float)22.5; angle <= (float)22.5 ; angle += 10) {
			computeAngularIntraPredictionBlock(predictionBlock, curFrame, angle);
			err = cost.calcCost(copy, predictionBlock);
			if (err < error) {
				error = err;
				data = predictionBlock.getColors();
				ArrayUtils.copy2DArray(data[ColorManager.Y_INDEX], 0, 0, temp[ColorManager.Y_INDEX], 0, 0, predictionBlock.getSize(), predictionBlock.getSize());
				ArrayUtils.copy2DArray(data[ColorManager.U_INDEX], 0, 0, temp[ColorManager.U_INDEX], 0, 0, predictionBlock.getSize()/2, predictionBlock.getSize()/2);
				ArrayUtils.copy2DArray(data[ColorManager.V_INDEX], 0, 0, temp[ColorManager.V_INDEX], 0, 0, predictionBlock.getSize()/2, predictionBlock.getSize()/2);
				kind = angle;
			}
		}
//		System.out.println(String.format("- Intraprediction: %f (%d.%d)[%d] %s", kind, predictionBlock.getPositionX(), predictionBlock.getPositionY(), predictionBlock.getSize(), new Color(ColorManager.convertYUVToRGB(predictionBlock.getMeanColor()))));
		predictionBlock.setColorComponents(temp);
	}
	
	private void computeAngularIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame,
			final float angle) {
		if (predictionBlock.getPositionX() <= 0 || predictionBlock.getPositionY() <= 0) {
			return;
		}
		if (angle == 0 || angle == 180) {
			computeVerticalIntraPredictionBlock(predictionBlock, curFrame);
			return;
		} else if (angle == 90 || angle == 270) {
			computeHorizontalIntraPredictionBlock(predictionBlock, curFrame);
			return;
		}
			
		double tan = Math.tan(Math.toRadians(angle));
		for (int t = 0; t < predictionBlock.getSize(); t++) {
			double YUVver[] = curFrame.getYUV(predictionBlock.getPositionX() + t - 1,  predictionBlock.getPositionY() - 1);
			double YUVhor[] = curFrame.getYUV(predictionBlock.getPositionX() - 1, predictionBlock.getPositionY() + t - 1);

			YUVver = YUVver[ColorManager.Y_INDEX] > 100 ? ColorManager.convertRGBToYUV(Color.RED) : ColorManager.convertRGBToYUV(Color.YELLOW);
			YUVhor = YUVhor[ColorManager.Y_INDEX] > 100 ? ColorManager.convertRGBToYUV(Color.BLUE) : ColorManager.convertRGBToYUV(Color.YELLOW);
			
//			for (int x = 0; x < predictionBlock.getSize(); x++) {
//				int z = MathUtils.round((tan * x) - (tan * t));
//				if (x >= 0 && x < predictionBlock.getSize() && z >= 0 && z < predictionBlock.getSize()) {
//					predictionBlock.setYUV(x, z, YUVver);
//				}
//			}
			for (int x = 0; x < predictionBlock.getSize(); x++) {
				int z = MathUtils.round(tan * x) + t;
				if (x >= 0 && x < predictionBlock.getSize() && z >= 0 && z < predictionBlock.getSize()) {
					predictionBlock.setYUV(x, z, YUVhor);
				}
			}
		}
	}

	private void computeVerticalIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame) {
		if (predictionBlock.getPositionY() <= 0) {
			return;
		}
		for (int x = 0; x < predictionBlock.getSize(); x++) {
			double[] YUV = curFrame.getYUV(predictionBlock.getPositionX() + x, predictionBlock.getPositionY() - 1);
			for (int y = 0; y < predictionBlock.getSize(); y++) {
				predictionBlock.setYUV(x, y, YUV);
			}
		}
	}

	private void computeHorizontalIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame) {
		if (predictionBlock.getPositionX() <= 0) {
			return;
		}
		for (int y = 0; y < predictionBlock.getSize(); y++) {
			double[] YUV = curFrame.getYUV(predictionBlock.getPositionX() - 1, predictionBlock.getPositionY() + y);
			for (int x = 0; x < predictionBlock.getSize(); x++) {
				predictionBlock.setYUV(x, y, YUV);
			}
		}
	}

	private void computeAverageIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame) {
		double[] YUV = predictionBlock.getMeanColor();
		for (int y = 0; y < predictionBlock.getSize(); y++) {
			for (int x = 0; x < predictionBlock.getSize(); x++) {
				predictionBlock.setYUV(x, y, YUV);
			}
		}
	}
}
