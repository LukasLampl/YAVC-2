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

package app.dct;

/**
 * The {@code DCTConstants} class is a collection of all
 * constatns used for the DCT-II operations performed by the
 * {@link app.dct.DCTEngine DCTEngine}.
 * 
 * @author Lukas Lampl
 * @since 1.4.4 [Optimized prototype]
 */
public class DCTConstants {
	/**
	 * Specifies the index at which to expect the Y-Component in
	 * a DCT-II or IDCT coefficient matrix that is 3 dimensional.
	 */
	public static final int Y_COEFFS_INDEX = 0;
	
	/**
	 * Specifies the index at which to expect the U-Component in
	 * a DCT-II or IDCT coefficient matrix that is 3 dimensional.
	 */
	public static final int U_COEFFS_INDEX = 1;
	
	/**
	 * Specifies the index at which to expect the V-Component in
	 * a DCT-II or IDCT coefficient matrix that is 3 dimensional.
	 */
	public static final int V_COEFFS_INDEX = 2;
}
