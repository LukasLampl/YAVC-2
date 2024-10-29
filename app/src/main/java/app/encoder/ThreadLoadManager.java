package app.encoder;

import java.util.ArrayList;

import app.utils.MacroBlock;

public class ThreadLoadManager {
	private final ArrayList<ArrayList<MacroBlock>> list;
	private ArrayList<ArrayList<MacroBlock>> load = null;
	
	private int totalLoad = 0;
	
	public ThreadLoadManager(ArrayList<ArrayList<MacroBlock>> list, final int totalLoad) {
		this.list = list;
		this.totalLoad = totalLoad;
		compute();
	}
	
	private void compute() {
		int loadPerThread = this.totalLoad / Runtime.getRuntime().availableProcessors();
		int currentLoad = 0;
		int currentIndex = 0;
		
		for (ArrayList<MacroBlock> blockList : this.list) {
			for (MacroBlock block : blockList) {
				currentLoad += block.getSquaredSize();
				this.load.get(currentIndex).add(block);
				
				if (currentLoad >= loadPerThread) {
					currentLoad = 0;
					currentIndex++;
				}
			}
		}
	}
	
	private int index = 0;
	
	public ArrayList<MacroBlock> getNextLoad() {
		return list.get(index++);
	}
}
