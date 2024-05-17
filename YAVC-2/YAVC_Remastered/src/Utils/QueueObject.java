package Utils;

import java.util.ArrayList;

public class QueueObject {
	private ArrayList<Vector> Vectors = null;
	
	public QueueObject(ArrayList<Vector> vecs) {
		this.Vectors = vecs;
	}
	
	public ArrayList<Vector> getVectors() {
		return this.Vectors;
	}
}
