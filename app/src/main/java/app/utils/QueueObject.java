package app.utils;

import java.util.ArrayList;

import app.encoder.ThreadLoadManager;
import app.interprediction.Vector;

public class QueueObject {
	private ArrayList<Vector> vectors = null;
	private ArrayList<MacroBlock> differences = new ArrayList<MacroBlock>();
	
	public QueueObject(ArrayList<Vector> vecs, ThreadLoadManager manager) {
		this.vectors = vecs;
		
		for (int i = 0; i < manager.getNumberOfChunks(); i++) {
			this.differences.addAll(manager.getLoadOf(i));
		}
	}
	
	public ArrayList<Vector> getVectors() {
		return this.vectors;
	}
	
	public ArrayList<MacroBlock> getDifferences() {
		return this.differences;
	}
}
