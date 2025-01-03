package app.intraprediction;

import java.awt.Dimension;

import app.utils.MacroBlock;

public class IntraDecoder extends IntraPipeline {
	public void computeAngularIntraPredictionBlock(MacroBlock predictionBlock, final double[][] verticalYUV,
			final double[][] horizontalYUV, final float angle, final Dimension dim) {
		super.computeAngularIntraPredictionBlock(predictionBlock, verticalYUV, horizontalYUV, angle, dim);;
	}
}
