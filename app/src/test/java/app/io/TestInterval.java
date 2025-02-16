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

package app.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import app.io.coder.cabac.Interval;

public class TestInterval {
	@Test
	public void test001() {
		Interval i = new Interval(0, Integer.MAX_VALUE);
		i.shift();
		assertEquals(0, i.getLow());
		assertEquals((Integer.MAX_VALUE << 1) & Interval.ALLOWED_BITS_MASK, i.getHigh());
	}
	
	@Test
	public void test002() {
		Interval i = new Interval(1, 1);
		
		for (int c = 0; c < Interval.MAX_BITS - 1; c++) {
			i.shift();
		}
		
		assertTrue(i.isRangeReadyForOutput());
	}
	
	@Test
	public void test003() {
		Interval i = new Interval(0, 12);
		assertTrue(i.isNElementOf(0));
		assertTrue(i.isNElementOf(1));
		assertTrue(i.isNElementOf(2));
		assertTrue(i.isNElementOf(3));
		assertTrue(i.isNElementOf(4));
		assertTrue(i.isNElementOf(5));
		assertTrue(i.isNElementOf(6));
		assertTrue(i.isNElementOf(7));
		assertTrue(i.isNElementOf(8));
		assertTrue(i.isNElementOf(9));
		assertTrue(i.isNElementOf(10));
		assertTrue(i.isNElementOf(11));
		assertFalse(i.isNElementOf(12));
	}
	
	@Test
	public void test004() {
		Interval i = new Interval(1, Integer.MAX_VALUE);
		
		for (int c = 0; c < Interval.MAX_BITS - 1; c++) {
			i.shift();
		}
		
		assertEquals(0x01, i.getLowMSB());
	}
}
