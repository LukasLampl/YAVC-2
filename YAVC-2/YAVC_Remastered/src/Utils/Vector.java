package Utils;

import java.awt.Point;

import Encoder.DCTEngine;

public class Vector {
	private static DCTEngine DCT_ENGINE = Main.Main.DCT_ENGINE;
	private Point startingPoint = null;
	private int spanX = 0;
	private int spanY = 0;
	private int size = 0;
	private int reference = 0;
	private MacroBlock appendedBlock = null;
	private MacroBlock mostEqualBlock = null;
	private double[][] YDifference = null;
	private double[][] UDifference = null;
	private double[][] VDifference = null;
	
	public Vector(Point pos) {
		this.startingPoint = pos;
	}
	
	public void setAppendedBlock(MacroBlock block) {
		this.appendedBlock = block;
	}
	
	public MacroBlock getAppendedBlock() {
		return this.appendedBlock;
	}
	
	public void setSpanX(int span) {
		this.spanX = span;
	}
	
	public void setSpanY(int span) {
		this.spanY = span;
	}
	
	public void setReference(int reference) {
		this.reference = reference;
	}
	
	public void setSize(int size) {
		this.size = size;
	}
	
	public Point getPosition() {
		return this.startingPoint;
	}
	
	public int getSpanX() {
		return this.spanX;
	}
	
	public int getSpanY() {
		return this.spanY;
	}
	
	public int getSize() {
		return this.size;
	}
	
	public int getReference() {
		return this.reference;
	}
	
	public void setAbsoluteDifferences(double[][][] YUVDifference) {
		this.YDifference = YUVDifference[0];
		this.UDifference = YUVDifference[1];
		this.VDifference = YUVDifference[2];
	}
	
	private double[][][] AbsoluteColorDifferenceDCTCoefficients = null;
	
	public double[][][] getDCTCoefficientsOfAbsoluteColorDifference() {
		this.AbsoluteColorDifferenceDCTCoefficients = DCT_ENGINE.computeDCTOfVectorColorDifference(this);
		return this.AbsoluteColorDifferenceDCTCoefficients;
	}
	
	public double[][][] getIDCTCoefficientsOfAbsoluteColorDifference() {
		return DCT_ENGINE.computeIDCTOfVectorColorDifference(this.AbsoluteColorDifferenceDCTCoefficients, this.size);
	}
	
	public double[][][] getAbsoluteColorDifference() {
		return new double[][][] {this.YDifference, this.UDifference, this.VDifference};
	}
	
	public void setMostEqualBlock(MacroBlock block) {
		this.mostEqualBlock = block;
	}
	
	public MacroBlock getMostEqualBlock() {
		return this.mostEqualBlock;
	}
}
