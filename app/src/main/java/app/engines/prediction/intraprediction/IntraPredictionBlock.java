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

package app.engines.prediction.intraprediction;

import java.awt.Point;

import app.Main;
import app.engines.dct.DCTEngine;
import app.managers.Discardable;
import app.rendering.ColorManager;
import app.utils.ArrayUtils;
import app.utils.MacroBlock;

public class IntraPredictionBlock implements Discardable {
	private static DCTEngine DCT_ENGINE = Main.DCT_ENGINE;
	private MacroBlock appendedBlock = null;
	
	private int size = 0;
	private int posX = 0;
	private int posY = 0;
	private int angle = 0;
	private double horizontal[][] = null;
	private double vertical[][] = null;
	private double dctDelta[][][] = null;

	public int getSize() {
		return this.size;
	}
	
	public void setSize(int size) {
		this.size = size;
	}
	
	public int getPosX() {
		return this.posX;
	}
	
	public void setPosX(int posX) {
		this.posX = posX;
	}
	
	public int getPosY() {
		return this.posY;
	}
	
	public void setPosY(int posY) {
		this.posY = posY;
	}
	
	public int getAngle() {
		return this.angle;
	}
	
	public void setAngle(int angle) {
		this.angle = angle;
	}
	
	public double[][] getHorizontal() {
		return this.horizontal;
	}
	
	public void setHorizontal(double[][] horizontal) {
		this.horizontal = horizontal;
	}
	
	public double[][] getVertical() {
		return this.vertical;
	}
	
	public void setVertical(double[][] vertical) {
		this.vertical = vertical;
	}
	
	public double[][][] getIDCTCoefficientsDelta(boolean allowModificationToOriginalData) {
		if (allowModificationToOriginalData) {
			return DCT_ENGINE.computeIDCTOfVectorColorDifference(this.dctDelta, this.size, true);
		}
		
		double[][][] clone = cloneDCTDeltaValues();
		return DCT_ENGINE.computeIDCTOfVectorColorDifference(clone, this.size, true);
	}
	
	/**
	 * Clones the {@link #absoluteColorDifferenceDCTCoefficients} array.
	 * This function should be used for getting the IDCT values, since the
	 * original array is referenced and might get quantified by mistake
	 * if not cloned.
	 * 
	 * @return Cloned array with all the data.
	 */
	private double[][][] cloneDCTDeltaValues() {
		double[][][] ref = this.dctDelta;
		double[][][] clone = ArrayUtils.get3DArray(this.size, true);
		
		for (int i = 0; i < ColorManager.CHANNELS; i++) {
			ArrayUtils.copy2DArray(ref[i], 0, 0, clone[i], 0, 0, this.size, this.size);
		}
		
		return clone;
	}
	
	public double[][][] getDCTCoefficientsOfDelta() {
		return this.dctDelta;
	}
	
	public void setDelta(double[][][] YUVDifference) {
		this.dctDelta = DCT_ENGINE.computeDCTOfVectorColorDifference(YUVDifference, this.size, true);
	}
	
	public void setAppendedBlock(MacroBlock block) {
		this.appendedBlock = block;
	}
	
	public MacroBlock getAppendedBlock() {
		return this.appendedBlock;
	}
	
	public Point getPosition() {
		return new Point(this.posX, this.posY);
	}

	@Override
	public void discard() {
		this.size = 0;
		this.posX = 0;
		this.posY = 0;
		this.angle = 0;
		this.horizontal = null;
		this.vertical = null;
		this.dctDelta = null;
	}
}
