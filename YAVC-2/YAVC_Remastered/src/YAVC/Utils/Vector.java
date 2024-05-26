package YAVC.Utils;

import java.awt.Point;
import java.util.ArrayList;

import YAVC.Encoder.DCTEngine;

public class Vector {
	private DCTEngine DCT_ENGINE = null;
	private Point startingPoint = null;
	private int spanX = 0;
	private int spanY = 0;
	private int size = 0;
	private int reference = 0;
	private MacroBlock appendedBlock = null;
	private MacroBlock mostEqualBlock = null;
	
	private ArrayList<double[][][]> AbsoluteColorDifferenceDCTCoefficients = null;
	private boolean invokedDCTOfDifferences = false;
	
	public Vector(final Point pos, final int size) {
		if (pos == null) throw new NullPointerException("Vector can't have NULL point as startingPoint");
		
		this.startingPoint = pos;
		this.size = size;
		this.DCT_ENGINE = YAVC.Main.Main.DCT_ENGINE;
	}
	
	public void setAppendedBlock(final MacroBlock block) {
		this.appendedBlock = block;
	}
	
	public MacroBlock getAppendedBlock() {
		return this.appendedBlock;
	}
	
	public void setSpanX(final int span) {
		this.spanX = span;
	}
	
	public void setSpanY(final int span) {
		this.spanY = span;
	}
	
	public void setReference(final int reference) {
		this.reference = reference;
	}
	
	public void setSize(final int size) {
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
	
	public void setAbsolutedifferenceDCTCoefficients(final ArrayList<double[][][]> diffs) {
		if (diffs == null) throw new NullPointerException("Can't use NULL as difference");
		
		this.AbsoluteColorDifferenceDCTCoefficients = diffs;
		this.invokedDCTOfDifferences = true;
	}
	
	public void setAbsoluteDifferences(final double[][][] YUVDifference) {
		this.AbsoluteColorDifferenceDCTCoefficients = DCT_ENGINE.computeDCTOfVectorColorDifference(YUVDifference, this.size);
		this.invokedDCTOfDifferences = true;
	}
	
	public ArrayList<double[][][]> getDCTCoefficientsOfAbsoluteColorDifference() {
		return this.AbsoluteColorDifferenceDCTCoefficients;
	}
	
	public double[][][] getIDCTCoefficientsOfAbsoluteColorDifference() {
		if (this.invokedDCTOfDifferences == false) throw new NullPointerException("No absolute difference were invoked, NULL DCT-Coefficients to process");
		return DCT_ENGINE.computeIDCTOfVectorColorDifference(cloneAbsoluteColorDifference(), this.size);
	}
	
	private ArrayList<double[][][]> cloneAbsoluteColorDifference() {
		ArrayList<double[][][]> copy = new ArrayList<double[][][]>(this.AbsoluteColorDifferenceDCTCoefficients.size());
		
		for (int i = 0; i < this.AbsoluteColorDifferenceDCTCoefficients.size(); i++) {
			double[][][] ref = this.AbsoluteColorDifferenceDCTCoefficients.get(i);
			int size = this.size, halfSize = this.size / 2;
			double[][][] clone = new double[3][][];
			clone[0] = new double[size][size];
			clone[1] = new double[halfSize][halfSize];
			clone[2] = new double[halfSize][halfSize];
			
			for (int n = 0; n < ref.length; n++) {
				for (int j = 0; j < ref[n].length; j++) {
					clone[n][j] = ref[n][j].clone();
				}
			}
			
			copy.add(clone);
		}
		
		return copy;
	}
	
	public void setMostEqualBlock(final MacroBlock block) {
		this.mostEqualBlock = block;
	}
	
	public MacroBlock getMostEqualBlock() {
		return this.mostEqualBlock;
	}
}
