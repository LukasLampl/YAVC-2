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

public class ContextModel {
	private final static int MAX_STATE = 64;
	private final static int HALF_STATE = MAX_STATE / 2;
	
	private int state = 0;
	private byte mostProbableSymbol = 0x00;
	
	public ContextModel(final int initalState) {
		this.state = initalState;
	}
	
	public void update(final byte symbol) {
		if (symbol == this.mostProbableSymbol) {
			this.state = MathUtils.max(this.state - 1, 0);
		} else {
			this.state = MathUtils.min(this.state + 1, MAX_STATE);
			
			if (this.state >= HALF_STATE) {
				this.mostProbableSymbol = getFlippedMPS();
				this.state = MAX_STATE - this.state;
			}
		}
	}
	
	public double getProbability() {
		return (double)(MAX_STATE - this.state) / (double)MAX_STATE;
	}
	
	public byte getMostProbableSymbol() {
		return this.mostProbableSymbol;
	}
	
	private byte getFlippedMPS() {
		return (byte)(this.mostProbableSymbol == 0x00 ? 0x01 : 0x00);
	}
}
