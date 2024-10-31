package app.utils;

import java.util.ArrayList;

import app.encoder.LoadDistributor;
import app.interprediction.Vector;

public class QueueObject {
	private ArrayList<Vector> vectors = new ArrayList<Vector>();
	private ArrayList<MacroBlock> differences = new ArrayList<MacroBlock>();
	
	public QueueObject(LoadDistributor<Vector> vecManager, LoadDistributor<MacroBlock> diffManager) {
		this.vectors.addAll(vecManager.getRawData());
		this.differences.addAll(diffManager.getRawData());
	}
	
	public ArrayList<Vector> getVectors() {
		return this.vectors;
	}
	
	public ArrayList<MacroBlock> getDifferences() {
		return this.differences;
	}
}
