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

package app.io.coder.cabac;

import app.utils.MathUtils;

public class Interval {
	public final static int MSB_MASK = 0x40000000;
	public final static int ALLOWED_BITS_MASK = 0x7FFFFFFF;
	public final static int MAX_BITS = Integer.SIZE - 1;
	
	private int high = 0;
	private int low = 0;
	
	public Interval(final int low, final int high) {
		this.high = high & ALLOWED_BITS_MASK;
		this.low = low & ALLOWED_BITS_MASK;
	}
	
	public Interval[] subdivide(final double percentage) {
		if (percentage < 0 || percentage > 1) {
			throw new IllegalStateException("Percentages below 0 and greater than 1 are not allowed!");
		}
		
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
		return (byte)((this.low >> 30) & 0x01);
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
