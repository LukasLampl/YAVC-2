package Utils;

import java.awt.Point;

public class Vector {
	private Point startingPoint = null;
	private int spanX = 0;
	private int spanY = 0;
	private int size = 0;
	private int reference = 0;
	private MacroBlock appendedBlock = null;
	private MacroBlock mostEqualBlock = null;
	
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
	
	public void setMostEqualBlock(MacroBlock block) {
		this.mostEqualBlock = block;
	}
	
	public MacroBlock getMostEqualBlock() {
		return this.mostEqualBlock;
	}
}
