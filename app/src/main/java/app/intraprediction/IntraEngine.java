package app.intraprediction;

import java.util.List;

import app.utils.MacroBlock;
import app.utils.PixelRaster;

public class IntraEngine {
	
	public enum intraMode { VERTICAL, HORIZONTAL, AVERAGE };
	
	public IntraEngine() {
	}
	
	public void computeIntraPrediction(final List<MacroBlock> predictionList, final PixelRaster curFrame) {
		predictionList.forEach(p -> computeIntraPredictionBlock(p, curFrame));
	}
	
	private void computeIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame) {
		intraMode predictionMode = getBlockPredictionMode(predictionBlock);
		switch (predictionMode) {
		case VERTICAL:
			computeVerticalIntraPredictionBlock(predictionBlock, curFrame);
			break;
		case HORIZONTAL:
			computeHorizontalIntraPredictionBlock(predictionBlock, curFrame);
			break;			
		case AVERAGE:
			computeAverageIntraPredictionBlock(predictionBlock, curFrame);
			break;			
		default:
			throw new IllegalStateException("Prediction mode is unavailable");
		}
	}
	
	private intraMode getBlockPredictionMode(final MacroBlock predictionBlock) {
		return intraMode.AVERAGE;
	}
	
	private void computeVerticalIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame) {
		if (predictionBlock.getPositionY() <= 0) {
			return;
		}
		for (int x = 0 ; x < predictionBlock.getSize() ; x++) {
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
		for (int y = 0 ; y < predictionBlock.getSize() ; y++) {
			double[] YUV = curFrame.getYUV(predictionBlock.getPositionX() - 1, predictionBlock.getPositionY() + y);
			for (int x = 0; x < predictionBlock.getSize(); x++) {
				predictionBlock.setYUV(x, y, YUV);
			}
		}
	}

	private void computeAverageIntraPredictionBlock(MacroBlock predictionBlock, final PixelRaster curFrame) {
		double[] YUV = predictionBlock.getMeanColor();
		for (int y = 0 ; y < predictionBlock.getSize() ; y++) {
			for (int x = 0; x < predictionBlock.getSize(); x++) {
				predictionBlock.setYUV(x, y, YUV);
			}
		}
	}
}
