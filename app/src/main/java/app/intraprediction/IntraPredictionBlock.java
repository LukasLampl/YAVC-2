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

package app.intraprediction;

import java.awt.Point;

import app.utils.Discardable;
import app.utils.MacroBlock;

public class IntraPredictionBlock implements Discardable {
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
	
	public double[][][] getDelta() {
//		return DCT_ENGINE.computeIDCTOfVectorColorDifference(this.dctDelta, this.size, true);
		return this.dctDelta;
	}
	
	public void setDelta(double[][][] delta) {
//		this.dctDelta = DCT_ENGINE.computeDCTOfVectorColorDifference(delta, this.size, true);
		this.dctDelta = delta;
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
