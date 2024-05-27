package YAVC.Utils;

public class MeanStructure {
	private int[][] meanArgbs = null;
	private int[][][] argbs = null;
	
	public MeanStructure(int[][] meanArgbs, int[][][] argbs) {
		this.meanArgbs = meanArgbs;
		this.argbs = argbs;
	}
	
	public int[][] get4x4Means() {
		return this.meanArgbs;
	}
	
	public int[][][] getArgbs() {
		return this.argbs;
	}
	
	public void set4x4Means(int[][] meanArgbs) {
		this.meanArgbs = meanArgbs;
	}
	
	public void setArgbs(int[][][] argbs) {
		this.argbs = argbs;
	}
}
