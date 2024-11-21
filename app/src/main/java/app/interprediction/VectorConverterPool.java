package app.interprediction;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import app.utils.ListManager;

public class VectorConverterPool {
	private ForkJoinPool pool = null;
	private List<Integer> indexes = null;
	private byte[] data = null;
	private ListManager<Vector> vectorManager = null;
	
	public VectorConverterPool(List<Integer> indexes, byte[] data, ListManager<Vector> vectorManager) {
		this.pool = new ForkJoinPool();
		this.indexes = indexes;
		this.data = data;
		this.vectorManager = vectorManager;
	}
	
	public void run() {
		this.pool.invoke(new VectorConversionTask(0, this.indexes.size(), this.indexes, this.data, this.vectorManager));
		this.pool.shutdown();
	}
}
