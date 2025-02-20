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

public class BinaryContextModel {
	private int[] frequencyOfSymbols = {1, 1};
	
	public void reset() {
		this.frequencyOfSymbols = new int[] {1, 1};
	}
	
	public int getNumberOfSymbols() {
		return 2; //Binary has either 0 or 1, thus 2 possible symbols.
	}
	
	public int getSymbolFrequency(final int symbol) {
		ensureBit(symbol);
		return frequencyOfSymbols[symbol];
	}
	
	public void incrementSymbolFrequency(final int symbol) {
		ensureBit(symbol);
		this.frequencyOfSymbols[symbol]++;
	}
	
	private void ensureBit(final int symbol) {
		if (symbol != 0x00 && symbol != 0x01) {
			throw new IllegalArgumentException("A bit must either be 0 or 1!");
		}
	}
}
