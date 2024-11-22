package app.utils;

public class MeanStructure {
	private final int[][] meanArgbs;
	private final int[][][] argbs;
	
	public MeanStructure(final int[][] meanArgbs, final int[][][] argbs) {
		this.meanArgbs = meanArgbs;
		this.argbs = argbs;
	}
	
	public int[][] get4x4Means() {
		return this.meanArgbs;
	}
	
	public int[][][] getArgbs() {
		return this.argbs;
	}
}
