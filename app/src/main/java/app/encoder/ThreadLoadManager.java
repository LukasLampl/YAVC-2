package app.encoder;

import java.util.ArrayList;

import app.interprediction.Vector;
import app.quadtree.QuadtreeEngine;
import app.utils.MacroBlock;

public class ThreadLoadManager<T> {
	private ArrayList<ArrayList<T>> list;
	private ArrayList<ArrayList<T>> evenlyDistributedObjects;
	
	private int totalPixels = 0;
	private int numberOfLoad = 0;
	private int numberOfChunks = 0;

	public ThreadLoadManager() {
		init();
	};
	
	public void clear() {
		init();
	}
	
	private void init() {
		this.list = new ArrayList<ArrayList<T>>();
		this.evenlyDistributedObjects = new ArrayList<ArrayList<T>>();
		
		for (int i = 0; i < QuadtreeEngine.NUMBER_OF_SIZES; i++) {
			this.list.add(new ArrayList<T>());
		}
	}

	public void update(ArrayList<ArrayList<T>> list, final int totalLoad) {
		this.list = list;
		this.totalPixels = totalLoad;
		compute();
	}
	
	public void setObj(T obj) {
		int estimatedIndex = 0;
		
		if (obj instanceof MacroBlock) {
			QuadtreeEngine.getIndexBySize(((MacroBlock)obj).getSize());
		}
		
		ArrayList<T> target = this.list.get(estimatedIndex);
		target.add(obj);
	}
	
	public void compute(int totalPixels) {
		this.totalPixels = totalPixels;
		compute();
	}
	
	private void compute() {
		this.numberOfChunks = Runtime.getRuntime().availableProcessors();
		int loadPerThread = this.totalPixels / this.numberOfChunks;
		int currentLoad = 0;
		int currentIndex = 0;
		
		for (int i = 0; i < this.numberOfChunks; i++) {
			this.evenlyDistributedObjects.add(new ArrayList<T>());
		}
		
		for (ArrayList<T> blockList : this.list) {
			for (T obj : blockList) {
				if (obj instanceof MacroBlock) {
					currentLoad += ((MacroBlock)obj).getSquaredSize();
				} else if (obj instanceof Vector) {
					currentLoad += ((Vector)obj).getSize() * ((Vector)obj).getSize();
				}
				
				this.evenlyDistributedObjects.get(currentIndex).add(obj);
				this.numberOfLoad++;
				
				if (currentLoad >= loadPerThread) {
					currentLoad = 0;
					currentIndex++;
				}
			}
		}
	}

	public ArrayList<T> getLoadOf(int index) {
		return this.evenlyDistributedObjects.get(index);
	}
	
	public int getLoadNumber() {
		return this.numberOfLoad;
	}
	
	public int getNumberOfChunks() {
		return this.numberOfChunks;
	}
}
