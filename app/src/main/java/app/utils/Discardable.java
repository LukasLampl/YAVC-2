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
 * An interface introduced to release ressources within Objects easily.
 * This interface should be used, when Objects reference to larger Objects,
 * like 3D arrays. After calling {@code discard()} the Object should
 * dereference those large Objects to release memory, preventing memory
 * leaks.
 * 
 * @author Lukas Lampl
 * @since 1.4 [Optimized prototype]
 */
public interface Discardable {
	/**
	 * Discard the Object and resets its data to default.
	 */
	public void discard();
}
