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

/**
 * The {@code BinaryContextModel} is a probability model for CABAC encoding and decoding
 * and holds all occurring frequencies of a binary state.
 * 
 * <p><b>Note:</b><br>
 * Using multiple models on a data stream is more efficient, since the predictions
 * do not alter as much as having a general model.
 * </p>
 * 
 * @author Lukas Lampl
 * @since 2.1.1 [QT_COMP]
 */
public class BinaryContextModel {
	/**
	 * The frequency of each binary state for the model.
	 */
	private int[] frequencyOfSymbols = {1, 1};
	
	/**
	 * Resets the model to the original state.
	 */
	public void reset() {
		this.frequencyOfSymbols = new int[] {1, 1};
	}
	
	/**
	 * Returns the number of symbols on the context model.
	 * 
	 * @return The number of symbols of the context model.
	 */
	public int getNumberOfSymbols() {
		return 2; //Binary has either 0 or 1, thus 2 possible symbols.
	}
	
	/**
	 * Gets the frequency of a given binary state.
	 * 
	 * @param symbol	The binary state from which to get the frequency of.
	 * @return The frequency of the given binary state.
	 */
	public int getSymbolFrequency(final int symbol) {
		ensureBit(symbol);
		return frequencyOfSymbols[symbol];
	}
	
	/**
	 * Increments the frequency of a given binary state by 1.
	 * 
	 * @param symbol	Binary state of which to increase the occurring frequency.
	 */
	public void incrementSymbolFrequency(final int symbol) {
		ensureBit(symbol);
		this.frequencyOfSymbols[symbol]++;
	}
	
	/**
	 * Ensures that a given symbol is a bit.
	 * 
	 * @param symbol	Symbol to check.
	 */
	private void ensureBit(final int symbol) {
		if (symbol != 0x00 && symbol != 0x01) {
			throw new IllegalArgumentException("A bit must either be 0 or 1!");
		}
	}
}
