package app.encoder;

import java.util.ArrayList;

import app.quadtree.QuadtreeEngine;
import app.utils.MacroBlock;

public class ThreadLoadManager {
	private ArrayList<ArrayList<MacroBlock>> list;
	private ArrayList<ArrayList<MacroBlock>> computedMacroBlockListList;
	
	private int totalPixels = 0;
	private int numberOfLoad = 0;
	private int numberOfChunks = 0;

	public ThreadLoadManager() {
		this.list = new ArrayList<ArrayList<MacroBlock>>();
		this.computedMacroBlockListList = new ArrayList<ArrayList<MacroBlock>>();
		
		for (int i = 0; i < QuadtreeEngine.NUMBER_OF_SIZES; i++) {
			this.list.add(new ArrayList<MacroBlock>());
		}
	};

	public void update(ArrayList<ArrayList<MacroBlock>> list, final int totalLoad) {
		this.list = list;
		this.totalPixels = totalLoad;
		compute();
	}
	
	public void setBlock(MacroBlock block) {
		int estimatedIndex = QuadtreeEngine.getIndexBySize(block.getSize());
		
		ArrayList<MacroBlock> target = this.list.get(estimatedIndex);
		target.add(block);
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
			this.computedMacroBlockListList.add(new ArrayList<MacroBlock>());
		}
		
		for (ArrayList<MacroBlock> blockList : this.list) {
			for (MacroBlock block : blockList) {
				currentLoad += block.getSquaredSize();
				this.computedMacroBlockListList.get(currentIndex).add(block);
				this.numberOfLoad++;
				
				if (currentLoad >= loadPerThread) {
					currentLoad = 0;
					currentIndex++;
				}
			}
		}
	}

	public ArrayList<MacroBlock> getLoadOf(int index) {
		return this.computedMacroBlockListList.get(index);
	}
	
	public int getLoadNumber() {
		return this.numberOfLoad;
	}
	
	public int getNumberOfChunks() {
		return this.numberOfChunks;
	}
}
