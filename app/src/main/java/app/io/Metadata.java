package app.io;

import java.awt.Dimension;

public class Metadata {
	private int frameNumber = 0;
	private Dimension dimensionOfFrames = new Dimension(1, 1);
	
	public Metadata(int frameNumber, Dimension dimensionOfFrames) {
		this.frameNumber = frameNumber;
		this.dimensionOfFrames = dimensionOfFrames;
	}
	
	public int getFrameNumber() {
		return this.frameNumber;
	}
	
	public Dimension getDimensionOfFrames() {
		return this.dimensionOfFrames;
	}
}
