/////////////////////////////////////////////////////////////
///////////////////////    LICENSE    ///////////////////////
/////////////////////////////////////////////////////////////
/*
The YAVC video / frame compressor compresses frames.
Copyright (C) 2024  Lukas Nian En Lampl

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package app.encoder;

import java.util.ArrayList;
import java.util.List;

import app.interprediction.Vector;
import app.quadtree.QuadtreeEngine;
import app.utils.MacroBlock;

/**
 * The class {@code LoadDistributor<T>} provides basic functionalities for
 * splitting work into comparably even sub-loads, which can be then processed
 * further by multithreading.
 * 
 * @param <T>	The type of the main-work that should be splitted.
 * 
 * @version 1.1.0
 * @author Lukas Lampl
 * @author Hans Lampl
 * 
 * @since 1.1.0
 */
public class LoadDistributor<T> {
	/**
	 * <p>Holds the raw list of all undistributed items.</p>
	 */
	private List<ArrayList<T>> undistributedList;
	
	/**
	 * <p>Holds the evenly split work.</p>
	 */
	private List<ArrayList<T>> evenlyDistributedObjects;
	
	/**
	 * <p>Holds all inputed items.</p>
	 */
	private ArrayList<T> rawItems;
	
	/**
	 * <p>The total amount of data. (Even in the objects itself e.g. pixels).</p>
	 */
	private int numberOfData = 0;
	
	/**
	 * <p>The number of objects that are split.</p>
	 */
	private int numberOfObjects = 0;
	
	/**
	 * <p>Amount chunks to split the undistributed work to.</p>
	 */
	private int numberOfChunks = Runtime.getRuntime().availableProcessors();

	/**
	 * <p>Creates an empty {@code LoadDistributor} with nothing to distribute.</p>
	 */
	public LoadDistributor() {
		init();
	};
	
	/**
	 * <p>Initializes all holder arrays so they can be used by {@link #setObj(Object)}.</p>
	 */
	private void init() {
		this.undistributedList = new ArrayList<ArrayList<T>>();
		this.evenlyDistributedObjects = new ArrayList<ArrayList<T>>();
		this.rawItems = new ArrayList<T>();
		
		for (int i = 0; i < QuadtreeEngine.NUMBER_OF_SIZES; i++) {
			this.undistributedList.add(new ArrayList<T>());
		}
		
		for (int i = 0; i < this.numberOfChunks; i++) {
			this.evenlyDistributedObjects.add(new ArrayList<T>());
		}
	}
	
	/**
	 * <p>Sets one given object into the {@link #undistributedList} by it's size,
	 * originated from {@link app.quadtree.QuadtreeEngine#getIndexBySize(int)}.</p>
	 * 
	 * <p><b>Note:</b><br>
	 * An update using {@link #compute(int)} is absolutely necessary, else no objects
	 * will be split and there will be no work.</p>
	 * 
	 * @param obj	The object to add.
	 */
	public void setObj(T obj) {
		int estimatedIndex = 0;
		
		if (obj instanceof MacroBlock) {
			QuadtreeEngine.getIndexBySize(((MacroBlock)obj).getSize());
		}
		
		ArrayList<T> target = this.undistributedList.get(estimatedIndex);
		target.add(obj);
		this.rawItems.add(obj);
	}
	
	/**
	 * <p>Adds a whole list of items to the LoadDistributor.</p>
	 * 
	 * @param l		The List to add.
	 */
	public void setAll(List<T> l) {
		for (T obj : l) {
			setObj(obj);
		}
	}
	
	/**
	 * <p>Sets the data amount and finally calls {@link #compute()}.</p>
	 * 
	 * @param nunmberOfData		The total amount of data (e.g. Pixels).
	 */
	public void compute(int numberOfData) {
		this.numberOfData = numberOfData;
		compute();
	}
	
	/**
	 * <p>Estimates the work for one thread by first calculating the work for one sub-work
	 * and then filling it up until the threshold is met. After that the next sub-work is filled
	 * until no undistributed work is left.</p>
	 * 
	 * <p><b>Information:</b><br>
	 * The last sub-work will have to least amount of work, since it is nearly impossible
	 * to have a data-set that matches the estimated amount of work per sub-work.</p>
	 */
	private void compute() {
		int loadPerThread = this.numberOfData / this.numberOfChunks;
		int currentLoad = 0;
		int currentIndex = 0;
		
		for (ArrayList<T> blockList : this.undistributedList) {
			for (T obj : blockList) {
				if (obj instanceof MacroBlock) {
					currentLoad += ((MacroBlock)obj).getSquaredSize();
				} else if (obj instanceof Vector) {
					currentLoad += ((Vector)obj).getSize() * ((Vector)obj).getSize();
				}
				
				this.evenlyDistributedObjects.get(currentIndex).add(obj);
				this.numberOfObjects++;
				
				if (currentLoad >= loadPerThread) {
					currentLoad = 0;
					currentIndex++;
				}
			}
		}
	}

	/**
	 * <p>Gets the sub-work at the specified index.
	 * 
	 * @param index	Index from which to get the sub-work from.
	 * @return The sub-work at the specified index.
	 */
	public ArrayList<T> getLoadOf(int index) {
		return this.evenlyDistributedObjects.get(index);
	}
	
	/**
	 * <p>Returns the total amount of objects in the undistributed and distributed list.</p>
	 * @return The total amount of objects in the undistributed and distributed list.
	 */
	public int getNumberOfObjects() {
		return this.numberOfObjects;
	}
	
	/**
	 * <p>Return the number of sub-works (chunks).</p>
	 * @return The number of sub-works (chunks).
	 */
	public int getNumberOfChunks() {
		return this.numberOfChunks;
	}
	
	/**
	 * <p>Returns the iterable to the evenly distributed sub-works (chunks).</p>
	 * @return The iterable to the evenly distributed sub-works (chunks).
	 */
	public Iterable<ArrayList<T>> getIterable() {
		return this.evenlyDistributedObjects;
	}
	
	/**
	 * <p>Returns all invoked data in the LoadDistributor.</p>
	 * @return The invoked data in the LoadDistributor.
	 */
	public ArrayList<T> getRawData() {
		return this.rawItems;
	}
}
