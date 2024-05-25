package YAVC.Utils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

import YAVC.Encoder.DCTEngine;

public class Vector {
	private static DCTEngine DCT_ENGINE = YAVC.Encoder.Encoder.DCT_ENGINE;
	private Point startingPoint = null;
	private int spanX = 0;
	private int spanY = 0;
	private int size = 0;
	private int reference = 0;
	private MacroBlock appendedBlock = null;
	private MacroBlock mostEqualBlock = null;
	
	private ArrayList<double[][][]> AbsoluteColorDifferenceDCTCoefficients = null;
	private boolean invokedDCTOfDifferences = false;
	
	public Vector(Point pos, int size) {
		this.startingPoint = pos;
		this.size = size;
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
		this.AbsoluteColorDifferenceDCTCoefficients = DCT_ENGINE.computeDCTOfVectorColorDifference(YUVDifference, this.size);
		this.invokedDCTOfDifferences = true;
	}
	
	public ArrayList<double[][][]> getDCTCoefficientsOfAbsoluteColorDifference() {
		return this.AbsoluteColorDifferenceDCTCoefficients;
	}
	
	public double[][][] getIDCTCoefficientsOfAbsoluteColorDifference() {
		if (this.invokedDCTOfDifferences == false) {
			System.err.println("No absolute difference were invoked, NULL DCT-Coefficients to process! > SKIP");
			return null;
		}
		
		return DCT_ENGINE.computeIDCTOfVectorColorDifference(this.AbsoluteColorDifferenceDCTCoefficients, this.size);
	}
	
	public void setMostEqualBlock(MacroBlock block) {
		this.mostEqualBlock = block;
	}
	
	public MacroBlock getMostEqualBlock() {
		return this.mostEqualBlock;
	}
}
