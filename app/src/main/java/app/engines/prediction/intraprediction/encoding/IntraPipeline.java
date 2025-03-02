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

package app.engines.prediction.intraprediction.encoding;

import java.awt.Dimension;

import app.utils.MathUtils;
import app.utils.components.MacroBlock;

public abstract class IntraPipeline {
	protected final static int YUV_HORIZONTAL_INDEX = 0;
	protected final static int YUV_VERTICAL_INDEX = 1;
	
	private final static int OFFSET_COMPENSATION = 100000;
	
	public final static int ANGLE_STEP = 5;
	public final static int MIN_ANGLE = 0;
	public final static int MAX_ANGLE = 180;
	public final static int NUM_OF_ANGLES = (MAX_ANGLE - MIN_ANGLE) / ANGLE_STEP;
	
	public static void computeAngularIntraPredictionBlock(MacroBlock predictionBlock, final double[][] verticalYUV,
			final double[][] horizontalYUV, final float angle, final Dimension dim) {
		if (isEdgeBlock(predictionBlock, dim)) {
			return;
		}
		if (angle == 0 || MathUtils.abs(angle) == 180) {
			computeVerticalIntraPredictionBlock(predictionBlock, verticalYUV);
			return;
		} else if (MathUtils.abs(angle) == 90 || MathUtils.abs(angle) == 270) {
			computeHorizontalIntraPredictionBlock(predictionBlock, horizontalYUV);
			return;
		}
		
		double tan = Math.tan(Math.toRadians(angle));
		for (int t = 0; t < predictionBlock.getSize(); t++) {
			double YUVver[] = verticalYUV[t];
			double YUVhor[] = horizontalYUV[t];
			if (angle > 0 && angle < 90) {
				if (t == 0) {
					bresenham(0, t, t, tan, predictionBlock, YUVhor);
					continue;
				}
				bresenham(t, 0, t, tan, predictionBlock, YUVver);
				bresenham(0, t, t, tan, predictionBlock, YUVhor);
			} else {
				if (t == 0) {
					bresenhamR(predictionBlock.getSize() - 1, t, t, tan, predictionBlock, YUVhor);
					continue;
				}
				bresenhamR(predictionBlock.getSize() - t - 1, 0, t, tan, predictionBlock, YUVver);
				bresenhamR(predictionBlock.getSize() - 1, t, t, tan, predictionBlock, YUVhor);
			}
		}
	}
	
	public static boolean isEdgeBlock(final MacroBlock block, final Dimension dim) {
		return block.getPositionX() <= 0 || block.getPositionY() <= 0
				|| block.getPositionX() + block.getSize() >= dim.width
				|| block.getPositionY() + block.getSize() >= dim.height;
	}
	
	protected static void computeVerticalIntraPredictionBlock(MacroBlock predictionBlock, final double[][] verticalYUV) {
		if (predictionBlock.getPositionY() <= 0) {
			throw new IllegalArgumentException("Cannot intrapredict border blocks.");
		}
		for (int x = 0; x < predictionBlock.getSize(); x++) {
			double[] YUV = verticalYUV[x];
			for (int y = 0; y < predictionBlock.getSize(); y++) {
				predictionBlock.setYUV(x, y, YUV);
			}
		}
	}

	protected static void computeHorizontalIntraPredictionBlock(MacroBlock predictionBlock, final double[][] horizontalYUV) {
		if (predictionBlock.getPositionX() <= 0) {
			throw new IllegalArgumentException("Cannot intrapredict border blocks.");
		}
		for (int y = 0; y < predictionBlock.getSize(); y++) {
			double[] YUV = horizontalYUV[y];
			for (int x = 0; x < predictionBlock.getSize(); x++) {
				predictionBlock.setYUV(x, y, YUV);
			}
		}
	}
	
	protected static double[][][] computeAverageIntraPredictionBlock(MacroBlock predictionBlock) {
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
	
	protected static void bresenham(final int x, final int y, final int t, final double m, final MacroBlock block, final double[] YUV) {
		int x1 = x;
		int x2 = x + OFFSET_COMPENSATION;
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

	protected static void bresenhamR(final int x, final int y, final int t, final double m, final MacroBlock block, final double[] YUV) {
		int x1 = x;
		int x2 = x - OFFSET_COMPENSATION;
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
	
	public static int getIndexByAngle(final int angle) {
		return angle / ANGLE_STEP;
	}
	
	public static int getAngleByIndex(final int index) {
		return index * ANGLE_STEP;
	}
}
