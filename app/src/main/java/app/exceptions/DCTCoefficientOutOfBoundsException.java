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

package app.exceptions;

/**
 * The {@code DCTCoefficientOutOfBoundsException} should be thrown, if a
 * DCT coefficients exceeds -127 or 127 in the coding (writing) process.
 * 
 * @author Lukas Lampl
 * @version 1.0.0 [optimized_prototype_2]
 */
public class DCTCoefficientOutOfBoundsException extends Exception {
	private static final long serialVersionUID = 7870111366146318728L;

	/**
	 * Creates a new {@code DCTCoefficientOutOfBoundsException} with the given
	 * message.
	 * 
	 * @param msg	The message to throw.
	 */
	public DCTCoefficientOutOfBoundsException(final String msg) {
		super(msg);
	}
}
