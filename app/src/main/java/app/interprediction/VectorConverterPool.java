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

package app.interprediction;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import app.utils.ListManager;

/**
 * The {@code VectorConverterPool} class is a parent class
 * for processing raw byte data into vectors.
 * 
 * @see app.interprediction.VectorConversionTask
 * @author Lukas Lampl
 * @since 1.2.5 [Optimized prototype]
 */
public class VectorConverterPool {
	/**
	 * The pool that will be used for processing.
	 */
	private ForkJoinPool pool = null;
	
	/**
	 * A collection of all indexes, where the vectors start.
	 */
	private List<Integer> indexes = null;
	
	/**
	 * The raw data containing the vectors.
	 */
	private byte[] data = null;
	
	/**
	 * A vector manager to which the results should be written to.
	 */
	private ListManager<Vector> vectorManager = null;
	
	/**
	 * Flag for whether the vector conversion should be single threaded or not.
	 */
	private boolean singleThreadedExcution = false;
	
	/**
	 * Initializes a {@code VectorConverterPool} with the given data.
	 * 
	 * @param indexes		The indexes of the vector starts.
	 * @param data			The raw data containing the vectors.
	 * @param vectorManager	A vector manager in which to write the results to.
	 */
	public VectorConverterPool(List<Integer> indexes, byte[] data, ListManager<Vector> vectorManager, boolean singleThreaded) {
		this.pool = ForkJoinPool.commonPool();
		this.indexes = indexes;
		this.data = data;
		this.vectorManager = vectorManager;
		this.singleThreadedExcution = singleThreaded;
	}
	
	/**
	 * Starts the conversion of the raw data to vectors.
	 * 
	 * @see app.interprediction.Vector
	 */
	public void run() {
		VectorConversionTask task = new VectorConversionTask(0, this.indexes.size(), this.indexes, this.data, this.vectorManager);
		
		if (this.singleThreadedExcution) {
			task.setSingleThreaded();
		}
		
		this.pool.invoke(task);
		this.pool.shutdown();
	}
}
