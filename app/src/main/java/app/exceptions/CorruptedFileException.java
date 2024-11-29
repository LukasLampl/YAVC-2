/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2024  Lukas Nian En Lampl

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
 * The {@code CorruptedFileException} serves as an Exception that should be
 * thrown, when the coded file is "corrupted" or identified as "corrupted" in
 * the decoding process.
 */
public class CorruptedFileException extends Exception {
	private static final long serialVersionUID = 4119688490860014192L;

	public CorruptedFileException(String message) {
		super(message);
	}
}
