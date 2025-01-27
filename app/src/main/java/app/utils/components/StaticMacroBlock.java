package app.utils.components;

import app.engines.quadtree.QuadtreeBase;

public class StaticMacroBlock extends MacroBlock {
	private final static int STATIC_MACROBLOCK_SIZE = QuadtreeBase.MAX_SIZE;
	
	public StaticMacroBlock(final int x, final int y, final int size, final boolean initColor) {
		super(x, y, STATIC_MACROBLOCK_SIZE, initColor);
	}

	public void mockSize(final int size) {
		super.size = size;
	}
	
	public void setPosition(final int x, final int y) {
		super.positionX = x;
		super.positionY = y;
	}
	
	public int getActualSize() {
		return STATIC_MACROBLOCK_SIZE;
	}
}
