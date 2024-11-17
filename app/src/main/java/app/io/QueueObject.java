package app.io;

import java.util.ArrayList;

import app.interprediction.Vector;
import app.utils.Discardable;
import app.utils.LoadDistributor;
import app.utils.MacroBlock;

public class QueueObject implements Discardable {
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
	
	@Override
	public void discard() {
		this.vectors.clear();
		this.differences.clear();
	}
}
