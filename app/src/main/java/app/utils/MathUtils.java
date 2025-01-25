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

package app.utils;

/**
 * The class {@code MathUtils} is a replacement of the
 * standard {@code Math} class to only do the necessary
 * calculation needed for the YAVC instead of generalizing everything.
 * 
 * @author Hans Lampl
 * @since 1.2 [Optimized prototype]
 */
public class MathUtils {
	/**
	 * Get the absolute value of the given value.
	 * 
	 * @param value	The value from which to get the absolute value from.
	 * @return The absolute value of the given value.
	 */
	public static double abs(final double value) {
		return (value < 0.0) ? -value : value;
	}
	
	/**
	 * Rounds a given double to the nearest integer by rounding mode
	 * HALF_UP, meaning if the first digit of the scale is >= 5 it rounds
	 * up in the positive way or down in the negative way.
	 * 
	 * @param value	The value to round.
	 * @return The rounded value.
	 */
	public static int round(final double value) {
		return value >= 0.0 ? (int)(value + 0.5) : (int)(value - 0.5);
	}
}
