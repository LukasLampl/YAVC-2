package app.io.coder.cabac;

import app.utils.MathUtils;

public class Interval {
	private final static int MSB_MASK = 0x40000000;
	private final static int ALLOWED_BITS_MASK = 0x7FFFFFFF;
	
	private int high = 0;
	private int low = 0;
	
	public Interval(final int low, final int high) {
		this.high = high;
		this.low = low;
	}
	
	public Interval[] subdivide(final double percentage) {
		final int range = this.high - this.low;
		final int mid = this.low + MathUtils.round(range * percentage);
		return new Interval[] {new Interval(this.low, mid), new Interval(mid, this.high)};
	}
	
	public boolean isNElementOf(final int element) {
		return this.high > element && this.low <= element;
	}
	
	public boolean isRangeReadyForOutput() {
		return (this.low & MSB_MASK) == (this.high & MSB_MASK);
	}
	
	public int getHigh() {
		return this.high;
	}
	
	public int getLow() {
		return this.low;
	}
	
	public byte getLowMSB() {
		return (byte)((this.low & MSB_MASK) >> 30);
	}
	
	public void shift() {
		this.low = (this.low << 1) & ALLOWED_BITS_MASK;
		this.high = (this.high << 1) & ALLOWED_BITS_MASK;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "["
				+ this.low + " - " + this.high + ")";
	}
}
