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
