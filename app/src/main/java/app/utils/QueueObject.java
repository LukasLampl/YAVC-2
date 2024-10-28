package app.utils;

import java.util.ArrayList;

import app.interprediction.Vector;

public class QueueObject {
	private ArrayList<Vector> Vectors = null;
	private ArrayList<MacroBlock> Differences = null;
	
	public QueueObject(ArrayList<Vector> vecs, ArrayList<MacroBlock> diffs) {
		this.Vectors = vecs;
		this.Differences = diffs;
	}
	
	public ArrayList<Vector> getVectors() {
		return this.Vectors;
	}
	
	public ArrayList<MacroBlock> getDifferences() {
		return this.Differences;
	}
}
