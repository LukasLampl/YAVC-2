package app.utils;

import java.util.ArrayList;

import app.encoder.ThreadLoadManager;
import app.interprediction.Vector;

public class QueueObject {
	private ArrayList<Vector> vectors = new ArrayList<Vector>();
	private ArrayList<MacroBlock> differences = new ArrayList<MacroBlock>();
	
	public QueueObject(ThreadLoadManager<Vector> vecManager, ThreadLoadManager<MacroBlock> diffManager) {
		for (int i = 0; i < vecManager.getNumberOfChunks(); i++) {
			this.vectors.addAll(vecManager.getLoadOf(i));
		}
		
		for (int i = 0; i < diffManager.getNumberOfChunks(); i++) {
			this.differences.addAll(diffManager.getLoadOf(i));
		}
	}
	
	public ArrayList<Vector> getVectors() {
		return this.vectors;
	}
	
	public ArrayList<MacroBlock> getDifferences() {
		return this.differences;
	}
}
